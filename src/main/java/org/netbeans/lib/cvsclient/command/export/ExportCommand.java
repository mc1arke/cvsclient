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
package org.netbeans.lib.cvsclient.command.export;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.netbeans.lib.cvsclient.ClientServices;
import org.netbeans.lib.cvsclient.command.Builder;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.KeywordSubstitutionOptions;
import org.netbeans.lib.cvsclient.command.RepositoryCommand;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.MessageEvent;
import org.netbeans.lib.cvsclient.request.ArgumentRequest;
import org.netbeans.lib.cvsclient.request.CommandRequest;
import org.netbeans.lib.cvsclient.request.DirectoryRequest;

/**
 * The export command exports the projects (modules in the repository) to the
 * local directory structure.
 * 
 * @author MIlos Kleint
 */
public class ExportCommand extends RepositoryCommand {

    /**
     * 
     */
    private static final long serialVersionUID = 5620628445557586047L;

    private final String UPDATING = ": Updating "; // NOI18N

    /**
     * A store of potentially empty directories. When a directory has a file in
     * it, it is removed from this set. This set allows the prune option to be
     * implemented.
     */
    private final Set<File> emptyDirectories = new HashSet<File>();
    private boolean pruneDirectories;
    private KeywordSubstitutionOptions keywordSubstitutionOptions;

    /** Holds value of property exportByDate. */
    private String exportByDate;

    /** Holds value of property exportByRevision. */
    private String exportByRevision;

    /** Holds value of property exportDirectory. */
    private String exportDirectory;

    /** Holds value of property useHeadIfNotFound. */
    private boolean useHeadIfNotFound;

    /** Don't shorten module paths if -d specified. */
    private boolean notShortenPaths;

    /** Do not run module program (if any). */
    private boolean notRunModuleProgram;

    public ExportCommand() {
        resetCVSCommand();
    }

    /**
     * Returns the keyword substitution option.
     */
    public KeywordSubstitutionOptions getKeywordSubstitutionOptions() {
        return keywordSubstitutionOptions;
    }

    /**
     * Sets the keywords substitution option.
     */
    public void setKeywordSubstitutionOptions(final KeywordSubstitutionOptions keywordSubstitutionOptions) {
        this.keywordSubstitutionOptions = keywordSubstitutionOptions;
    }

    /**
     * Set whether to prune directories. This is the -P option in the
     * command-line CVS.
     */
    public void setPruneDirectories(final boolean pruneDirectories) {
        this.pruneDirectories = pruneDirectories;
    }

    /**
     * Get whether to prune directories.
     * 
     * @return true if directories should be removed if they contain no files,
     *         false otherwise.
     */
    public boolean isPruneDirectories() {
        return pruneDirectories;
    }

    /**
     * Execute this command
     * 
     * @param client
     *            the client services object that provides any necessary
     *            services to this command, including the ability to actually
     *            process all the requests
     */
    @Override
    protected void postExpansionExecute(final ClientServices client, final EventManager em) throws CommandException,
                    AuthenticationException {
        //
        // moved modules code to the end of the other arguments --GAR
        //
        final int FIRST_INDEX = 0;
        final int SECOND_INDEX = 1;
        if (!isRecursive()) {
            requests.add(FIRST_INDEX, new ArgumentRequest("-l")); // NOI18N
        }
        if (useHeadIfNotFound) {
            requests.add(FIRST_INDEX, new ArgumentRequest("-f")); // NOI18N
        }
        if ((exportDirectory != null) && (!exportDirectory.equals(""))) {
            requests.add(FIRST_INDEX, new ArgumentRequest("-d")); // NOI18N
            requests.add(SECOND_INDEX, new ArgumentRequest(getExportDirectory()));
        }
        if ((exportByDate != null) && (exportByDate.length() > 0)) {
            requests.add(FIRST_INDEX, new ArgumentRequest("-D")); // NOI18N
            requests.add(SECOND_INDEX, new ArgumentRequest(getExportByDate()));
        }
        if ((exportByRevision != null) && (exportByRevision.length() > 0)) {
            requests.add(FIRST_INDEX, new ArgumentRequest("-r")); // NOI18N
            requests.add(SECOND_INDEX, new ArgumentRequest(getExportByRevision()));
        }
        if (notShortenPaths) {
            requests.add(FIRST_INDEX, new ArgumentRequest("-N")); // NOI18N
        }
        if (notRunModuleProgram) {
            requests.add(FIRST_INDEX, new ArgumentRequest("-n")); // NOI18N
        }
        if (getKeywordSubstitutionOptions() != null) {
            requests.add(new ArgumentRequest("-k" + getKeywordSubstitutionOptions())); // NOI18N
        }

        addArgumentRequests();

        requests.add(new DirectoryRequest(".", client.getRepository())); // NOI18N
        requests.add(CommandRequest.EXPORT);
        try {
            client.processRequests(requests);
            if (pruneDirectories) {
                pruneEmptyDirectories();
            }
            requests.clear();

        } catch (final CommandException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CommandException(ex, ex.getLocalizedMessage());
        } finally {
            removeAllCVSAdminFiles();
        }
    }

    private void removeAllCVSAdminFiles() {
        File rootDirect = null;
        if (getExportDirectory() != null) {
            rootDirect = new File(getLocalDirectory(), getExportDirectory());
            deleteCVSSubDirs(rootDirect);
        } else {
            rootDirect = new File(getLocalDirectory());
            final Iterator<String> mods = expandedModules.iterator();
            while (mods.hasNext()) {
                final String mod = mods.next().toString();
                final File modRoot = new File(rootDirect.getAbsolutePath(), mod);
                deleteCVSSubDirs(modRoot);
            }
        }
    }

    private void deleteCVSSubDirs(final File root) {
        if (root.isDirectory()) {
            final File[] subDirs = root.listFiles();
            if (subDirs == null) {
                return;
            }

            for (final File subDir : subDirs) {
                if (subDir.isDirectory()) {
                    if (subDir.getName().equalsIgnoreCase("CVS")) { // NOI18N
                        final File[] adminFiles = subDir.listFiles();
                        for (final File adminFile : adminFiles) {
                            adminFile.delete();
                        }
                        subDir.delete();
                    } else {
                        deleteCVSSubDirs(subDir);
                    }
                }
            }
        }
    }

    @Override
    public String getCVSCommand() {
        final StringBuffer toReturn = new StringBuffer("export "); // NOI18N
        toReturn.append(getCVSArguments());
        if ((modules != null) && (modules.size() > 0)) {
            for (final String string : modules) {
                final String module = string;
                toReturn.append(module);
                toReturn.append(' ');
            }
        } else {
            final String localizedMsg = CommandException.getLocalMessage("ExportCommand.moduleEmpty.text"); // NOI18N
            toReturn.append(" "); // NOI18N
            toReturn.append(localizedMsg);
        }
        return toReturn.toString();
    }

    @Override
    public String getCVSArguments() {
        final StringBuffer toReturn = new StringBuffer(""); // NOI18N
        if (!isRecursive()) {
            toReturn.append("-l "); // NOI18N
        }
        if (isUseHeadIfNotFound()) {
            toReturn.append("-f "); // NOI18N
        }
        if (getExportByDate() != null) {
            toReturn.append("-D "); // NOI18N
            toReturn.append(getExportByDate());
            toReturn.append(" "); // NOI18N
        }
        if (getExportByRevision() != null) {
            toReturn.append("-r "); // NOI18N
            toReturn.append(getExportByRevision());
            toReturn.append(" "); // NOI18N
        }
        if (isPruneDirectories()) {
            toReturn.append("-P "); // NOI18N
        }
        if (isNotShortenPaths()) {
            toReturn.append("-N "); // NOI18N
        }
        if (isNotRunModuleProgram()) {
            toReturn.append("-n "); // NOI18N
        }
        if (getExportDirectory() != null) {
            toReturn.append("-d "); // NOI18N
            toReturn.append(getExportDirectory());
            toReturn.append(" "); // NOI18N
        }
        if (getKeywordSubstitutionOptions() != null) {
            toReturn.append("-k"); // NOI18N
            toReturn.append(getKeywordSubstitutionOptions().toString());
            toReturn.append(" "); // NOI18N
        }
        return toReturn.toString();
    }

    @Override
    public boolean setCVSCommand(final char opt, final String optArg) {
        if (opt == 'k') {
            setKeywordSubstitutionOptions(KeywordSubstitutionOptions.findKeywordSubstOption(optArg));
        } else if (opt == 'r') {
            setExportByRevision(optArg);
        } else if (opt == 'f') {
            setUseHeadIfNotFound(true);
        } else if (opt == 'D') {
            setExportByDate(optArg);
        } else if (opt == 'd') {
            setExportDirectory(optArg);
        } else if (opt == 'P') {
            setPruneDirectories(true);
        } else if (opt == 'N') {
            setNotShortenPaths(true);
        } else if (opt == 'n') {
            setNotRunModuleProgram(true);
        } else if (opt == 'l') {
            setRecursive(false);
        } else if (opt == 'R') {
            setRecursive(true);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void resetCVSCommand() {
        setModules(null);
        setKeywordSubstitutionOptions(null);
        setPruneDirectories(false);
        setRecursive(true);
        setExportByDate(null);
        setExportByRevision(null);
        setExportDirectory(null);
        setUseHeadIfNotFound(false);
        setNotShortenPaths(false);
        setNotRunModuleProgram(false);
    }

    @Override
    public String getOptString() {
        return "k:r:D:NPlRnd:f"; // NOI18N
    }

    /**
     * Creates the ExportBuilder.
     */
    @Override
    public Builder createBuilder(final EventManager eventManager) {
        return new ExportBuilder(eventManager, this);
    }

    /**
     * Called when the server wants to send a message to be displayed to the
     * user. The message is only for information purposes and clients can choose
     * to ignore these messages if they wish.
     * 
     * @param e
     *            the event
     */
    @Override
    public void messageSent(final MessageEvent e) {
        super.messageSent(e);
        // we use this event to determine which directories need to be checked
        // for updating
        if (pruneDirectories && (e.getMessage().indexOf(UPDATING) > 0)) {
            final File file = new File(getLocalDirectory(), e.getMessage().substring(
                            e.getMessage().indexOf(UPDATING) + UPDATING.length()));
            emptyDirectories.add(file);
        }
    }

    /**
     * Prunes a directory, recursively pruning its subdirectories
     * 
     * @param directory
     *            the directory to prune
     */
    private boolean pruneEmptyDirectory(final File directory) throws IOException {
        boolean empty = true;

        final File[] contents = directory.listFiles();

        // should never be null, but just in case...
        if (contents != null) {
            for (int i = 0; i < contents.length; i++) {
                if (contents[i].isFile()) {
                    empty = false;
                } else {
                    if (!contents[i].getName().equals("CVS")) { // NOI18N
                        empty = pruneEmptyDirectory(contents[i]);
                    }
                }

                if (!empty) {
                    break;
                }
            }

            if (empty) {
                // check this is a CVS directory and not some directory the user
                // has stupidly called CVS...
                final File entriesFile = new File(directory, "CVS/Entries"); // NOI18N
                if (entriesFile.exists()) {
                    final File adminDir = new File(directory, "CVS"); // NOI18N
                    final File[] adminFiles = adminDir.listFiles();
                    for (final File adminFile : adminFiles) {
                        adminFile.delete();
                    }
                    adminDir.delete();
                    directory.delete();
                }
            }
        }

        return empty;
    }

    /**
     * Remove any directories that don't contain any files
     */
    private void pruneEmptyDirectories() throws IOException {
        final Iterator<File> it = emptyDirectories.iterator();
        while (it.hasNext()) {
            final File dir = it.next();
            // we might have deleted it already (due to recursive delete)
            // so we need to check existence
            if (dir.exists()) {
                pruneEmptyDirectory(dir);
            }
        }
        emptyDirectories.clear();
    }

    /**
     * Getter for property exportByDate.
     * 
     * @return Value of property exportByDate.
     */
    public String getExportByDate() {
        return exportByDate;
    }

    /**
     * Setter for property exportByDate.
     * 
     * @param exportByDate
     *            New value of property exportByDate.
     */
    public void setExportByDate(final String exportByDate) {
        this.exportByDate = exportByDate;
    }

    /**
     * Getter for property exportByRevision.
     * 
     * @return Value of property exportByRevision.
     */
    public String getExportByRevision() {
        return exportByRevision;
    }

    /**
     * Setter for property exportByRevision.
     * 
     * @param exportByRevision
     *            New value of property exportByRevision.
     */
    public void setExportByRevision(final String exportByRevision) {
        this.exportByRevision = exportByRevision;
    }

    /**
     * Getter for property exportDirectory.
     * 
     * @return Value of property exportDirectory.
     */
    public String getExportDirectory() {
        return exportDirectory;
    }

    /**
     * Setter for property exportDirectory.
     * 
     * @param exportDirectory
     *            New value of property exportDirectory.
     */
    public void setExportDirectory(final String exportDirectory) {
        this.exportDirectory = exportDirectory;
    }

    /**
     * Getter for property useHeadIfNotFound.
     * 
     * @return Value of property useHeadIfNotFound.
     */
    public boolean isUseHeadIfNotFound() {
        return useHeadIfNotFound;
    }

    /**
     * Setter for property useHeadIfNotFound.
     * 
     * @param useHeadIfNotFound
     *            New value of property useHeadIfNotFound.
     */
    public void setUseHeadIfNotFound(final boolean useHeadIfNotFound) {
        this.useHeadIfNotFound = useHeadIfNotFound;
    }

    /**
     * Getter for property notShortenPaths.
     * 
     * @return Value of property notShortenPaths.
     */
    public boolean isNotShortenPaths() {
        return notShortenPaths;
    }

    /**
     * Setter for property notShortenPaths.
     * 
     * @param notShortenPaths
     *            New value of property notShortenPaths.
     */
    public void setNotShortenPaths(final boolean notShortenPaths) {
        this.notShortenPaths = notShortenPaths;
    }

    /**
     * Getter for property notRunModuleProgram.
     * 
     * @return Value of property notRunModuleProgram.
     */
    public boolean isNotRunModuleProgram() {
        return notRunModuleProgram;
    }

    /**
     * Setter for property notRunModuleProgram.
     * 
     * @param notRunModuleProgram
     *            New value of property notRunModuleProgram.
     */
    public void setNotRunModuleProgram(final boolean notRunModuleProgram) {
        this.notRunModuleProgram = notRunModuleProgram;
    }

}
