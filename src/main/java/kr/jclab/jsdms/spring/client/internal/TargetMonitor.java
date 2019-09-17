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
import com.jcraft.jsch.JSchException;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;

import java.io.File;
import java.io.IOException;

public class TargetMonitor {
    protected final ServiceImpl service;

    protected final JsDMSSpringClientProperties.TargetProperties properties;
    protected final String gitRepoName;

    protected final SshSessionFactory sshSessionFactory;
    protected final CredentialsProvider credentialsProvider;

    protected final File repoDir;
    protected final File resourceMasterDir;

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

    void init() {
        if(!this.repoDir.exists()) {
            this.repoDir.mkdirs();
        }

        service.asyncDownloadMasterBranch(this);
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

    public void forceTrigger(String branchName) {
        service.asyncPullBranch(TargetMonitor.this, branchName);
    }
}