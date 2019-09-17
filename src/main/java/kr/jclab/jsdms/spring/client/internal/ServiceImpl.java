/*
 * Copyright 2018 JC-Lab. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.jclab.jsdms.spring.client.internal;

import kr.jclab.jsdms.spring.client.JsDMSSpringClientProperties;
import kr.jclab.jsdms.spring.client.event.RepositoryChangeHandler;
import kr.jclab.jsdms.spring.client.service.JsDMSSpringClientService;
import com.jcraft.jsch.JSchException;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;

import java.io.File;
import java.io.IOException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceImpl implements JsDMSSpringClientService {
    private JsDMSSpringClientProperties properties;

    private ExecutorService executorService;
    private OrderingExecutor executor;

    private ZkClient zkClient = null;

    private Map<String, TargetMonitor> targetMonitors = new HashMap();


    private Set<RepositoryChangeHandler> repositoryChangeHandlers = new HashSet<RepositoryChangeHandler>();


    public ServiceImpl(JsDMSSpringClientProperties properties) {
        this.properties = properties;
        executorService = Executors.newFixedThreadPool(2);
        executor = new OrderingExecutor(executorService);

        for(JsDMSSpringClientProperties.TargetProperties targetProperties : this.properties.getTargets()) {
            String gitUri = targetProperties.getGitUri();
            int fpos = gitUri.lastIndexOf("/");
            int dpot = gitUri.lastIndexOf(".");
            if(fpos < 0) {
                throw new IllegalArgumentException("Wrong git uri: " + gitUri);
            }
            String gitRepoName = gitUri.substring(fpos + 1, (dpot < 0) ? gitUri.length() : dpot);
            ZookeeperTargetMonitor targetMonitor = null;
            try {
                targetMonitor = new ZookeeperTargetMonitor(this, targetProperties, gitRepoName);
                targetMonitors.put(targetProperties.getName(), targetMonitor);
            } catch (JSchException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if("zookeeper".compareToIgnoreCase(properties.getSource()) == 0) {
            this.zkClient = new ZkClient(properties.getZookeeper().getConnectString(), 60000, 5000, new ZkSerializer() {

                @Override
                public byte[] serialize(Object data) throws ZkMarshallingError {
                    if(data instanceof byte[]) {
                        return (byte[])data;
                    }
                    throw new ZkMarshallingError("Not support instance");
                }

                @Override
                public Object deserialize(byte[] bytes) throws ZkMarshallingError {
                    return bytes;
                }
            });
            onZookeeperConnected();
        }
    }

    public ZkClient getZkClient() {
        return this.zkClient;
    }

    private void onZookeeperConnected() {
        for(JsDMSSpringClientProperties.TargetProperties targetProperties : this.properties.getTargets()) {
            String gitUri = targetProperties.getGitUri();
            int fpos = gitUri.lastIndexOf("/");
            int dpot = gitUri.lastIndexOf(".");
            if (fpos < 0) {
                throw new IllegalArgumentException("Wrong git uri: " + gitUri);
            }
            String gitRepoName = gitUri.substring(fpos + 1, (dpot < 0) ? gitUri.length() : dpot);
            String zpath = "/dms/git-repo-status/" + gitRepoName;
            Object object = targetMonitors.get(targetProperties.getName());
            if (object instanceof ZookeeperTargetMonitor) {
                ZookeeperTargetMonitor targetMonitor = (ZookeeperTargetMonitor) object;
                targetMonitor.start(zpath);
            }
        }
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }

    public void asyncDownloadMasterBranch(TargetMonitor targetMonitor) {
        executor.execute(new DownloadMasterBranchTask(targetMonitor), targetMonitor);
    }

    public void asyncPullBranch(TargetMonitor targetMonitor, String branchName) {
        executor.execute(new PullBranchTask(targetMonitor, branchName), targetMonitor);
    }

    private <T extends TransportCommand> T configTransportCommand(TargetMonitor targetMonitor, T transportCommand) {
        TransportConfigCallback transportConfigCallback = targetMonitor.getSshTransportConfigCallback();
        CredentialsProvider credentialsProvider = targetMonitor.getCredentialsProvider();
        if (transportConfigCallback != null)
            transportCommand.setTransportConfigCallback(transportConfigCallback);
        if (credentialsProvider != null)
            transportCommand.setCredentialsProvider(credentialsProvider);
        return transportCommand;
    }

    private class DownloadMasterBranchTask implements Runnable {
        TargetMonitor targetMonitor;

        public DownloadMasterBranchTask(TargetMonitor targetMonitor) {
            this.targetMonitor = targetMonitor;
        }

        @Override
        public void run() {
            Git git = null;
            TransportConfigCallback transportConfigCallback = targetMonitor.getSshTransportConfigCallback();
            CredentialsProvider credentialsProvider = targetMonitor.getCredentialsProvider();
            try {
                File resourceDir = targetMonitor.getResourceMasterDir();
                try {
                    git = Git.open(resourceDir);
                } catch (IOException e) {
                    System.err.println(resourceDir.getAbsolutePath() + ": start download master branch because of " + e.getMessage());
                }
                if(git != null) {
                    System.out.println(resourceDir.getAbsolutePath() + ": master branch already downloaded");

                    FetchCommand fetchCommand = configTransportCommand(targetMonitor, git.fetch());
                    FetchResult fetchResult = fetchCommand.call();
                    System.out.println(resourceDir.getAbsolutePath() + ": Fetch complete");

                    PullCommand pullCommand = configTransportCommand(targetMonitor, git.pull());
                    PullResult pullResult = pullCommand.call();
                    System.out.println(resourceDir.getAbsolutePath() + ": Pull complete");

                }else{
                    CloneCommand cloneCommand = configTransportCommand(targetMonitor, Git.cloneRepository());
                    cloneCommand.setURI(targetMonitor.getProperties().getGitUri());
                    cloneCommand.setDirectory(targetMonitor.getResourceMasterDir());
                    cloneCommand.setBranch("master");
                    git = cloneCommand.call();
                    System.out.println(resourceDir.getAbsolutePath() + ": download complete");
                }

                List<Ref> branchList = git.branchList()
                        .setListMode(ListBranchCommand.ListMode.REMOTE)
                        .call();
                System.out.println(resourceDir.getAbsolutePath() + ": Found branches: " + branchList);
                for(Ref ref : branchList) {
                    String name = ref.getName();
                    int bpos = name.lastIndexOf("/");
                    if(bpos >= 0) {
                        String branchName = name.substring(bpos + 1);
                        if (branchName.startsWith("deploy-")) {
                            asyncPullBranch(targetMonitor, branchName);
                        }
                    }
                }
            } catch (InvalidRemoteException e) {
                e.printStackTrace();
            } catch (TransportException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            } finally {
                if(git != null) {
                    git.close();
                }
            }
        }
    }

    private class PullBranchTask implements Runnable {
        TargetMonitor targetMonitor;
        String branchName;

        public PullBranchTask(TargetMonitor targetMonitor, String branchName) {
            this.targetMonitor = targetMonitor;
            this.branchName = branchName;
        }

        @Override
        public void run() {
            File masterDir = targetMonitor.getResourceMasterDir();
            File branchDir = targetMonitor.getResourceBranchName(branchName);
            Git git = null;
            System.out.println(branchDir.getAbsolutePath() + ": start branch(" + branchName + ") pull");
            try {
                int retry;
                if(!branchDir.exists()) {
                    System.out.println(branchDir.getAbsolutePath() + ": copy from master");
                    FileUtils.copyDirectory(masterDir, branchDir);
                }
                git = Git.open(branchDir);
                List<Ref> branchies = git.branchList().call();
                boolean hasBranch = false;
                for(Ref ref : branchies) {
                    String[] temp = ref.getName().split("/");
                    if(branchName.equalsIgnoreCase(temp[temp.length - 1])) {
                        hasBranch = true;
                        break;
                    }
                }
                if(!git.getRepository().getBranch().equalsIgnoreCase(branchName)) {
                    git.checkout()
                            .setForceRefUpdate(true)
                            .setCreateBranch(!hasBranch)
                            .setName(branchName)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                            .setStartPoint("origin/" + branchName)
                            .call();
                }
                for(retry = 0; retry < 2; retry++) {
                    try {
                        configTransportCommand(targetMonitor, git.pull())
                                .setRemoteBranchName(branchName)
                                .call();
                        break;
                    } catch (CheckoutConflictException conflictEx) {
                        System.err.println(branchDir.getAbsolutePath() + ": CheckoutConflictException!");
                        git.reset()
                                .setMode(ResetCommand.ResetType.HARD)
                                .setRef("origin/" + branchName)
                                .call();
                    }
                }
                System.out.println(branchDir.getAbsolutePath() + ": pull completed");

                JsDMSSpringClientProperties.TargetProperties targetProperties = targetMonitor.getProperties();
                synchronized (repositoryChangeHandlers) {
                    for(RepositoryChangeHandler handler : repositoryChangeHandlers) {
                        handler.onRepositoryChanged(targetProperties.getName(), branchName, branchDir);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RefAlreadyExistsException e) {
                e.printStackTrace();
            } catch (InvalidRefNameException e) {
                e.printStackTrace();
            } catch (RefNotFoundException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            } finally {
                if(git != null) {
                    git.close();
                }
            }
        }
    }

    @Override
    public File findRepoDirByName(String name) {
        TargetMonitor targetMonitor = targetMonitors.get(name);
        if(targetMonitor == null)
            return null;
        return targetMonitor.getRepoDir();
    }

    @Override
    public void addRepositoryChangeHandler(RepositoryChangeHandler handler) {
        synchronized (this.repositoryChangeHandlers) {
            this.repositoryChangeHandlers.add(handler);
        }
    }

    @Override
    public void removeRepositoryChangeHandler(RepositoryChangeHandler handler) {
        synchronized (this.repositoryChangeHandlers) {
            this.repositoryChangeHandlers.remove(handler);
        }
    }

    @Override
    public void forceTrigger(String name, String branchName) {
        TargetMonitor targetMonitor = targetMonitors.get(name);
        if(targetMonitor != null) {
            targetMonitor.forceTrigger(branchName);
        }
    }

    public void stop() {
        executorService.shutdown();
    }
}
