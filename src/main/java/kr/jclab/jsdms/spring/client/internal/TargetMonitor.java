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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import kr.jclab.jsdms.spring.client.JsDMSSpringClientProperties;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.zookeeper.ZooDefs.Ids.ANYONE_ID_UNSAFE;

public class TargetMonitor implements IZkChildListener, IZkDataListener {
    /**
     * This is a completely open ACL .
     */
    public final ArrayList<ACL> ZOOKEEPER_OPEN_ACL_UNSAFE = new ArrayList<ACL>(
            Collections.singletonList(new ACL(ZooDefs.Perms.ALL, ANYONE_ID_UNSAFE))
    );

    private final ServiceImpl service;

    private final JsDMSSpringClientProperties.TargetProperties properties;
    private final String gitRepoName;
    private String zpathRoot;

    private final SshSessionFactory sshSessionFactory;
    private final CredentialsProvider credentialsProvider;

    private final File repoDir;
    private final File resourceMasterDir;


    public TargetMonitor(ServiceImpl service, JsDMSSpringClientProperties.TargetProperties properties, String gitRepoName) throws JSchException, IOException {
        this.service = service;
        this.properties = properties;
        this.gitRepoName = gitRepoName;
        this.repoDir = new File(properties.getResourceDirectory(), gitRepoName);
        this.resourceMasterDir = new File(this.repoDir, "master");

        if(properties.getSshKeyPlain() != null) {
            this.sshSessionFactory = SshKeySshSessionFactory.createByPrivateKey(properties.getSshKeyPlain(), properties.getSshKeyPassphrase());
        }else if(properties.getSshKeyFile() != null) {
            this.sshSessionFactory = SshKeySshSessionFactory.createByPrivateFile(new File(properties.getSshKeyFile()), properties.getSshKeyPassphrase());
        }else{
            this.sshSessionFactory = null;
        }

        if(this.sshSessionFactory != null) {
            ((SshKeySshSessionFactory) this.sshSessionFactory).setPort(properties.getGitPort());
            ((SshKeySshSessionFactory) this.sshSessionFactory).setHostKeyRepository(new MyHostKeyRepository());
        }

        this.credentialsProvider = null;
    }

    public void start(String zpath) {
        final ZkClient zkClient = service.getZkClient();

        this.zpathRoot = zpath;

        if(!this.repoDir.exists()) {
            this.repoDir.mkdirs();
        }

        service.asyncDownloadMasterBranch(this);

        service.execute(() ->{
            zkClient.createPersistent(zpath, true);
            registerWatcherToChilds(this.zpathRoot, zkClient.getChildren(this.zpathRoot));
            zkClient.subscribeChildChanges(this.zpathRoot, this);
        });
    }

    public final File getRepoDir() {
        return repoDir;
    }

    public final File getResourceMasterDir() {
        return resourceMasterDir;
    }

    public final File getResourceBranchName(String branchName) throws IllegalArgumentException {
        if(branchName.startsWith("deploy-")) {
            return new File(this.repoDir, branchName.substring(7));
        }
        throw new IllegalArgumentException("branchName is not starts with 'deploy-': " + branchName);
    }

    public final JsDMSSpringClientProperties.TargetProperties getProperties() {
        return this.properties;
    }

    public CredentialsProvider getCredentialsProvider() {
        return this.credentialsProvider;
    }

    public TransportConfigCallback getSshTransportConfigCallback() {
        if(this.sshSessionFactory == null)
            return null;
        return (Transport transport) -> {
            SshTransport sshTransport = (SshTransport)transport;
            sshTransport.setSshSessionFactory(this.sshSessionFactory);
        };
    }

    private void onNodeDataChanged(String path, Object data) {
        if(!(data instanceof byte[]))
            return ;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> jsonDoc = objectMapper.readValue((byte[])data, Map.class);
            String before = (String)jsonDoc.get("before");
            String after = (String)jsonDoc.get("after");

            if(before != null && (after == null || !before.equalsIgnoreCase(after))) {
                File fpath = new File(path);
                String branchName = fpath.getName();
                service.asyncPullBranch(TargetMonitor.this, branchName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerWatcherToChilds(String parentPath, List<String> childs) {
        ZkClient zkClient = service.getZkClient();
        for(String child : childs) {
            String zpath = parentPath + "/" + child;
            zkClient.subscribeDataChanges(zpath, this);
        }
    }

    @Override
    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
        ServiceImpl service = this.service;
        registerWatcherToChilds(parentPath, currentChilds);

        if(parentPath.equalsIgnoreCase(this.zpathRoot)) {
            service.execute(() -> {
                for (String child : currentChilds) {
                    String cpath = parentPath + "/" + child;
                    Object cdata = this.service.getZkClient().readData(cpath);
                    onNodeDataChanged(cpath, cdata);
                }
            });
        }
    }

    @Override
    public void handleDataChange(String dataPath, Object data) throws Exception {
        onNodeDataChanged(dataPath, data);
    }

    @Override
    public void handleDataDeleted(String dataPath) throws Exception {
    }
}