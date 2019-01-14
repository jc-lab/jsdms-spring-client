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

import com.jcraft.jsch.*;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SshKeySshSessionFactory extends SshSessionFactory {
    private final JSch sch;
    private HostKeyRepository hostKeyRepository = null;
    private Integer port = null;

    protected SshKeySshSessionFactory(JSch sch) {
        this.sch = sch;
    }

    public static SshKeySshSessionFactory createByPrivateKey(String privateKey, String passphrase) throws JSchException {
        JSch sch = new JSch();
        Identity identity = new SshKeyIdentity(sch, "key", KeyPair.load(sch, privateKey.getBytes(), null));
        if(passphrase == null)
            sch.addIdentity(identity, null);
        else
            sch.addIdentity(identity, passphrase.getBytes());
        return new SshKeySshSessionFactory(sch);
    }

    public static SshKeySshSessionFactory createByPrivateKey(String privateKey) throws JSchException {
        return createByPrivateKey(privateKey, null);
    }

    public static SshKeySshSessionFactory createByPrivateFile(File privateFile, String passphrase) throws JSchException, IOException {
        JSch sch = new JSch();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(privateFile));
        byte[] buffer = new byte[bis.available()];
        bis.read(buffer);
        if(passphrase == null)
            sch.addIdentity(new String(buffer));
        else
            sch.addIdentity(new String(buffer), passphrase);
        return new SshKeySshSessionFactory(sch);
    }

    public static SshKeySshSessionFactory createByPrivateFile(File privateFile) throws JSchException, IOException {
        return createByPrivateFile(privateFile, null);
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setHostKeyRepository(HostKeyRepository hostKeyRepository) {
        this.hostKeyRepository = hostKeyRepository;
    }

    @Override
    public RemoteSession getSession(URIish uri, CredentialsProvider credentialsProvider, FS fs, int tms) throws TransportException {
        try {
            Session session = this.sch.getSession(uri.getUser(), uri.getHost());
            if(this.hostKeyRepository != null)
                session.setHostKeyRepository(this.hostKeyRepository);
            if(this.port != null)
                session.setPort(this.port);
            else
                session.setPort(22);
            if(tms <= 0)
                session.connect();
            else
                session.connect(tms);
            return new JschSession(session, uri);
        } catch (JSchException e) {
            e.printStackTrace();
        }
        return null;
    }
}
