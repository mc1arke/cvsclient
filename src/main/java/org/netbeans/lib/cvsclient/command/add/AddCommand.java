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
package org.netbeans.lib.cvsclient.command.add;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.netbeans.lib.cvsclient.ClientServices;
import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.command.BuildableCommand;
import org.netbeans.lib.cvsclient.command.Builder;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.KeywordSubstitutionOptions;
import org.netbeans.lib.cvsclient.command.WrapperUtils;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.MessageEvent;
import org.netbeans.lib.cvsclient.request.ArgumentRequest;
import org.netbeans.lib.cvsclient.request.ArgumentxRequest;
import org.netbeans.lib.cvsclient.request.CommandRequest;
import org.netbeans.lib.cvsclient.request.DirectoryRequest;
import org.netbeans.lib.cvsclient.request.EntryRequest;
import org.netbeans.lib.cvsclient.request.IsModifiedRequest;
import org.netbeans.lib.cvsclient.request.KoptRequest;
import org.netbeans.lib.cvsclient.request.Request;
import org.netbeans.lib.cvsclient.request.RootRequest;
import org.netbeans.lib.cvsclient.request.StickyRequest;
import org.netbeans.lib.cvsclient.util.SimpleStringPattern;
import org.netbeans.lib.cvsclient.util.StringPattern;

/**
 * Adds a file or directory.
 * 
 * @author Robert Greig
 */
public class AddCommand extends BuildableCommand {
    /**
     * 
     */
    private static final long serialVersionUID = -399479185279096573L;
    /**
     * Constants that identify a message that creates a directory in repository.
     */
    private static final String DIR_ADDED = " added to the repository"; // NOI18N
    private static final String DIRECTORY = "Directory "; // NOI18N

    /**
     * The requests that are sent and processed.
     */
    private List<Request> requests;

    /**
     * The argument requests that are collected and sent in the end just before
     * the add request.
     */
    private final List<Request> argumentRequests = new LinkedList<Request>();

    /**
     * The list of new directories.
     */
    /*
     * private HashMap newDirList;
     */
    private final List<Paths> newDirList = new LinkedList<Paths>();

    /**
     * The client services that are provided to this command.
     */
    private ClientServices clientServices;

    /**
     * The files and/or directories to operate on.
     */
    private File[] files;

    /**
     * Holds value of property message, (add's switch -m).
     */
    private String message;

    /**
     * Holds value of property keywordSubst.
     */
    private KeywordSubstitutionOptions keywordSubst;

    /**
     * This is the merged wrapper map that contains has both server side and
     * client side wrapper settings merged except for the .cvswrappers in the
     * individual directories
     */
    private Map<StringPattern, KeywordSubstitutionOptions> wrapperMap;

    /**
     * Holds the cvswrappers map for each directory, keyed by directory name
     */
    private final HashMap<String, Map<StringPattern, KeywordSubstitutionOptions>> dir2WrapperMap = new HashMap<String, Map<StringPattern, KeywordSubstitutionOptions>>(
                    16);

    private static final Map<StringPattern, KeywordSubstitutionOptions> EMPTYWRAPPER = new HashMap<StringPattern, KeywordSubstitutionOptions>(
                    1);

    /**
     * Constructor.
     */
    public AddCommand() {
        resetCVSCommand();
    }

    /**
     * Set the files and/or directories on which to execute the command. Sorts
     * the paameter so that directories are first and files follow. That way a
     * directory and it's content will be passed correctly. The user of the
     * library has to specify all the files+dirs being added though. This is
     * just a sanity check, so that no unnessesary errors occur.
     */
    public void setFiles(final File[] files) {
        this.files = files;
        if (files == null) {
            return;
        }

        // sort array: directories first, files follow
        this.files = new File[files.length];
        int dirCount = 0;
        int fileCount = 0;
        final int totalCount = files.length;
        for (int index = 0; index < totalCount; index++) {
            final File currentFile = files[index];
            if (currentFile.isDirectory()) {
                this.files[dirCount] = currentFile;
                dirCount++;
            } else {
                this.files[totalCount - (1 + fileCount)] = currentFile;
                fileCount++;
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
     * Getter for property message.
     * 
     * @return Value of property message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Setter for property message.
     * 
     * @param message
     *            New value of property message.
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Getter for property keywordSubst.
     * 
     * @return Value of property keywordSubst.
     */
    public KeywordSubstitutionOptions getKeywordSubst() {
        return keywordSubst;
    }

    /**
     * Setter for property keywordSubst.
     * 
     * @param keywordSubst
     *            New value of property keywordSubst.
     */
    public void setKeywordSubst(final KeywordSubstitutionOptions keywordSubst) {
        this.keywordSubst = keywordSubst;
    }

    /**
     * Add requests for a particular file or directory to be added.
     */
    protected void addRequests(final File file) throws IOException, CommandException {
        if (file.isDirectory()) {
            addRequestsForDirectory(file, false);
        } else {
            addRequestsForFile(file);
        }
    }

    /**
     * Add requests for a particular directory.
     * 
     * @param directory
     *            the directory to add
     * @param adding
     *            - for the directory to be added, set to true. used internally
     *            to recurse Directory requests.
     * @throws IOException
     *             if an error occurs
     */
    private void addRequestsForDirectory(final File directory, final boolean recursion) throws IOException {

        final File parentDirectory = directory.getParentFile();
        final String dir = recursion ? getRelativeToLocalPathInUnixStyle(directory)
                        : getRelativeToLocalPathInUnixStyle(parentDirectory);

        String partPath;
        if (dir.equals(".")) { // NOI18N
            partPath = directory.getName();
        } else {
            // trim the leading slash from the pathname we end up with
            // (e.g. we end up with something like \banana\foo
            // and this gives us banana\foo). Also replace backslashes with
            // forward slashes. The standard CVS server doesn't like
            // backslashes very much.
            partPath = dir + "/" + directory.getName(); // NOI18N
            // recursively scroll back to the localPath..
            addRequestsForDirectory(parentDirectory, true);
        }

        if (recursion) {
            partPath = dir;
        }

        // Note that the repository file for the directory being added has not
        // been created yet, so we are forced to read the repository for
        // the parent directory and build the appropriate entry by tagging
        // on the directory name (called partPath here)
        String repository;
        String tag;

        if (recursion) {
            repository = clientServices.getRepositoryForDirectory(directory.getAbsolutePath());
            tag = clientServices.getStickyTagForDirectory(directory);
        } else {
            repository = clientServices.getRepositoryForDirectory(parentDirectory.getAbsolutePath());
            if (repository.endsWith(".")) {
                repository = repository.substring(0, repository.length() - 1) + directory.getName();
            } else {
                repository = repository + "/" + directory.getName(); // NOI18N
            }
            tag = clientServices.getStickyTagForDirectory(parentDirectory);
        }

        requests.add(new DirectoryRequest(partPath, repository));
        if (tag != null) {
            requests.add(new StickyRequest(tag));
        }

        if (!recursion) {
            argumentRequests.add(new ArgumentRequest(partPath));
            /*
             * newDirList.put(partPath, repository);
             */
            newDirList.add(new Paths(partPath, repository));
        }
        // MK argument after Dir request.. also with the rel path from the
        // current working dir
    }

    /**
     * Add requests for a particular file.
     */
    protected void addRequestsForFile(final File file) throws IOException, CommandException {
        final File directory = file.getParentFile();
        final String dir = getRelativeToLocalPathInUnixStyle(directory);

        final String repository = clientServices.getRepositoryForDirectory(directory.getAbsolutePath());
        requests.add(new DirectoryRequest(dir, repository));
        final String tag = clientServices.getStickyTagForDirectory(directory);
        if (tag != null) {
            requests.add(new StickyRequest(tag));
        }

        final Entry entry = clientServices.getEntry(file);

        if (entry != null) {
            requests.add(new EntryRequest(entry));
        } else {

            Map<StringPattern, KeywordSubstitutionOptions> directoryLevelWrapper = dir2WrapperMap.get(dir);
            if (directoryLevelWrapper == null) {

                // we have not parsed the cvs wrappers for this directory
                // read the wrappers for this directory

                final File wrapperFile = new File(directory, ".cvswrappers"); // NOI18N
                if (wrapperFile.exists()) {
                    directoryLevelWrapper = new HashMap<StringPattern, KeywordSubstitutionOptions>(5);
                    WrapperUtils.readWrappersFromFile(wrapperFile, directoryLevelWrapper);
                } else {
                    directoryLevelWrapper = EMPTYWRAPPER;
                }

                // store the wrapper map indexed by directory name
                dir2WrapperMap.put(dir, directoryLevelWrapper);
            }

            final boolean isBinary = isBinary(clientServices, file.getName(), directoryLevelWrapper);

            if (isBinary) {
                requests.add(new KoptRequest("-kb")); // NOI18N
            }
            requests.add(new IsModifiedRequest(file));
        }

        if (dir.equals(".")) { // NOI18N
            argumentRequests.add(new ArgumentRequest(file.getName(), true));
        } else {
            argumentRequests.add(new ArgumentRequest(dir + "/" + file.getName())); // NOI18N
        }
    }

    /**
     * Returns true, if the file for the specified filename should be treated as
     * a binary file.
     * 
     * The information comes from the wrapper map and the set
     * keywordsubstitution.
     */
    private boolean isBinary(final ClientServices client, final String filename,
                    final Map<StringPattern, KeywordSubstitutionOptions> directoryLevelWrappers)
                    throws CommandException {
        KeywordSubstitutionOptions keywordSubstitutionOptions = getKeywordSubst();

        if (keywordSubstitutionOptions == KeywordSubstitutionOptions.BINARY) {
            return true;
        }

        // The keyWordSubstitutions was set based on MIME-types by
        // CVSAdd which had no notion of cvswrappers. Therefore some
        // filetypes returned as text may actually be binary within CVS
        // We check for those files here

        boolean wrapperFound = false;

        if (wrapperMap == null) {
            // process the wrapper settings as we have not done it before.
            wrapperMap = WrapperUtils.mergeWrapperMap(client);
        }

        for (final StringPattern stringPattern : wrapperMap.keySet()) {
            final SimpleStringPattern pattern = (SimpleStringPattern) stringPattern;
            if (pattern.doesMatch(filename)) {
                keywordSubstitutionOptions = wrapperMap.get(pattern);
                wrapperFound = true;
                break;
            }
        }

        // if no wrappers are found to match the server and local settings, try
        // the wrappers for this local directory
        if (!wrapperFound && (directoryLevelWrappers != null) && (directoryLevelWrappers != EMPTYWRAPPER)) {
            for (final StringPattern stringPattern : directoryLevelWrappers.keySet()) {
                final SimpleStringPattern pattern = (SimpleStringPattern) stringPattern;
                if (pattern.doesMatch(filename)) {
                    keywordSubstitutionOptions = directoryLevelWrappers.get(pattern);
                    wrapperFound = true;
                    break;
                }
            }
        }

        return keywordSubstitutionOptions == KeywordSubstitutionOptions.BINARY;
    }

    /**
     * Execute a command.
     * 
     * @param client
     *            the client services object that provides any necessary
     *            services to this command, including the ability to actually
     *            process all the requests
     */
    @Override
    public void execute(final ClientServices client, final EventManager em) throws CommandException,
                    AuthenticationException {
        if ((files == null) || (files.length == 0)) {
            throw new CommandException("No files have been specified for " + // NOI18N
                            "adding.", CommandException.getLocalMessage("AddCommand.noFilesSpecified", null)); // NOI18N
        }

        client.ensureConnection();

        clientServices = client;
        setLocalDirectory(client.getLocalPath());

        final String directory = client.getLocalPath();
        final File cvsfolder = new File(directory, "CVS");
        if (!cvsfolder.isDirectory()) {
            // setFailed();
            final MessageEvent event = new MessageEvent(this,
                            "cvs [add aborted]: there is no version here; do 'cvs checkout' first", true);
            messageSent(event);
            em.fireCVSEvent(event);
            return;
        }
        /*
         * newDirList = new HashMap();
         */
        newDirList.clear();

        super.execute(client, em);

        requests = new LinkedList<Request>();

        if (client.isFirstCommand()) {
            requests.add(new RootRequest(client.getRepository()));
        }

        // sets the message argument -m .. one for all files being sent..
        String message = getMessage();
        if (message != null) {
            message = message.trim();
        }
        if ((message != null) && (message.length() > 0)) {
            addMessageRequest(message);
        }

        if ((getKeywordSubst() != null) && !getKeywordSubst().equals("")) { // NOI18N
            requests.add(new ArgumentRequest("-k" + getKeywordSubst())); // NOI18N
        }

        try {
            // current dir sent to server BEFORE and AFTER - kinda hack??
            for (final File file : files) {
                addRequests(file);
            }

            // now add the request that indicates the working directory for the
            // command
            requests.add(new DirectoryRequest(".", // NOI18N
                            client.getRepositoryForDirectory(getLocalDirectory())));

            requests.addAll(argumentRequests);
            argumentRequests.clear(); // MK sanity check.
            requests.add(CommandRequest.ADD);
            client.processRequests(requests);
        } catch (final CommandException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CommandException(ex, ex.getLocalizedMessage());
        } finally {
            requests.clear();
        }
    }

    private void addMessageRequest(final String message) {
        requests.add(new ArgumentRequest("-m")); // NOI18N
        final StringTokenizer token = new StringTokenizer(message, "\n", false); // NOI18N
        boolean first = true;
        while (token.hasMoreTokens()) {
            if (first) {
                requests.add(new ArgumentRequest(token.nextToken()));
                first = false;
            } else {
                requests.add(new ArgumentxRequest(token.nextToken()));
            }
        }
    }

    /**
     * This method returns how the command would look like when typed on the
     * command line. Each command is responsible for constructing this
     * information.
     * 
     * @returns <command's name> [<parameters>] files/dirs. Example: checkout -p
     *          CvsCommand.java
     */
    @Override
    public String getCVSCommand() {
        final StringBuffer toReturn = new StringBuffer("add "); // NOI18N
        toReturn.append(getCVSArguments());
        final File[] files = getFiles();
        if (files != null) {
            for (final File file : files) {
                toReturn.append(file.getName());
                toReturn.append(' ');
            }
        }
        return toReturn.toString();
    }

    /**
     * Method that is called while the command is being executed. Descendants
     * can override this method to return a Builder instance that will parse the
     * server's output and create data structures.
     */
    @Override
    public Builder createBuilder(final EventManager eventManager) {
        return new AddBuilder(eventManager, this);
    }

    /**
     * Takes the arguments and sets the command. To be mainly used for automatic
     * settings (like parsing the .cvsrc file)
     * 
     * @return true if the option (switch) was recognized and set
     */
    @Override
    public boolean setCVSCommand(final char opt, final String optArg) {
        if (opt == 'm') {
            setMessage(optArg);
        } else if (opt == 'k') {
            final KeywordSubstitutionOptions keywordSubst = KeywordSubstitutionOptions.findKeywordSubstOption(optArg);
            setKeywordSubst(keywordSubst);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Returns a string indicating the available options.
     */
    @Override
    public String getOptString() {
        return "m:k:"; // NOI18N
    }

    /**
     * Listens for output of the command. If new directory is added, executes
     * the createCvsFiles() method.
     */
    @Override
    public void messageSent(final MessageEvent e) {
        String str = e.getMessage();
        if (str.endsWith(DIR_ADDED)) {
            str = str.substring(DIRECTORY.length(), str.indexOf(DIR_ADDED)).trim();
            createCvsFiles(str);
        }
        super.messageSent(e);
    }

    /**
     * For new directory that was added to the repository, creates the admin
     * files in CVS subdir.
     */
    private void createCvsFiles(final String newDirInRepository) {
        String repository = newDirInRepository;
        String dirName = repository;
        if (dirName.lastIndexOf('/') >= 0) {
            dirName = dirName.substring(dirName.lastIndexOf('/') + 1, dirName.length());
        }

        if (newDirList.size() == 0) {
            System.err.println("JavaCVS: Bug in AddCommand|createCvsFiles"); // NOI18N
            System.err.println("         newDirInRepository = " + newDirInRepository); // NOI18N
            return;
        }

        Paths paths = null;
        for (final Iterator<Paths> i = newDirList.iterator(); i.hasNext();) {
            paths = i.next();
            if (paths.getRepositoryPath().equals(newDirInRepository)) {
                i.remove();
                break;
            }
        }

        final String local = paths.getPartPath();
        final String part = paths.getRepositoryPath();
        repository = paths.getRepositoryPath();

        String tempDirName = part;
        if (part.lastIndexOf('/') >= 0) {
            tempDirName = part.substring(part.lastIndexOf('/') + 1, part.length());
        }

        if (!tempDirName.equalsIgnoreCase(dirName)) {
            System.err.println("JavaCVS: Bug in AddCommand|createCvsFiles"); // NOI18N
            System.err.println("         newDirInRepository = " + newDirInRepository); // NOI18N
            System.err.println("         tempDirName = " + tempDirName); // NOI18N
            System.err.println("         dirName = " + dirName); // NOI18N
            return;
        }

        try {
            if (repository.startsWith(".")) { // NOI18N
                repository = repository.substring(1);
            }
            clientServices.updateAdminData(local, repository, null);
            createCvsTagFile(local, repository);
        } catch (final IOException ex) {
            System.err.println("TODO: couldn't create/update Cvs admin files"); // NOI18N
        }
        /*
         * Iterator it = newDirList.keySet().iterator(); while (it.hasNext()) {
         * String local = (String)it.next(); String part =
         * (String)newDirList.get(local); String tempDirName = part; if
         * (part.lastIndexOf('/') >= 0) { tempDirName =
         * part.substring(part.lastIndexOf('/') + 1, part.length()); }
         * 
         * if (tempDirName.equalsIgnoreCase(dirName)) { try {
         * clientServices.updateAdminData(local, repository, null);
         * createCvsTagFile(local, repository); it.remove(); // hack.. in case 2
         * dirs being added have the same name?? break; } catch (IOException
         * exc) {
         * System.out.println("TODO: couldn't create/update Cvs admin files"); }
         * } }
         */
    }

    private void createCvsTagFile(final String local, final String repository) throws IOException {
        final File current = new File(getLocalDirectory(), local);
        final File parent = current.getParentFile();
        final String tag = clientServices.getStickyTagForDirectory(parent);
        if (tag != null) {
            final File tagFile = new File(current, "CVS/Tag"); // NOI18N
            tagFile.createNewFile();
            final PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(tagFile)));
            w.println(tag);
            w.close();
        }
    }

    /**
     * resets all switches in the command. After calling this method, the
     * command should have no switches defined and should behave defaultly.
     */
    @Override
    public void resetCVSCommand() {
        setMessage(null);
        setKeywordSubst(null);
    }

    /**
     * Returns the arguments of the command in the command-line style. Similar
     * to getCVSCommand() however without the files and command's name
     */
    @Override
    public String getCVSArguments() {
        final StringBuffer toReturn = new StringBuffer();
        if (getMessage() != null) {
            toReturn.append("-m \""); // NOI18N
            toReturn.append(getMessage());
            toReturn.append("\" "); // NOI18N
        }
        if (getKeywordSubst() != null) {
            toReturn.append("-k"); // NOI18N
            toReturn.append(getKeywordSubst().toString());
            toReturn.append(" "); // NOI18N
        }
        return toReturn.toString();
    }

    private static class Paths {
        private final String partPath;
        private final String repositoryPath;

        public Paths(final String partPath, final String repositoryPath) {
            this.partPath = partPath;
            this.repositoryPath = repositoryPath;
        }

        public String getPartPath() {
            return partPath;
        }

        public String getRepositoryPath() {
            return repositoryPath;
        }
    }
}
