package org.netbeans.lib.cvsclient.connection;

import java.io.Serializable;

public class ConnectionIdentity implements Serializable {
    
    private static final long serialVersionUID = 4145082117499748924L;

    private String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa";
    private String privateKeyPassword = null;
    private String knownHostsFile = System.getProperty("user.home") + "/.ssh/known_hosts";
    
    public String getPrivateKeyPath() {
        return privateKeyPath;
    }
    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }
    public String getPrivateKeyPassword() {
        return privateKeyPassword;
    }
    public void setPrivateKeyPassword(String privateKeyPassword) {
        this.privateKeyPassword = privateKeyPassword;
    }
    public String getKnownHostsFile() {
        return knownHostsFile;
    }
    public void setKnownHostsFile(String knownHostsFile) {
        this.knownHostsFile = knownHostsFile;
    }

}
