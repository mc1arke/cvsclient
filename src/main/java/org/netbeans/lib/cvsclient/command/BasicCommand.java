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
package org.netbeans.lib.cvsclient.command;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.netbeans.lib.cvsclient.ClientServices;
import org.netbeans.lib.cvsclient.admin.DateComparator;
import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.request.ArgumentRequest;
import org.netbeans.lib.cvsclient.request.DirectoryRequest;
import org.netbeans.lib.cvsclient.request.EntryRequest;
import org.netbeans.lib.cvsclient.request.ModifiedRequest;
import org.netbeans.lib.cvsclient.request.QuestionableRequest;
import org.netbeans.lib.cvsclient.request.Request;
import org.netbeans.lib.cvsclient.request.RootRequest;
import org.netbeans.lib.cvsclient.request.StickyRequest;
import org.netbeans.lib.cvsclient.request.UnchangedRequest;

/**
 * A class that provides common functionality for many of the CVS command that
 * send similar sequences of requests.
 * 
 * @author Robert Greig
 */
public abstract class BasicCommand extends BuildableCommand {
    /**
     * 
     */
    private static final long serialVersionUID = -1125981549032452939L;

    /**
     * The requests that are sent and processed.
     */
    protected List<Request> requests = new LinkedList<Request>();

    /**
     * The client services that are provided to this command.
     */
    protected ClientServices clientServices;

    /**
     * Whether to update recursively.
     */
    private boolean recursive = true;

    /**
     * The files and/or directories to operate on.
     */
    protected File[] files;

    /**
     * Gets the value of the recursive option.
     * 
     * @return true if recursive, false if not
     * @deprecated use isRecursive instead
     */
    @Deprecated
    public boolean getRecursive() {
        return recursive;
    }

    /**
     * Gets the value of the recursive option.
     * 
     * @return true if recursive, false if not
     */
    public boolean isRecursive() {
        return recursive;
    }

    /**
     * Sets the value of the recursive option.
     * 
     * @param recursive
     *            true if the command should recurse, false otherwise
     */
    public void setRecursive(final boolean recursive) {
        this.recursive = recursive;
    }

    /**
     * Return true if file is a symbolic link.
     * Symbolic links are ignored by the major cvs clients.
     * Symbolic link to dir within cvs tree can cause infinate loop of cvs update following symlink. 
     * Solution when recursive check is directory a symlink and ignore it if so.
     * JENKINS-23234: jenkins cvs update hang when recursive symlink in directory
     * @param file name of file/dir/symlink to test
     * @return true if file is actually a symbolic link, false if not 
     */
    public static boolean isSymLink(File file) {
        if (file == null)
            return false;
        try {
            File canon;
            if (file.getParent() == null) {
                canon = file;
            } else {
                File canonDir = file.getParentFile().getCanonicalFile();
                canon = new File(canonDir, file.getName());
            }
            return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
        } catch (IOException ex) {
            System.err.println("isSymLink exception:" + ex);
        }
        return false;
    }


    /**
     * Set the files and/or directories on which to execute the command. The way
     * these are processed is:
     * <P>
     * <UL>
     * <LI>Default action (i.e. not setting the files explicitly or setting them
     * to
     * 
     * <pre>
     * null
     * </pre>
     * 
     * ) is to use the directory in which the command was executed (see how
     * directories are treated, below)</LI>
     * <LI>Files are handled how you would expect</LI>
     * <LI>For directories, all files within the directory are sent</LI>
     * </UL>
     * 
     * @param theFiles
     *            the files to operate on. May be null to indicate that the
     *            local directory specified in the client should be used. Full,
     *            absolute canonical pathnames <b>must</b> be supplied.
     */
    public void setFiles(final File[] theFiles) {
        // sort array.. files first, directories follow
        if (theFiles == null) {
            files = theFiles;
            return;
        }

        // assert theFiles.length > 0 : "Empty array causes random AIOOBEs!"; //
        // ClientRuntime.java:119

        files = new File[theFiles.length];
        int fileCount = 0;
        int dirCount = 0;
        final int totalCount = theFiles.length;
        for (int index = 0; index < totalCount; index++) {
            final File currentFile = theFiles[index];
            if (currentFile.isDirectory()) {
                files[totalCount - (1 + dirCount)] = currentFile;
                dirCount = dirCount + 1;
            } else {
                files[fileCount] = currentFile;
                fileCount = fileCount + 1;
            }
        }
    }

    /**
     * Get the files and/or directories specified for this command to operate
     * on.
     * 
     * @return the array of Files
     */
    public File[] getFiles() {
        return files;
    }

    /**
     * Get a single file from the "files" list. returns only files, not
     * directories. This method is used from within the builders, because for
     * single file requests, the cvs server doesn't return us enough information
     * to identify what file has been returned. Thus we sort the "files" array
     * (files come before directories. Then the response froms erver comes in
     * the same order and the files can be found this way.
     * 
     * @param index
     *            the index of the file in the list.
     */
    public File getXthFile(final int index) {
        if ((index < 0) || (index >= files.length)) {
            return null;
        }
        final File file = files[index];
        if (!file.isFile()) {
            return null;
        }
        return file;
    }

    /**
     * @param ending
     *            - the ending part of the file's pathname.. path separator is
     *            cvs's default '/'
     */
    public File getFileEndingWith(final String ending) {
        final String locEnding = ending.replace('\\', '/');
        final String localDir = getLocalDirectory().replace('\\', '/');
        int index = 0;
        for (index = 0; index < files.length; index++) {
            String path = files[index].getAbsolutePath();
            final String parentPath = files[index].getParentFile().getAbsolutePath().replace('\\', '/');
            path = path.replace('\\', '/');
            if ((path.endsWith(locEnding) && (locEnding.indexOf('/') >= 0))
                            || (files[index].getName().equals(locEnding) && parentPath.equals(localDir))) {
                return files[index];
            }
        }
        return null;
    }

    /**
     * Add the appropriate requests for a specified path. For a directory,
     * process all the files within that directory and for a single file, just
     * send it. For each directory, send a directory request. For each file send
     * an Entry request followed by a Modified request.
     * 
     * @param path
     *            the particular path to issue requests for. May be either a
     *            file or a directory.
     */
    private void addRequests(final File path) throws FileNotFoundException, IOException, CommandAbortedException {
        if (path == null) {
            throw new IllegalArgumentException("Cannot add requests for a " + "null path.");
        }

        if (!path.exists() || path.isFile()) {
            addRequestsForFile(path);
        } else {
            addRequestsForDirectory(path);
        }
    }

    /**
     * Should return true if unchanged files should not be sent to server. If
     * false is returned, all files will be sent to server This method is used
     * by <code>sendEntryAndModifiedRequests</code>.
     */
    protected boolean doesCheckFileTime() {
        return true;
    }

    /**
     * Send an Entry followed by a Modified or Unchanged request based on
     * whether the file has been untouched on the local machine.
     * 
     * @param entry
     *            the entry for the file
     * @param file
     *            the file in question
     */
    protected void sendEntryAndModifiedRequests(Entry entry, final File file) {
        if (entry == null) {
            return;
        }

        // for deleted added files, don't send anything..
        if ((file != null) && !file.exists() && entry.isNewUserFile()) {
            return;
        }

        final Date entryLastModified = entry.getLastModified();
        final boolean hadConflicts = entry.hadConflicts();
        if (!hadConflicts) {
            // we null out the conflict field if there is no conflict
            // because we use that field to store the timestamp of the
            // file (just like command-line CVS). There is no point
            // in sending this information to the CVS server, even
            // though it should be ignored by the server.
            entry.setConflict(null);
        } else if (fileRequiresConflictResolution(file, entry)) {
            // send entry in wire conflict format
            final Entry clone = new Entry(entry.toString());
            clone.setConflict(Entry.HAD_CONFLICTS_AND_TIMESTAMP_MATCHES_FILE);
            entry = clone;
        }
        addRequest(new EntryRequest(entry));

        if ((file == null) || !file.exists() || entry.isUserFileToBeRemoved()) {
            return;
        }

        if (doesCheckFileTime() && !hadConflicts && (entryLastModified != null)) {
            if (DateComparator.getInstance().equals(file.lastModified(), entryLastModified.getTime())) {
                addRequest(new UnchangedRequest(file.getName()));
                return;
            }
        }

        addRequest(new ModifiedRequest(file, entry.isBinary()));
    }

    /**
     * When committing, we need to perform a check that will prevent the
     * unmodified conflicting files from being checked in. This is the behavior
     * of command line CVS client. This method checks the Entry for the file
     * against the time stamp. The user can optimally call this method only if
     * the Entry for the file indicates a conflict
     * 
     * @param entry
     *            The Entry object corresponding to the file
     * @param file
     *            The File object representing the file on the filesystem
     * @return boolean Returns true if the file's timestamp is same or less than
     *         the time when the conflicting merge was done by CVS update as
     *         indicated by the Entry.
     */
    private final boolean fileRequiresConflictResolution(final File file, final Entry entry) {

        if (file == null) {
            return false; // null file is set by clean copy
        }

        boolean ret = false;

        if (entry.hadConflicts()) {
            // TODO introduce common timestamp comparation logic
            // We only test accuracy of upto a second (1000ms) because that is
            // the precision of the timestamp in the entries file
            final long mergedTime = entry.getLastModified().getTime() / 1000;
            final long timeStampOfFile = file.lastModified() / 1000;
            ret = timeStampOfFile <= mergedTime;
        }

        return ret;
    }

    /**
     * Adds the appropriate requests for a given directory. Sends a directory
     * request followed by as many Entry and Modified requests as required
     * 
     * @param directory
     *            the directory to send requests for
     * @throws IOException
     *             if an error occurs constructing the requests
     */
    protected void addRequestsForDirectory(final File directory) throws IOException, CommandAbortedException {
        if (!clientServices.exists(directory)) {
            return;
        }
        if (clientServices.isAborted()) {
            throw new CommandAbortedException("Command aborted during request generation",
                            "Command aborted during request generation");
        }

        addDirectoryRequest(directory);

        final File[] dirFiles = directory.listFiles();
        List<File> localFiles;
        if (dirFiles == null) {
            localFiles = new ArrayList<File>(0);
        } else {
            localFiles = new ArrayList<File>(Arrays.asList(dirFiles));
            localFiles.remove(new File(directory, "CVS"));
        }

        List<File> subDirectories = null;
        if (isRecursive()) {
            subDirectories = new LinkedList<File>();
        }

        // get all the entries we know about, and process them
        for (final Iterator<?> it = clientServices.getEntries(directory); it.hasNext();) {
            final Entry entry = (Entry) it.next();
            final File file = new File(directory, entry.getName());

            if (!isSymLink(file)) {
                if (entry.isDirectory()) {
                    if (isRecursive()) {
                        subDirectories.add(new File(directory, entry.getName()));
                    }
                }
                else {
                    addRequestForFile(file, entry);
                }
            } 
            else {
                // JENKINS-23234: jenkins cvs update hang when recursive symlink in directory
                System.err.println("addRequestsForDirectory prevent potential infinate loop, ignoring symlink:" + file);
            }
            
            localFiles.remove(file);
        }

        // In case that CVS folder does not exist, we need to process all
        // directories that have CVS subfolders:
        if (isRecursive() && !new File(directory, "CVS").exists()) {
            final File[] subFiles = directory.listFiles();
            if (subFiles != null) {
                for (final File subFile : subFiles) {
                    if (subFile.isDirectory() && new File(subFile, "CVS").exists()) {
                        subDirectories.add(subFile);
                    }
                }
            }
        }

        for (final File file : localFiles) {
            final String localFileName = (file).getName();
            if (!clientServices.shouldBeIgnored(directory, localFileName)) {
                addRequest(new QuestionableRequest(localFileName));
            }
        }

        if (isRecursive()) {
            for (final File file : subDirectories) {
                final File subdirectory = file;
                final File cvsSubDir = new File(subdirectory, "CVS"); // NOI18N
                if (clientServices.exists(cvsSubDir)) {
                    addRequestsForDirectory(subdirectory);
                }
            }
        }
    }

    /**
     * This method is called for each explicit file and for files within a
     * directory.
     */
    protected void addRequestForFile(final File file, final Entry entry) {
        sendEntryAndModifiedRequests(entry, file);
    }

    /**
     * Add the appropriate requests for a single file. A directory request is
     * sent, followed by an Entry and Modified request
     * 
     * @param file
     *            the file to send requests for
     * @throws IOException
     *             if an error occurs constructing the requests
     */
    protected void addRequestsForFile(final File file) throws IOException {
        addDirectoryRequest(file.getParentFile());

        try {
            final Entry entry = clientServices.getEntry(file);
            // a non-null entry means the file does exist in the
            // Entries file for this directory
            if (entry != null) {
                addRequestForFile(file, entry);
            } else if (file.exists()) {
                // #50963 file exists locally without an entry AND the request
                // is
                // for the file explicitly
                final boolean unusedBinaryFlag = false;
                addRequest(new ModifiedRequest(file, unusedBinaryFlag));
            }
        } catch (final IOException ex) {
            System.err.println("An error occurred getting the Entry " + "for file " + file + ": " + ex);
            ex.printStackTrace();
        }
    }

    /**
     * Adds a DirectoryRequest (and maybe a StickyRequest) to the request list.
     */
    protected final void addDirectoryRequest(final File directory) {

        if (isSymLink(directory)) {
            // JENKINS-23234: jenkins cvs update hang when recursive symlink in directory
            System.err.println("addDirectoryRequest prevent potential infinate loop, ignoring symlink:" + directory);
        } 
        else {

            // remove localPath prefix from directory. If left with
            // nothing, use dot (".") in the directory request
            final String dir = getRelativeToLocalPathInUnixStyle(directory);

            try {
                final String repository = clientServices.getRepositoryForDirectory(directory.getAbsolutePath());
                addRequest(new DirectoryRequest(dir, repository));
                final String tag = clientServices.getStickyTagForDirectory(directory);
                if (tag != null) {
                    addRequest(new StickyRequest(tag));
                }
            } catch (final FileNotFoundException ex) {
                // we can ignore this exception safely because it just means
                // that the user has deleted a directory referenced in a
                // CVS/Entries file
            } catch (final IOException ex) {
                System.err.println("An error occurred reading the respository " + "for the directory " + dir + ": " + ex);
                ex.printStackTrace();
            }

        }

    }

    /**
     * Add the argument requests. The argument requests are created using the
     * original set of files/directories passed in. Subclasses of this class
     * should call this method at the appropriate point in their execute()
     * method. Note that arguments are appended to the list.
     */
    protected void addArgumentRequests() {
        if (files == null) {
            return;
        }

        for (final File file : files) {
            final String relativePath = getRelativeToLocalPathInUnixStyle(file);
            addRequest(new ArgumentRequest(relativePath, true));
        }
    }

    /**
     * Execute a command. This implementation sends a Root request, followed by
     * as many Directory and Entry requests as is required by the recurse
     * setting and the file arguments that have been set. Subclasses should call
     * this first, and tag on the end of the requests list any further requests
     * and, finally, the actually request that does the command (e.g.
     * 
     * <pre>
     * update
     * </pre>
     * 
     * ,
     * 
     * <pre>
     * status
     * </pre>
     * 
     * etc.)
     * 
     * @param client
     *            the client services object that provides any necessary
     *            services to this command, including the ability to actually
     *            process all the requests
     * @throws CommandException
     *             if an error occurs executing the command
     */
    @Override
    public void execute(final ClientServices client, final EventManager em) throws CommandException,
                    AuthenticationException {
        requests.clear();
        clientServices = client;
        super.execute(client, em);

        if (client.isFirstCommand()) {
            addRequest(new RootRequest(client.getRepository()));
        }

        addFileRequests();
    }

    private void addFileRequests() throws CommandException {
        try {
            if ((files != null) && (files.length > 0)) {
                for (final File file : files) {
                    addRequests(file);
                }
            } else {
                // if no arguments have been specified, then specify the
                // local directory - the "top level" for this command
                if (assumeLocalPathWhenUnspecified()) {
                    addRequests(new File(getLocalDirectory()));
                }
            }
        } catch (final Exception ex) {
            throw new CommandException(ex, ex.getLocalizedMessage());
        }
    }

    /**
     * The result from this command is used only when the getFiles() returns
     * null or empty array. in such a case and when this method returns true, it
     * is assumed the localpath should be taken as the 'default' file for the
     * building of requests. Generally assumed to be true. Can be overriden by
     * subclasses. However make sure you know what you are doing. :)
     */
    protected boolean assumeLocalPathWhenUnspecified() {
        return true;
    }

    /**
     * Adds the specified request to the request list.
     */
    protected final void addRequest(final Request request) {
        requests.add(request);
    }

    /**
     * Adds the request for the current working directory.
     */
    protected final void addRequestForWorkingDirectory(final ClientServices clientServices) throws IOException {
        addRequest(new DirectoryRequest(".", // NOI18N
                        clientServices.getRepositoryForDirectory(getLocalDirectory())));
    }

    /**
     * If the specified value is true, add a ArgumentRequest for the specified
     * argument.
     */
    protected final void addArgumentRequest(final boolean value, final String argument) {
        if (!value) {
            return;
        }

        addRequest(new ArgumentRequest(argument));
    }

    /**
     * Appends the file's names to the specified buffer.
     */
    protected final void appendFileArguments(final StringBuffer buffer) {
        final File[] files = getFiles();
        if (files == null) {
            return;
        }

        for (int index = 0; index < files.length; index++) {
            if (index > 0) {
                buffer.append(' ');
            }
            buffer.append(files[index].getName());
        }
    }
}
