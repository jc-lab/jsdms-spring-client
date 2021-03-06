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

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.UserInfo;

public class MyHostKeyRepository implements HostKeyRepository {
    @Override
    public int check(String host, byte[] key) {
        return HostKeyRepository.OK;
    }

    @Override
    public void add(HostKey hostkey, UserInfo ui) {

    }

    @Override
    public void remove(String host, String type) {

    }

    @Override
    public void remove(String host, String type, byte[] key) {

    }

    @Override
    public String getKnownHostsRepositoryID() {
        return null;
    }

    @Override
    public HostKey[] getHostKey() {
        return new HostKey[0];
    }

    @Override
    public HostKey[] getHostKey(String host, String type) {
        return new HostKey[0];
    }
}
