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

/**
 * Identity for KeyPair
 * reference: com.jcraft.jsch.IdentityFile
 */
class SshKeyIdentity implements Identity {
    private JSch jsch;
    private KeyPair kpair;
    private String identity;

    public SshKeyIdentity(JSch jsch, String name, KeyPair kpair) throws JSchException{
        this.jsch = jsch;
        this.identity = name;
        this.kpair = kpair;
    }

    /**
     * Decrypts this identity with the specified pass-phrase.
     * @param passphrase the pass-phrase for this identity.
     * @return <tt>true</tt> if the decryption is succeeded
     * or this identity is not cyphered.
     */
    public boolean setPassphrase(byte[] passphrase) throws JSchException{
        return kpair.decrypt(passphrase);
    }

    /**
     * Returns the public-key blob.
     * @return the public-key blob
     */
    public byte[] getPublicKeyBlob(){
        return kpair.getPublicKeyBlob();
    }

    /**
     * Signs on data with this identity, and returns the result.
     * @param data data to be signed
     * @return the signature
     */
    public byte[] getSignature(byte[] data){
        return kpair.getSignature(data);
    }

    /**
     * @deprecated This method should not be invoked.
     * @see #setPassphrase(byte[] passphrase)
     */
    public boolean decrypt(){
        throw new RuntimeException("not implemented");
    }

    /**
     * Returns the name of the key algorithm.
     * @return "ssh-rsa" or "ssh-dss"
     */
    public String getAlgName(){
        return "ssh-rsa";
    }

    /**
     * Returns the name of this identity.
     * It will be useful to identify this object in the {@link IdentityRepository}.
     */
    public String getName(){
        return identity;
    }

    /**
     * Returns <tt>true</tt> if this identity is cyphered.
     * @return <tt>true</tt> if this identity is cyphered.
     */
    public boolean isEncrypted(){
        return kpair.isEncrypted();
    }

    /**
     * Disposes internally allocated data, like byte array for the private key.
     */
    public void clear(){
        kpair.dispose();
        kpair = null;
    }

    /**
     * Returns an instance of {@link KeyPair} used in this {@link Identity}.
     * @return an instance of {@link KeyPair} used in this {@link Identity}.
     */
    public KeyPair getKeyPair(){
        return kpair;
    }
}
