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
package org.netbeans.lib.cvsclient.command.tag;

import java.io.EOFException;

import org.netbeans.lib.cvsclient.ClientServices;
import org.netbeans.lib.cvsclient.command.Builder;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.RepositoryCommand;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.TerminationEvent;
import org.netbeans.lib.cvsclient.request.ArgumentRequest;
import org.netbeans.lib.cvsclient.request.CommandRequest;

/**
 * The rtag command adds or deletes a tag to the specified files/directories in
 * the repository.
 * 
 * @author Martin Entlicher
 */
public class RtagCommand extends RepositoryCommand {
    /**
     * 
     */
    private static final long serialVersionUID = -6502970922103442790L;

    private boolean clearFromRemoved;

    private boolean deleteTag;

    private boolean makeBranchTag;

    private boolean overrideExistingTag;

    private boolean matchHeadIfRevisionNotFound;

    private boolean noExecTagProgram;

    private String tag;

    private String tagByDate;

    private String tagByRevision;

    /**
     * Construct a new tag command.
     */
    public RtagCommand() {
    }

    /**
     * Creates the TagBuilder.
     * 
     * @param eventManager
     *            the event manager used to received cvs events
     */
    @Override
    public Builder createBuilder(final EventManager eventManager) {
        return new TagBuilder(eventManager, getLocalDirectory());
    }

    /**
     * Returns true if the tag from removed files is cleared.
     */
    public boolean isClearFromRemoved() {
        return clearFromRemoved;
    }

    /**
     * Clear tag from removed files
     */
    public void setClearFromRemoved(final boolean clearFromRemoved) {
        this.clearFromRemoved = clearFromRemoved;
    }

    /**
     * Returns true if the tag should be deleted (otherwise added).
     */
    public boolean isDeleteTag() {
        return deleteTag;
    }

    /**
     * Sets whether the tag should be deleted (true) or added (false).
     */
    public void setDeleteTag(final boolean deleteTag) {
        this.deleteTag = deleteTag;
    }

    /**
     * Returns true if the tag should be a branch tag.
     */
    public boolean isMakeBranchTag() {
        return makeBranchTag;
    }

    /**
     * Sets whether the tag should be a branch tag.
     */
    public void setMakeBranchTag(final boolean makeBranchTag) {
        this.makeBranchTag = makeBranchTag;
    }

    /**
     * Returns true to indicate that existing tag will be overridden.
     */
    public boolean isOverrideExistingTag() {
        return overrideExistingTag;
    }

    /**
     * Sets whether existing tags should be overridden.
     */
    public void setOverrideExistingTag(final boolean overrideExistingTag) {
        this.overrideExistingTag = overrideExistingTag;
    }

    public boolean isMatchHeadIfRevisionNotFound() {
        return matchHeadIfRevisionNotFound;
    }

    public void setMatchHeadIfRevisionNotFound(final boolean matchHeadIfRevisionNotFound) {
        this.matchHeadIfRevisionNotFound = matchHeadIfRevisionNotFound;
    }

    public boolean isNoExecTagProgram() {
        return noExecTagProgram;
    }

    public void setNoExecTagProgram(final boolean noExecTagProgram) {
        this.noExecTagProgram = noExecTagProgram;
    }

    /**
     * Returns the tag that should be added or deleted.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the tag that should be added or deleted.
     */
    public void setTag(final String tag) {
        this.tag = tag;
    }

    /**
     * Returns the latest date of a revision to be tagged.
     * 
     * @return date value. the latest Revision not later ten date is tagged.
     */
    public String getTagByDate() {
        return tagByDate;
    }

    /**
     * Sets the latest date of a revision to be tagged.
     * 
     * @param tagDate
     *            New value of property tagDate.
     */
    public void setTagByDate(final String tagDate) {
        tagByDate = tagDate;
    }

    /**
     * Sets the latest date of a revision to be tagged. Can be both a number and
     * a tag.
     * 
     * @return Value of property tagRevision.
     */
    public String getTagByRevision() {
        return tagByRevision;
    }

    /**
     * Sets the latest date of a revision to be tagged. Can be both a number and
     * a tag.
     * 
     * @param tagRevision
     *            New value of property tagRevision.
     */
    public void setTagByRevision(final String tagRevision) {
        tagByRevision = tagRevision;
    }

    /**
     * Execute the command.
     * 
     * @param client
     *            the client services object that provides any necessary
     *            services to this command, including the ability to actually
     *            process all the requests.
     */
    @Override
    protected void postExpansionExecute(final ClientServices client, final EventManager eventManager)
                    throws CommandException, AuthenticationException {
        client.ensureConnection();

        try {
            if (clearFromRemoved) {
                requests.add(new ArgumentRequest("-a")); // NOI18N
            }

            if (overrideExistingTag) {
                requests.add(new ArgumentRequest("-F")); // NOI18N
            }

            if (matchHeadIfRevisionNotFound) {
                requests.add(new ArgumentRequest("-f")); // NOI18N
            }

            if (makeBranchTag) {
                requests.add(new ArgumentRequest("-b")); // NOI18N
            }

            if (deleteTag) {
                requests.add(new ArgumentRequest("-d")); // NOI18N
            }

            if (noExecTagProgram) {
                requests.add(new ArgumentRequest("-n ")); // NOI18N
            }

            if ((tagByDate != null) && (tagByDate.length() > 0)) {
                requests.add(new ArgumentRequest("-D")); // NOI18N
                requests.add(new ArgumentRequest(getTagByDate()));
            }
            if ((tagByRevision != null) && (tagByRevision.length() > 0)) {
                requests.add(new ArgumentRequest("-r")); // NOI18N
                requests.add(new ArgumentRequest(getTagByRevision()));
            }

            requests.add(new ArgumentRequest(getTag()));

            // addRequestForWorkingDirectory(client);
            addArgumentRequests();
            addRequest(CommandRequest.RTAG);

            client.processRequests(requests);
        } catch (final CommandException ex) {
            throw ex;
        } catch (final EOFException ex) {
            throw new CommandException(ex, CommandException.getLocalMessage("CommandException.EndOfFile", null)); // NOI18N
        } catch (final Exception ex) {
            throw new CommandException(ex, ex.getLocalizedMessage());
        } finally {
            requests.clear();
        }
    }

    /**
     * Called when server responses with "ok" or "error", (when the command
     * finishes).
     */
    @Override
    public void commandTerminated(final TerminationEvent e) {
        if (builder != null) {
            builder.outputDone();
        }
    }

    /**
     * This method returns how the tag command would looklike when typed on the
     * command line.
     */
    @Override
    public String getCVSCommand() {
        final StringBuffer toReturn = new StringBuffer("rtag "); // NOI18N
        toReturn.append(getCVSArguments());
        if (getTag() != null) {
            toReturn.append(getTag());
            toReturn.append(" "); // NOI18N
        }
        appendModuleArguments(toReturn);
        return toReturn.toString();
    }

    /**
     * Takes the arguments and sets the command. To be mainly used for automatic
     * settings (like parsing the .cvsrc file)
     * 
     * @return true if the option (switch) was recognized and set
     */
    @Override
    public boolean setCVSCommand(final char opt, final String optArg) {
        if (opt == 'R') {
            setRecursive(true);
        } else if (opt == 'l') {
            setRecursive(false);
        } else if (opt == 'a') {
            setClearFromRemoved(true);
        } else if (opt == 'd') {
            setDeleteTag(true);
        } else if (opt == 'F') {
            setOverrideExistingTag(true);
        } else if (opt == 'f') {
            setMatchHeadIfRevisionNotFound(true);
        } else if (opt == 'b') {
            setMakeBranchTag(true);
        } else if (opt == 'n') {
            setNoExecTagProgram(true);
        } else if (opt == 'D') {
            setTagByDate(optArg.trim());
        } else if (opt == 'r') {
            setTagByRevision(optArg.trim());
        } else {
            return false;
        }
        return true;
    }

    /**
     * String returned by this method defines which options are available for
     * this command.
     */
    @Override
    public String getOptString() {
        return "RlaFfbdnD:r:"; // NOI18N
    }

    /**
     * Resets all switches in the command. After calling this method, the
     * command should have no switches defined and should behave defaultly.
     */
    @Override
    public void resetCVSCommand() {
        setRecursive(true);
        setClearFromRemoved(false);
        setDeleteTag(false);
        setMakeBranchTag(false);
        setOverrideExistingTag(false);
        setMatchHeadIfRevisionNotFound(false);
        setNoExecTagProgram(false);
    }

    /**
     * Returns the arguments of the command in the command-line style. Similar
     * to getCVSCommand() however without the files and command's name
     */
    @Override
    public String getCVSArguments() {
        final StringBuffer toReturn = new StringBuffer();
        if (!isRecursive()) {
            toReturn.append("-l "); // NOI18N
        }
        if (isClearFromRemoved()) {
            toReturn.append("-a "); // NOI18N
        }
        if (isOverrideExistingTag()) {
            toReturn.append("-F "); // NOI18N
        }
        if (isMatchHeadIfRevisionNotFound()) {
            toReturn.append("-f ");
        }
        if (isMakeBranchTag()) {
            toReturn.append("-b "); // NOI18N
        }
        if (isDeleteTag()) {
            toReturn.append("-d "); // NOI18N
        }
        if (isNoExecTagProgram()) {
            toReturn.append("-n "); // NOI18N
        }
        if ((getTagByRevision() != null) && (getTagByRevision().length() > 0)) {
            toReturn.append("-r "); // NOI18N
            toReturn.append(getTagByRevision());
            toReturn.append(" "); // NOI18N
        }
        if ((getTagByDate() != null) && (getTagByDate().length() > 0)) {
            toReturn.append("-D "); // NOI18N
            toReturn.append(getTagByDate());
            toReturn.append(" "); // NOI18N
        }
        return toReturn.toString();
    }
}
