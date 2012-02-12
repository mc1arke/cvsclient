/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.cvsclient.connection;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.net.SocketFactory;

import org.netbeans.lib.cvsclient.CVSRoot;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.util.LoggedDataInputStream;
import org.netbeans.lib.cvsclient.util.LoggedDataOutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * Provides SSH tunnel for :ext: connection method. 
 * 
 * @author Maros Sandor
 */
public class SSHConnection extends AbstractConnection {

    private static final long serialVersionUID = 8088416203833050235L;

    private static final String CVS_SERVER_COMMAND = System.getenv("CVS_SERVER") != null?
        System.getenv("CVS_SERVER") + " server": "cvs server";  // NOI18N

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final ConnectionIdentity connectionIdentity;
    
    /* 
     * these values aren't serializable so we have to mark them as transient. The session
     * and channel aren't created until {@link #open} has been called so, providing we don't
     * serialize after the connection has been opened, we should be ok.
     */
    private transient Session session;
    private transient ChannelExec channel;

    /**
     * Creates new SSH connection object.
     * 
     * @param socketFactory socket factory to use when connecting to SSH server
     * @param host host names of the SSH server
     * @param port port number of SSH server
     * @param username SSH username
     * @param password SSH password
     */ 
    public SSHConnection(CVSRoot root, ConnectionIdentity connectionIdentity) {
        this.host = root.getHostName();
        this.port = root.getPort() == 0 ? 22 : root.getPort();
        this.username = root.getUserName() != null ? root.getUserName() : System.getProperty("user.name"); // NOI18N
        this.password = root.getPassword();
        setRepository(root.getRepository());
        this.connectionIdentity = connectionIdentity;
    }

    public void open() throws AuthenticationException, CommandAbortedException {

        Properties props = new Properties();
        props.put("StrictHostKeyChecking", "no"); // NOI18N
        props.put("PreferredAuthentications", "publickey,password");
        
        
        JSch jsch = new JSch();
        try {
            session = jsch.getSession(username, host, port);
            session.setUserInfo(new SSHUserInfo());
            
            jsch.setKnownHosts(connectionIdentity.getKnownHostsFile());
            jsch.addIdentity(connectionIdentity.getPrivateKeyPath(), connectionIdentity.getPrivateKeyPassword());

            session.setSocketFactory(new SocketFactoryBridge(SocketFactory.getDefault()));
            session.setConfig(props);
            session.connect();
        } catch (JSchException e) {
            throw new AuthenticationException(e, "SSH connection failed.");
        }
        
        try {
            channel = (ChannelExec) session.openChannel("exec"); // NOI18N
            channel.setCommand(CVS_SERVER_COMMAND);
            setInputStream(new LoggedDataInputStream(new SshChannelInputStream(channel)));
            setOutputStream(new LoggedDataOutputStream(channel.getOutputStream()));
            channel.connect();
        } catch (JSchException e) {
            IOException ioe = new IOException("SSH connection failed.");
            ioe.initCause(e);
            throw new AuthenticationException(ioe, "Opening SSH channel failed.");
        } catch (IOException e) {
            throw new AuthenticationException(e, "Opening SSH channel failed.");
        }
    }

    /**
     * Verifies that we can successfuly connect to the SSH server and run 'cvs server' command on it.
     * 
     * @throws AuthenticationException if connection to the SSH server cannot be established (network problem)
     */ 
    public void verify() throws AuthenticationException {
        try {
            open();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }
            if (channel.getExitStatus() != -1) {
                throw new AuthenticationException(CVS_SERVER_COMMAND, "Error executing "+ CVS_SERVER_COMMAND +" on server. Set CVS_SERVER environment variable properly.");
            }
            close();
        } catch (CommandAbortedException e) {
            throw new AuthenticationException(e, "Opening SSH connection failed.");
        } catch (IOException e) {
            throw new AuthenticationException(e,"SSH: close connection failed.");
        } finally {
            reset();
        }
    }

    private void reset() {
        session = null;
        channel = null;
        setInputStream(null);
        setOutputStream(null);
    }
    
    public void close() throws IOException {
        if (session != null) session.disconnect();
        if (channel != null) channel.disconnect();
        reset();
    }

    public boolean isOpen() {
        return channel != null && channel.isConnected();
    }

    public int getPort() {
        return port;
    }

    public void modifyInputStream(ConnectionModifier modifier) throws IOException {
        modifier.modifyInputStream(getInputStream());
    }

    public void modifyOutputStream(ConnectionModifier modifier) throws IOException {
        modifier.modifyOutputStream(getOutputStream());
    }

    /**
     * Provides JSch with SSH password.
     */ 
    private class SSHUserInfo implements UserInfo, UIKeyboardInteractive {
        public String getPassphrase() {
            return null;
        }

        public String getPassword() {
            return password;
        }

        public boolean promptPassword(String message) {
            return true;
        }

        public boolean promptPassphrase(String message) {
            return true;
        }

        public boolean promptYesNo(String message) {
            return false;
        }

        public void showMessage(String message) {
        }

        public String[] promptKeyboardInteractive(String destination,
                                                  String name,
                                                  String instruction,
                                                  String[] prompt,
                                                  boolean[] echo){
          String[] response=new String[prompt.length];
          if(prompt.length==1){
            response[0]=password;
          }
          return response;                                                
        }
    }
    
    /**
     * Bridges com.jcraft.jsch.SocketFactory and javax.net.SocketFactory. 
     */ 
    private static class SocketFactoryBridge implements com.jcraft.jsch.SocketFactory {
        
        private SocketFactory socketFactory;
        
        public SocketFactoryBridge(SocketFactory socketFactory) {
            this.socketFactory = socketFactory;
        }

        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return socketFactory.createSocket(host, port);
        }

        public InputStream getInputStream(Socket socket) throws IOException {
            return socket.getInputStream();
        }

        public OutputStream getOutputStream(Socket socket) throws IOException {
            return socket.getOutputStream();
        }
    }
    
    private static class SshChannelInputStream extends FilterInputStream {
        
        private final Channel channel;

        public SshChannelInputStream(Channel channel) throws IOException {
            super(channel.getInputStream());
            this.channel = channel;
        }

        public int available() throws IOException {
            checkChannelState();
            return super.available();
        }

        private void checkChannelState() throws IOException {
            int exitStatus = channel.getExitStatus();
            if (exitStatus > 0 || exitStatus < -1) throw new IOException("Error executing " + CVS_SERVER_COMMAND + " on server.\\\\nSet CVS_SERVER environment variable properly.");
            if (exitStatus == 0 || channel.isEOF()) throw new EOFException("EOF: SSH tunnel closed.");
        }
    }
}
