/*****************************************************************************
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
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is the CVS Client Library.
 * The Initial Developer of the Original Software is Robert Greig.
 * Portions created by Robert Greig are Copyright (C) 2000.
 * All Rights Reserved.
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
 *
 * Contributor(s): Robert Greig.
 *****************************************************************************/
package org.netbeans.lib.cvsclient;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.netbeans.lib.cvsclient.admin.AdminHandler;
import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.command.Command;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.GlobalOptions;
import org.netbeans.lib.cvsclient.command.KeywordSubstitutionOptions;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.Connection;
import org.netbeans.lib.cvsclient.event.CVSEvent;
import org.netbeans.lib.cvsclient.event.EnhancedMessageEvent;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.file.DefaultFileHandler;
import org.netbeans.lib.cvsclient.file.FileDetails;
import org.netbeans.lib.cvsclient.file.FileHandler;
import org.netbeans.lib.cvsclient.file.GzippedFileHandler;
import org.netbeans.lib.cvsclient.request.ExpandModulesRequest;
import org.netbeans.lib.cvsclient.request.GzipFileContentsRequest;
import org.netbeans.lib.cvsclient.request.Request;
import org.netbeans.lib.cvsclient.request.RootRequest;
import org.netbeans.lib.cvsclient.request.UnconfiguredRequestException;
import org.netbeans.lib.cvsclient.request.UseUnchangedRequest;
import org.netbeans.lib.cvsclient.request.ValidRequestsRequest;
import org.netbeans.lib.cvsclient.request.ValidResponsesRequest;
import org.netbeans.lib.cvsclient.request.WrapperSendRequest;
import org.netbeans.lib.cvsclient.response.ErrorMessageResponse;
import org.netbeans.lib.cvsclient.response.Response;
import org.netbeans.lib.cvsclient.response.ResponseException;
import org.netbeans.lib.cvsclient.response.ResponseFactory;
import org.netbeans.lib.cvsclient.response.ResponseServices;
import org.netbeans.lib.cvsclient.util.BugLog;
import org.netbeans.lib.cvsclient.util.DefaultIgnoreFileFilter;
import org.netbeans.lib.cvsclient.util.IgnoreFileFilter;
import org.netbeans.lib.cvsclient.util.LoggedDataInputStream;
import org.netbeans.lib.cvsclient.util.LoggedDataOutputStream;
import org.netbeans.lib.cvsclient.util.Logger;
import org.netbeans.lib.cvsclient.util.StringPattern;

/**
 * The main way of communication with a server using the CVS Protocol. The
 * client is not responsible for setting up the connection with the server, only
 * interacting with it.
 * 
 * @see org.netbeans.lib.cvsclient.connection.Connection
 * @author Robert Greig
 */
public class Client implements ClientServices, ResponseServices {
    /**
     * 
     */
    private static final long serialVersionUID = 3100889723982260702L;

    /**
     * The connection used to interact with the server.
     */
    private Connection connection;

    /**
     * The file handler to use.
     */
    private FileHandler transmitFileHandler;

    private FileHandler gzipFileHandler = new GzippedFileHandler();
    private FileHandler uncompFileHandler = new DefaultFileHandler();

    private boolean dontUseGzipFileHandler;

    /**
     * The modified date.
     */
    private Date modifiedDate;

    /**
     * The admin handler to use.
     */
    private AdminHandler adminHandler;

    /**
     * The local path, ie the path to the directory in which this command was
     * executed.
     */
    private String localPath;

    /**
     * Whether any commands have been executed so far. This allows us to send
     * some initialisation requests before the first command.
     */
    private boolean isFirstCommand = true;

    /**
     * The event manager.
     */
    private final EventManager eventManager = new EventManager(this);

    /**
     * The global options for the executing command.
     */
    private GlobalOptions globalOptions;

    /**
     * This is set to true if we should abort the current command.
     */
    private boolean abort;

    private ResponseFactory responseFactory;

    private IgnoreFileFilter ignoreFileFilter;

    /*
     * The valid list of requests that is valid for the CVS server corresponding
     * to this client
     */
    private final Map<String, ClientServices> validRequests = new HashMap<String, ClientServices>();

    /**
     * A map of file patterns and keyword substitution options
     */
    private Map<StringPattern, KeywordSubstitutionOptions> wrappersMap = null;

    /**
     * This will be set to true after initialization requests are sent to the
     * server. The initialization requests setup valid requests and send
     * RootRequest to the server.
     */
    private boolean initialRequestsSent = false;

    private boolean printConnectionReuseWarning = false;

    private static final Set<Class<?>> ALLOWED_CONNECTION_REUSE_REQUESTS = new HashSet<Class<?>>(
                    Arrays.asList(new Class<?>[] { ExpandModulesRequest.class, WrapperSendRequest.class }));

    // processRequests & getCounter
    private LoggedDataInputStream loggedDataInputStream;
    private LoggedDataOutputStream loggedDataOutputStream;
    private boolean warned;

    /**
     * Construct a Client using a given connection and file handler. You must
     * initialize the connection and adminHandler first. <code>
     *  // establish connection to the given CVS pserver
     *  PServerConnection connection = new PServerConnection();
     *  connection.setUserName(userName);
     *  connection.setHostName(hostName);
     *  connection.setEncodedPassword(StandardScrambler.getInstance().scramble(password));
     *  connection.setRepository(repository);
     *  // test the connection
     *  try {
     *      connection.open();
     *  } catch (AuthenticationException e) {
     *      // do something
     *  }
     * 
     *  // create a CVS client
     *  Client cvsClient = new Client(connection,new StandardAdminHandler());
     * 
     *  // set the directory in which we work
     *  cvsClient.setLocalPath(localPath);
     * </code>
     * 
     * @param connection
     *            the connection to the cvs server
     * @param adminHandler
     *            the admin handler to use
     */
    public Client(final Connection connection, final AdminHandler adminHandler) {
        setConnection(connection);
        setAdminHandler(adminHandler);
        ignoreFileFilter = new DefaultIgnoreFileFilter();
        dontUseGzipFileHandler = false;
    }

    /**
     * Get the connection used for communicating with the server. Connection.
     * 
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Set the connection used for communicating with the server.
     * 
     * @param c
     *            the connection to use for all communication with the server
     */
    public void setConnection(final Connection connection) {
        this.connection = connection;
        initialRequestsSent = false;
        setIsFirstCommand(true);
    }

    /**
     * Get the admin handler uesd to read and write administrative information
     * about files on the local machine.
     * 
     * @return the admin handler
     */
    public AdminHandler getAdminHandler() {
        return adminHandler;
    }

    /**
     * Set the admin handler used to read and write administrative information
     * about files on the local machine.
     */
    public void setAdminHandler(final AdminHandler adminHandler) {
        this.adminHandler = adminHandler;
    }

    /**
     * Get the local path; that is, the path to the directory in which the
     * currently executing command is referring.
     */
    public String getLocalPath() {
        return localPath;
    }

    /**
     * Set the local path, i.e. the path to the directory in which all commands
     * are given (top level).
     */
    public void setLocalPath(String localPath) {
        // remove trailing (back-) slash
        localPath = localPath.replace('\\', '/');
        while (localPath.endsWith("/")) { // NOI18N
            localPath = localPath.substring(0, localPath.length() - 1);
        }

        this.localPath = localPath;
    }

    /**
     * Returns true if no previous command was executed using thiz.
     */
    public boolean isFirstCommand() {
        return isFirstCommand;
    }

    /**
     * Set whether this is the first command. Normally you do not need to set
     * this yourself - after execution the first command will have set this to
     * false.
     */
    public void setIsFirstCommand(final boolean isFirstCommand) {
        this.isFirstCommand = isFirstCommand;
    }

    /**
     * Return the uncompressed file handler.
     */
    public FileHandler getUncompressedFileHandler() {
        return uncompFileHandler;
    }

    /**
     * Set the uncompressed file handler.
     */
    public void setUncompressedFileHandler(final FileHandler uncompFileHandler) {
        this.uncompFileHandler = uncompFileHandler;
    }

    /**
     * Return the Gzip stream handler.
     */
    public FileHandler getGzipFileHandler() {
        return gzipFileHandler;
    }

    /**
     * Set the handler for Gzip data.
     */
    public void setGzipFileHandler(final FileHandler gzipFileHandler) {
        this.gzipFileHandler = gzipFileHandler;
    }

    /**
     * ReSet the filehandler for Gzip compressed data. Makes sure the requests
     * for sending gzipped data are not sent..
     */
    public void dontUseGzipFileHandler() {
        setGzipFileHandler(new DefaultFileHandler());
        dontUseGzipFileHandler = true;
    }

    public boolean isAborted() {
        return abort;
    }

    /**
     * Ensures, that the connection is open.
     * 
     * @throws AuthenticationException
     *             if it wasn't possible to connect
     */
    public void ensureConnection() throws AuthenticationException {
        BugLog.getInstance().assertNotNull(getConnection());

        if (getConnection().isOpen()) {
            return;
        }

        // #69689 detect silent servers, possibly caused by proxy errors
        final Throwable ex[] = new Throwable[1];
        final boolean opened[] = new boolean[] { false };
        final Runnable probe = new Runnable() {
            public void run() {
                try {
                    getConnection().open();
                    synchronized (opened) {
                        opened[0] = true;
                    }
                } catch (final Throwable e) {
                    synchronized (ex) {
                        ex[0] = e;
                    }
                }
            }
        };

        final Thread probeThread = new Thread(probe, "CVS Server Probe"); // NOI18N
        probeThread.start();
        try {

            probeThread.join(60 * 1000); // 1 min

            Throwable wasEx;
            synchronized (ex) {
                wasEx = ex[0];
            }
            if (wasEx != null) {
                if (wasEx instanceof CommandAbortedException) {
                    // User cancelled
                    abort();
                    return;
                } else if (wasEx instanceof AuthenticationException) {
                    throw (AuthenticationException) wasEx;
                } else if (wasEx instanceof RuntimeException) {
                    throw (RuntimeException) wasEx;
                } else if (wasEx instanceof Error) {
                    throw (Error) wasEx;
                } else {
                    assert false : wasEx;
                }
            }

            boolean wasOpened;
            synchronized (opened) {
                wasOpened = opened[0];
            }
            if (wasOpened == false) {
                probeThread.interrupt();
                throw new AuthenticationException("Timeout, no response from server.",
                                "Timeout, no response from server.");
            }

        } catch (final InterruptedException e) {

            // User cancelled
            probeThread.interrupt();
            abort();
        }
    }

    /**
     * Process all the requests. The connection must have been opened and set
     * first.
     * 
     * @param requests
     *            the requets to process
     */
    public void processRequests(final List<Request> requests) throws IOException, UnconfiguredRequestException,
                    ResponseException, CommandAbortedException {

        if ((requests == null) || (requests.size() == 0)) {
            throw new IllegalArgumentException("[processRequests] requests " + // NOI18N
                            "was either null or empty."); // NOI18N
        }

        if (abort) {
            throw new CommandAbortedException("Aborted during request processing", // NOI18N
                            CommandException.getLocalMessage("Client.commandAborted", null)); // NOI18N
        }

        loggedDataInputStream = null;
        loggedDataOutputStream = null;

        // send the initialisation requests if we are handling the first
        // command
        boolean filterRootRequest = true;
        if (isFirstCommand()) {
            setIsFirstCommand(false);
            int pos = 0;
            if (!initialRequestsSent) {
                pos = fillInitialRequests(requests);
                initialRequestsSent = true;
                filterRootRequest = false;
            }
            if (globalOptions != null) {
                // sends the global options that are to be sent to server (-q,
                // -Q, -t, -n, l)
                for (final Request request2 : globalOptions.createRequestList()) {
                    final Request request = request2;
                    requests.add(pos++, request);
                }

                if (globalOptions.isUseGzip() && (globalOptions.getCompressionLevel() != 0)) {
                    requests.add(pos++, new GzipFileContentsRequest(globalOptions.getCompressionLevel()));
                }
            }
        } else if (printConnectionReuseWarning) {
            if (System.getProperty("javacvs.multiple_commands_warning") == null) { // NOI18N
                System.err.println("WARNING TO DEVELOPERS:"); // NOI18N
                System.err.println("Please be warned that attempting to reuse one open connection for more commands is not supported by cvs servers very well."); // NOI18N
                System.err.println("You are advised to open a new Connection each time."); // NOI18N
                System.err.println("If you still want to proceed, please do: System.setProperty(\"javacvs.multiple_commands_warning\", \"false\")"); // NOI18N
                System.err.println("That will disable this message."); // NOI18N
            }
        }

        if (!ALLOWED_CONNECTION_REUSE_REQUESTS.contains(requests.get(requests.size() - 1).getClass())) {
            printConnectionReuseWarning = true;
        }

        final boolean fireEnhancedEvents = getEventManager().isFireEnhancedEventSet();
        int fileDetailRequestCount = 0;

        if (fireEnhancedEvents) {
            for (final Request request2 : requests) {
                final Request request = request2;

                final FileDetails fileDetails = request.getFileForTransmission();
                if ((fileDetails != null) && fileDetails.getFile().exists()) {
                    fileDetailRequestCount++;
                }
            }
            final CVSEvent event = new EnhancedMessageEvent(this, EnhancedMessageEvent.REQUESTS_COUNT, new Integer(
                            fileDetailRequestCount));
            getEventManager().fireCVSEvent(event);
        }

        LoggedDataOutputStream dos = connection.getOutputStream();
        loggedDataOutputStream = dos;

        // this list stores stream modification requests, each to be called
        // to modify the input stream the next time we need to process a
        // response
        final List<Request> streamModifierRequests = new LinkedList<Request>();

        // sending files does not seem to allow compression
        transmitFileHandler = getUncompressedFileHandler();

        for (final Request request2 : requests) {
            if (abort) {
                throw new CommandAbortedException("Aborted during request processing", // NOI18N
                                CommandException.getLocalMessage("Client.commandAborted", null)); // NOI18N
            }

            final Request request = request2;

            if (request instanceof GzipFileContentsRequest) {
                if (dontUseGzipFileHandler) {
                    continue;
                }
            }

            // skip the root request if already sent
            if (request instanceof RootRequest) {
                if (filterRootRequest) {
                    continue;
                } else { // Even if we should not filter the RootRequest now, we
                         // must filter all successive RootRequests
                    filterRootRequest = true;
                }
            }
            // send request to server
            final String requestString = request.getRequestString();
            dos.writeBytes(requestString);

            // we must modify the outputstream now, but defer modification
            // of the inputstream until we are about to read a response.
            // This is because some modifiers (e.g. gzip) read the header
            // on construction, and obviously no header is present when
            // no response has been sent
            request.modifyOutputStream(connection);
            if (request.modifiesInputStream()) {
                streamModifierRequests.add(request);
            }
            dos = connection.getOutputStream();

            final FileDetails fileDetails = request.getFileForTransmission();
            if (fileDetails != null) {
                final File file = fileDetails.getFile();
                // only transmit the file if it exists! When committing
                // a remove request you cannot transmit the file
                if (file.exists()) {
                    Logger.logOutput(new String("<Sending file: " + // NOI18N
                                    file.getAbsolutePath() + ">\n").getBytes("utf8")); // NOI18N

                    if (fireEnhancedEvents) {
                        final CVSEvent event = new EnhancedMessageEvent(this, EnhancedMessageEvent.FILE_SENDING, file);
                        getEventManager().fireCVSEvent(event);

                        fileDetailRequestCount--;
                    }

                    if (fileDetails.isBinary()) {
                        transmitFileHandler.transmitBinaryFile(file, dos);
                    } else {
                        transmitFileHandler.transmitTextFile(file, dos);
                    }

                    if (fireEnhancedEvents && (fileDetailRequestCount == 0)) {
                        final CVSEvent event = new EnhancedMessageEvent(this, EnhancedMessageEvent.REQUESTS_SENT, "Ok"); // NOI18N
                        getEventManager().fireCVSEvent(event);
                    }
                }
            }
            if (request.isResponseExpected()) {
                dos.flush();

                // now perform the deferred modification of the input stream
                final Iterator<Request> modifiers = streamModifierRequests.iterator();
                while (modifiers.hasNext()) {
                    System.err.println("Modifying the inputstream..."); // NOI18N
                    final Request smRequest = modifiers.next();
                    System.err.println("Request is a: " + // NOI18N
                                    smRequest.getClass().getName());
                    smRequest.modifyInputStream(connection);
                }
                streamModifierRequests.clear();

                handleResponse();
            }
        }
        dos.flush();

        transmitFileHandler = null;
    }

    private ResponseFactory getResponseFactory() {
        if (responseFactory == null) {
            responseFactory = new ResponseFactory();
        }
        return responseFactory;
    }

    /**
     * Handle the response from a request.
     * 
     * @throws ResponseException
     *             if there is a problem reading the response
     */
    private void handleResponse() throws ResponseException, CommandAbortedException {
        try {
            final LoggedDataInputStream dis = connection.getInputStream();
            loggedDataInputStream = dis;

            int ch = -1;
            try {
                ch = dis.read();
            } catch (final InterruptedIOException ex) {
                abort();
            }

            while (!abort && (ch != -1)) {
                final StringBuffer responseNameBuffer = new StringBuffer();
                // read in the response name
                while ((ch != -1) && ((char) ch != '\n') && ((char) ch != ' ')) {
                    responseNameBuffer.append((char) ch);
                    try {
                        ch = dis.read();
                    } catch (final InterruptedIOException ex) {
                        abort();
                        break;
                    }
                }

                final String responseString = responseNameBuffer.toString();
                final Response response = getResponseFactory().createResponse(responseString);
                // Logger.logInput(new String("<" + responseString +
                // " processing start>\n").getBytes()); // NOI18N
                response.process(dis, this);
                final boolean terminal = response.isTerminalResponse();

                // handle SpecialResponses
                if (terminal && (response instanceof ErrorMessageResponse)) {
                    final ErrorMessageResponse errorResponce = (ErrorMessageResponse) response;
                    final String errMsg = errorResponce.getMessage();
                    throw new CommandAbortedException(errMsg, errMsg);
                }
                // Logger.logInput(new String("<" + responseString +
                // " processed " + terminal + ">\n").getBytes()); // NOI18N
                if (terminal || abort) {
                    break;
                }

                try {
                    ch = dis.read();
                } catch (final InterruptedIOException ex) {
                    abort();
                    break;
                }
            }

            if (abort) {
                final String localMsg = CommandException.getLocalMessage("Client.commandAborted", null); // NOI18N
                throw new CommandAbortedException("Aborted during request processing", localMsg); // NOI18N
            }
        } catch (final EOFException ex) {
            throw new ResponseException(ex, CommandException.getLocalMessage("CommandException.EndOfFile", null)); // NOI18N
        } catch (final IOException ex) {
            throw new ResponseException(ex);
        }
    }

    /**
     * Execute a command. Do not forget to initialize the CVS Root on
     * globalOptions first! Example: <code>
     *   GlobalOptions options = new GlobalOptions();
     *   options.setCVSRoot(":pserver:"+userName+"@"+hostName+":"+cvsRoot);
     * </code>
     * 
     * @param command
     *            the command to execute
     * @param options
     *            the global options to use for executing the command
     * @throws CommandException
     *             if an error occurs when executing the command
     * @throws CommandAbortedException
     *             if the command is aborted
     */
    public boolean executeCommand(final Command command, final GlobalOptions globalOptions) throws CommandException,
                    CommandAbortedException, AuthenticationException {
        BugLog.getInstance().assertNotNull(command);
        BugLog.getInstance().assertNotNull(globalOptions);

        this.globalOptions = globalOptions;

        getUncompressedFileHandler().setGlobalOptions(globalOptions);
        getGzipFileHandler().setGlobalOptions(globalOptions);

        try {
            eventManager.addCVSListener(command);
            command.execute(this, eventManager);
        } finally {
            eventManager.removeCVSListener(command);
        }
        return !command.hasFailed();
    }

    /**
     * Counts {@link #processRequests(java.util.List)}. send and received bytes.
     * 
     * @thread it assumes that client is not run in paralel.
     */
    public long getCounter() {
        long ret = 0;
        if (loggedDataInputStream != null) {
            ret += loggedDataInputStream.getCounter();
        }
        if (loggedDataOutputStream != null) {
            ret += loggedDataOutputStream.getCounter();
        }
        return ret;
    }

    /**
     * Convert a <i>pathname</i> in the CVS sense (see 5.10 in the protocol
     * document) into a local absolute pathname for the file.
     * 
     * @param localDirectory
     *            the name of the local directory, relative to the directory in
     *            which the command was given
     * @param repository
     *            the full repository name for the file
     */
    public String convertPathname(String localDirectory, final String repository) {
        final int lastIndexOfSlash = repository.lastIndexOf('/');
        final String filename = repository.substring(lastIndexOfSlash + 1);

        if (localDirectory.startsWith("./")) { // NOI18N
            // remove the dot
            localDirectory = localDirectory.substring(1);
        }
        if (localDirectory.startsWith("/")) { // NOI18N
            // remove the leading slash
            localDirectory = localDirectory.substring(1);
        }
        // note that the localDirectory ends in a slash
        return getLocalPath() + '/' + localDirectory + filename;
    }

    /**
     * Get the repository path from the connection.
     * 
     * @return the repository path, e.g. /home/bob/cvs. Delegated to the
     *         Connection in this case
     * @see Connection#getRepository()
     */
    public String getRepository() {
        return connection.getRepository();
    }

    /**
     * Create or update the administration files for a particular file. This
     * will create the CVS directory if necessary, and the Root and Repository
     * files if necessary. It will also update the Entries file with the new
     * entry
     * 
     * @param localDirectory
     *            the local directory, relative to the directory in which the
     *            command was given, where the file in question lives
     * @param repositoryPath
     *            the path of the file in the repository, in absolute form.
     * @param entry
     *            the entry object for that file
     */
    public void updateAdminData(final String localDirectory, String repositoryPath, final Entry entry)
                    throws IOException {
        final String absolutePath = localPath + '/' + localDirectory;
        if (repositoryPath.startsWith(getRepository())) {
            repositoryPath = repositoryPath.substring(getRepository().length() + 1);
        } else {
            if (warned == false) {
                String warning = "#65188 warning C/S protocol error (section 5.10). It's regurarly observed with cvs 1.12.xx servers.\n"; // NOI18N
                warning += "  unexpected pathname=" + repositoryPath + " missing root prefix=" + getRepository() + "\n"; // NOI18N
                warning += "  relaxing, but who knows all consequences...."; // NOI18N
                System.err.println(warning);
                warned = true;
            }
        }

        adminHandler.updateAdminData(absolutePath, repositoryPath, entry, globalOptions);
    }

    /**
     * Set the modified date of the next file to be written. The next call to
     * write<Type>File will use this date.
     * 
     * @param modifiedDate
     *            the date the file should be marked as modified
     */
    public void setNextFileDate(final Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * Get the modified date for the next file.
     * 
     * @return the date and then null the instance variable.
     */
    public Date getNextFileDate() {
        //
        // We null the instance variable so that future calls will not
        // retrieve a date specified for a previous file
        //
        final Date copy = modifiedDate;
        modifiedDate = null;
        return copy;
    }

    /**
     * Get the Entry for the specified file, if one exists.
     * 
     * @param f
     *            the file
     * @throws IOException
     *             if the Entries file cannot be read
     */
    public Entry getEntry(final File f) throws IOException {
        return adminHandler.getEntry(f);
    }

    /**
     * Get the entries for a specified directory.
     * 
     * @param directory
     *            the directory for which to get the entries
     * @return an iterator of Entry objects
     */
    public Iterator<Entry> getEntries(final File directory) throws IOException {
        return adminHandler.getEntries(directory);
    }

    public boolean exists(final File file) {
        return adminHandler.exists(file);
    }

    /**
     * Get the repository path for a given directory, for example in the
     * directory /home/project/foo/bar, the repository directory might be
     * /usr/cvs/foo/bar. The repository directory is commonly stored in the file
     * 
     * <pre>
     * Repository
     * </pre>
     * 
     * in the CVS directory on the client (this is the case in the standard CVS
     * command-line tool).
     * 
     * If no
     * 
     * <pre>
     * CVS / Repository
     * </pre>
     * 
     * file was found, the specified directory, the localpath are used to
     * "guess" the repository path.
     * 
     * @param directory
     *            the directory
     */
    public String getRepositoryForDirectory(String directory) throws IOException {
        try {
            final String repository = adminHandler.getRepositoryForDirectory(directory, getRepository());
            return repository;
        } catch (final IOException ex) {
            // an IOException is thrown, if the adminHandler can't detect the
            // repository
            // by reading the CVS/Repository file, e.g. when checking out into a
            // new directory
            try {
                directory = new File(directory).getCanonicalPath();
            } catch (final IOException ioex) {
            }
            directory = directory.replace('\\', '/');
            while (directory.endsWith("/")) { // NOI18N
                directory = directory.substring(0, directory.length() - 1);
            }

            // must also canonicalize 'localPath' to be in sync with 'directory'
            String localPathCanonical = getLocalPath();
            try {
                localPathCanonical = new File(getLocalPath()).getCanonicalPath();
            } catch (final IOException ioex) {
            }
            localPathCanonical = localPathCanonical.replace('\\', '/');
            while (localPathCanonical.endsWith("/")) { // NOI18N
                localPathCanonical = localPathCanonical.substring(0, localPathCanonical.length() - 1);
            }
            final int localPathLength = localPathCanonical.length();

            String repository;
            if (directory.length() >= localPathLength) {
                repository = getRepository() + directory.substring(localPathLength);
            } else { // Asking for some folder upon the local working path
                repository = getRepository();
            }
            return repository;
        }
    }

    public String getRepositoryForDirectory(final File directory) throws IOException {
        return adminHandler.getRepositoryForDirectory(directory.getAbsolutePath(), getRepository());
    }

    /**
     * Set the Entry for the specified file.
     * 
     * @param file
     *            the file
     * @param entry
     *            the new entry
     * @throws IOException
     *             if an error occurs writing the details
     */
    public void setEntry(final File file, final Entry entry) throws IOException {
        adminHandler.setEntry(file, entry);
    }

    /**
     * Remove the Entry for the specified file.
     * 
     * @param file
     *            the file whose entry is to be removed
     * @throws IOException
     *             if an error occurs writing the Entries file
     */
    public void removeEntry(final File file) throws IOException {
        adminHandler.removeEntry(file);
    }

    /**
     * Remove the specified file from the local disk. If the file does not
     * exist, the operation does nothing.
     * 
     * @param pathname
     *            the full path to the file to remove
     * @throws IOException
     *             if an IO error occurs while removing the file
     */
    public void removeLocalFile(final String pathname) throws IOException {
        transmitFileHandler.removeLocalFile(pathname);
    }

    /**
     * Removes the specified file determined by pathName and repositoryName. In
     * this implementation the filename from repositoryPath is added to the
     * localpath (which doesn't have the filename in it) and that file is
     * deleted.
     */
    public void removeLocalFile(final String pathName, final String repositoryName) throws IOException {
        final int ind = repositoryName.lastIndexOf('/');
        if (ind <= 0) {
            return;
        }

        final String fileName = repositoryName.substring(ind + 1);
        final String localFile = pathName + fileName;
        final File fileToDelete = new File(getLocalPath(), localFile);
        removeLocalFile(fileToDelete.getAbsolutePath());
        removeEntry(fileToDelete);
    }

    public void copyLocalFile(final String pathname, final String newName) throws IOException {
        transmitFileHandler.copyLocalFile(pathname, newName);
    }

    /**
     * Get the CVS event manager. This is generally called by response handlers
     * that want to fire events.
     * 
     * @return the eventManager
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Get the global options that are set to this client. Individual commands
     * can get the global options via this method.
     */
    public GlobalOptions getGlobalOptions() {
        return globalOptions;
    }

    /**
     * Call this method to abort the current command. The command will be
     * aborted at the next suitable time
     */
    public synchronized void abort() {
        abort = true;
    }

    /**
     * Get all the files contained within a given directory that are <b>known to
     * CVS</b>.
     * 
     * @param directory
     *            the directory to look in
     * @return a set of all files.
     */
    public Set<File> getAllFiles(final File directory) throws IOException {
        return adminHandler.getAllFiles(directory);
    }

    public void setIgnoreFileFilter(final IgnoreFileFilter ignoreFileFilter) {
        this.ignoreFileFilter = ignoreFileFilter;
    }

    public IgnoreFileFilter getIgnoreFileFilter() {
        return ignoreFileFilter;
    }

    public boolean shouldBeIgnored(final File directory, final String noneCvsFile) {
        if (ignoreFileFilter != null) {
            return ignoreFileFilter.shouldBeIgnored(directory, noneCvsFile);
        }
        return false;
    }

    /**
     * Checks for presence of CVS/Tag file and returns it's value.
     * 
     * @return the value of CVS/Tag file for the specified directory null if
     *         file doesn't exist
     */
    public String getStickyTagForDirectory(final File directory) {
        return adminHandler.getStickyTagForDirectory(directory);
    }

    /**
     * This method is called when a response for the ValidRequests request is
     * received.
     * 
     * @param requests
     *            A List of requests that is valid for this CVS server separated
     *            by spaces.
     */
    public void setValidRequests(final String requests) {
        // We need to tokenize the requests and add it to our map

        final StringTokenizer tokenizer = new StringTokenizer(requests);
        String token;
        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();
            // we just add an object with the corresponding request
            // as the key.
            validRequests.put(token, this);
        }

    }

    private int fillInitialRequests(final List<Request> requests) {
        int pos = 0;
        requests.add(pos++, new RootRequest(getRepository()));
        requests.add(pos++, new UseUnchangedRequest());
        requests.add(pos++, new ValidRequestsRequest());
        requests.add(pos++, new ValidResponsesRequest());
        return pos;
    }

    /**
     * This method is called by WrapperSendResponse for each wrapper setting
     * sent back by the CVS server
     * 
     * @param pattern
     *            A StringPattern indicating the pattern for which the wrapper
     *            applies
     * @param option
     *            A KeywordSubstituionOption corresponding to the setting
     */
    public void addWrapper(final StringPattern pattern, final KeywordSubstitutionOptions option) {
        if (wrappersMap == null) {
            throw new IllegalArgumentException("This method should be called " + "by WrapperSendResponse only.");
        }
        wrappersMap.put(pattern, option);
    }

    /**
     * Returns the wrappers map associated with the CVS server The map is valid
     * only after the connection is established
     */
    public Map<StringPattern, KeywordSubstitutionOptions> getWrappersMap() throws CommandException {
        if (wrappersMap == null) {
            wrappersMap = new HashMap<StringPattern, KeywordSubstitutionOptions>();
            final List<Request> requests = new ArrayList<Request>();
            requests.add(new WrapperSendRequest());
            final boolean isFirst = isFirstCommand();
            try {
                processRequests(requests);
            } catch (final Exception ex) {
                throw new CommandException(ex, "An error during obtaining server wrappers.");
            } finally {
                // Do not alter isFirstCommand property
                setIsFirstCommand(isFirst);
            }
            wrappersMap = Collections.<StringPattern, KeywordSubstitutionOptions> unmodifiableMap(wrappersMap);
        }
        return wrappersMap;
    }

    /**
     * Factory for creating clients.
     */
    public static interface Factory {

        /**
         * Creates new client instance. Never null. It uses fresh connection.
         */
        Client createClient();
    }
}
