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
package kr.jclab.jsdms.spring.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * External configuration properties for a JsDMS Spring Client created by Spring.
 *
 * @author jichan (development@jc-lab.net)
 */
@ConfigurationProperties(prefix = "spring.jsdms.client")
public class JsDMSSpringClientProperties {
    public static class ZookeeperProperties {
        private String connectString;

        public String getConnectString() {
            return connectString;
        }

        public void setConnectString(String connectString) {
            this.connectString = connectString;
        }
    }

    public static class TargetProperties {
        private String name;
        private String type;
        private String resourceDirectory;

        private String gitUri;
        private Integer gitPort;
        private String sshKeyPlain;
        private String sshKeyFile;
        private String sshKeyPassphrase;

        private List<String> sshHostKeys;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getResourceDirectory() {
            return resourceDirectory;
        }

        public void setResourceDirectory(String resourceDirectory) {
            this.resourceDirectory = resourceDirectory;
        }

        public String getGitUri() {
            return gitUri;
        }

        public void setGitUri(String gitUri) {
            this.gitUri = gitUri;
        }

        public Integer getGitPort() {
            return gitPort;
        }

        public void setGitPort(Integer gitPort) {
            this.gitPort = gitPort;
        }

        public String getSshKeyPlain() {
            return sshKeyPlain;
        }

        public void setSshKeyPlain(String sshKeyPlain) {
            this.sshKeyPlain = sshKeyPlain;
        }

        public String getSshKeyFile() {
            return sshKeyFile;
        }

        public void setSshKeyFile(String sshKeyFile) {
            this.sshKeyFile = sshKeyFile;
        }

        public String getSshKeyPassphrase() {
            return sshKeyPassphrase;
        }

        public void setSshKeyPassphrase(String sshKeyPassphrase) {
            this.sshKeyPassphrase = sshKeyPassphrase;
        }

        public List<String> getSshHostKeys() {
            return sshHostKeys;
        }

        public void setSshHostKeys(List<String> sshHostKeys) {
            this.sshHostKeys = sshHostKeys;
        }
    }

    private String source;
    private ZookeeperProperties zookeeper;
    private List<TargetProperties> targets;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<TargetProperties> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetProperties> targets) {
        this.targets = targets;
    }

    public ZookeeperProperties getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(ZookeeperProperties zookeeper) {
        this.zookeeper = zookeeper;
    }
}
