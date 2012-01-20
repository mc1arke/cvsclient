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
package org.netbeans.lib.cvsclient.command.history;

import java.util.LinkedList;
import java.util.List;

import org.netbeans.lib.cvsclient.ClientServices;
import org.netbeans.lib.cvsclient.command.Builder;
import org.netbeans.lib.cvsclient.command.Command;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.TerminationEvent;
import org.netbeans.lib.cvsclient.request.ArgumentRequest;
import org.netbeans.lib.cvsclient.request.CommandRequest;
import org.netbeans.lib.cvsclient.request.Request;
import org.netbeans.lib.cvsclient.request.RootRequest;
import org.netbeans.lib.cvsclient.request.UseUnchangedRequest;

/**
 * The history command provides information history of activities in repository.
 * 
 * @author Milos Kleint
 */
public class HistoryCommand extends Command {
    /**
     * 
     */
    private static final long serialVersionUID = 3606230950833982897L;

    /**
     * The requests that are sent and processed.
     */
    private final List<Request> requests = new LinkedList<Request>();

    /** Holds value of property forAllUsers. */
    private boolean forAllUsers;

    /** Holds value of property goBackToRecord. */
    private String showBackToRecordContaining;

    /** Holds value of property reportCommits. */
    private boolean reportCommits;

    /** Holds value of property sinceDate. */
    private String sinceDate;

    /** Holds value of property reportEverything. */
    private boolean reportEverything;

    /** Holds value of property lastEventOfProject. */
    private boolean lastEventOfProject;

    /** Holds value of property reportCheckout. */
    private boolean reportCheckouts;

    /** Holds value of property sinceRevision. */
    private String sinceRevision;

    /** Holds value of property reportTags. */
    private boolean reportTags;

    /** Holds value of property sinceTag. */
    private String sinceTag;

    /** Holds value of property forWorkingDirectory. */
    private boolean forWorkingDirectory;

    /** Holds value of property reportEventType. */
    private String reportEventType;

    /** Holds value of property timeZone. */
    private String timeZone;

    /** Holds value of property lastEventForFile. */
    private String[] lastEventForFile;

    /** Holds value of property reportOnModule. */
    private String[] reportOnModule;

    /** Holds value of property reportLastEventForModule. */
    private String[] reportLastEventForModule;

    /** Holds value of property forUsers. */
    private String[] forUsers;

    /**
     * Construct a new history command
     */
    public HistoryCommand() {
    }

    /**
     * Create a builder for this command.
     * 
     * @param eventMan
     *            the event manager used to receive events.
     */
    public Builder createBuilder(final EventManager eventMan) {
        return null;
    }

    /**
     * Execute a command
     * 
     * @param client
     *            the client services object that provides any necessary
     *            services to this command, including the ability to actually
     *            process all the requests.
     */
    @Override
    public void execute(final ClientServices client, final EventManager em) throws CommandException,
                    AuthenticationException {

        client.ensureConnection();

        requests.clear();

        super.execute(client, em);

        try {
            if (client.isFirstCommand()) {
                requests.add(new RootRequest(client.getRepository()));
                requests.add(new UseUnchangedRequest());
            }

            addBooleanArgument(requests, isForAllUsers(), "-a"); // NOI18N
            addBooleanArgument(requests, isForWorkingDirectory(), "-w"); // NOI18N
            addBooleanArgument(requests, isLastEventOfProject(), "-l"); // NOI18N
            addBooleanArgument(requests, isReportCheckouts(), "-o"); // NOI18N
            addBooleanArgument(requests, isReportCommits(), "-c"); // NOI18N
            addBooleanArgument(requests, isReportEverything(), "-e"); // NOI18N
            addBooleanArgument(requests, isReportTags(), "-T"); // NOI18N
            addStringArgument(requests, getReportEventType(), "-x"); // NOI18N
            addStringArgument(requests, getShowBackToRecordContaining(), "-b"); // NOI18N
            addStringArgument(requests, getSinceDate(), "-D"); // NOI18N
            addStringArgument(requests, getSinceRevision(), "-r"); // NOI18N
            addStringArgument(requests, getSinceTag(), "-t"); // NOI18N
            addStringArrayArgument(requests, getForUsers(), "-u"); // NOI18N
            addStringArrayArgument(requests, getReportLastEventForModule(), "-n"); // NOI18N
            addStringArrayArgument(requests, getReportOnModule(), "-m"); // NOI18N
            addStringArrayArgument(requests, getLastEventForFile(), "-f"); // NOI18N
            if (!isReportCheckouts() && !isReportCommits() && !isReportTags() && !isReportEverything()
                            && (getReportEventType() == null) && (getReportOnModule() == null)) {
                // this is the default switch if nothing else is specified.
                addBooleanArgument(requests, true, "-c"); // NOI18N
            }
            if (getTimeZone() != null) {
                addStringArgument(requests, getTimeZone(), "-z"); // NOI18N
            } else {
                addStringArgument(requests, "+0000", "-z"); // NOI18N
            }
            requests.add(CommandRequest.HISTORY);
            client.processRequests(requests);
        } catch (final CommandException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CommandException(ex, ex.getLocalizedMessage());
        } finally {
            requests.clear();
        }
    }

    private void addStringArgument(final List<Request> reqList, final String property, final String cvsSwitch) {
        if (property != null) {
            reqList.add(new ArgumentRequest(cvsSwitch));
            reqList.add(new ArgumentRequest(property));
        }
    }

    private void addStringArrayArgument(final List<Request> reqList, final String[] property, final String cvsSwitch) {
        if (property != null) {
            for (final String element : property) {
                reqList.add(new ArgumentRequest(cvsSwitch));
                reqList.add(new ArgumentRequest(element));
            }
        }
    }

    private void addBooleanArgument(final List<Request> reqList, final boolean property, final String cvsSwitch) {
        if (property == true) {
            reqList.add(new ArgumentRequest(cvsSwitch));
        }
    }

    /**
     * called when server responses with "ok" or "error", (when the command
     * finishes)
     */
    @Override
    public void commandTerminated(final TerminationEvent e) {
    }

    /**
     * This method returns how the command would looklike when typed on the
     * command line. Each command is responsible for constructing this
     * information.
     * 
     * @returns <command's name> [<parameters>] files/dirs. Example: checkout -p
     *          CvsCommand.java
     * 
     */
    @Override
    public String getCVSCommand() {
        final StringBuffer toReturn = new StringBuffer("history "); // NOI18N
        toReturn.append(getCVSArguments());
        return toReturn.toString();
    }

    /**
     * takes the arguments and sets the command. To be mainly used for automatic
     * settings (like parsing the .cvsrc file)
     * 
     * @return true if the option (switch) was recognized and set
     */
    @Override
    public boolean setCVSCommand(final char opt, final String optArg) {
        if (opt == 'a') {
            setForAllUsers(true);
        } else if (opt == 'b') {
            setShowBackToRecordContaining(optArg);
        } else if (opt == 'c') {
            setReportCommits(true);
        } else if (opt == 'D') {
            setSinceDate(optArg);
        } else if (opt == 'e') {
            setReportEverything(true);
        } else if (opt == 'l') {
            setLastEventOfProject(true);
        } else if (opt == 'o') {
            setReportCheckouts(true);
        } else if (opt == 'r') {
            setSinceRevision(optArg);
        } else if (opt == 'T') {
            setReportTags(true);
        } else if (opt == 't') {
            setSinceTag(optArg);
        } else if (opt == 'w') {
            setForWorkingDirectory(true);
        } else if (opt == 'x') {
            setReportEventType(optArg);
        } else if (opt == 'z') {
            setTimeZone(optArg);
        } else if (opt == 'f') {
            addLastEventForFile(optArg);
        } else if (opt == 'm') {
            addReportOnModule(optArg);
        } else if (opt == 'n') {
            addReportLastEventForModule(optArg);
        } else if (opt == 'u') {
            addForUsers(optArg);
        } else {
            return false;
        }
        return true;
    }

    /**
     * String returned by this method defines which options are available for
     * this particular command
     */
    @Override
    public String getOptString() {
        return "ab:cD:ef:lm:n:or:Tt:u:wx:z:"; // NOI18N
    }

    /**
     * resets all switches in the command. After calling this method, the
     * command should have no switches defined and should behave defaultly.
     */
    @Override
    public void resetCVSCommand() {
        setForAllUsers(false);
        setForUsers(null);
        setForWorkingDirectory(false);
        setLastEventForFile(null);
        setLastEventOfProject(false);
        setReportCheckouts(false);
        setReportCommits(false);
        setReportEventType(null);
        setReportEverything(false);
        setReportLastEventForModule(null);
        setReportOnModule(null);
        setReportTags(false);
        setShowBackToRecordContaining(null);
        setSinceDate(null);
        setSinceRevision(null);
        setSinceTag(null);
        setTimeZone(null);
    }

    /**
     * Returns the arguments of the command in the command-line style. Similar
     * to getCVSCommand() however without the files and command's name
     */
    @Override
    public String getCVSArguments() {
        final StringBuffer toReturn = new StringBuffer(""); // NOI18N
        if (isForAllUsers()) {
            toReturn.append("-a "); // NOI18N
        }
        if (isForWorkingDirectory()) {
            toReturn.append("-w "); // NOI18N
        }
        if (isLastEventOfProject()) {
            toReturn.append("-l "); // NOI18N
        }
        if (isReportCheckouts()) {
            toReturn.append("-o "); // NOI18N
        }
        if (isReportCommits()) {
            toReturn.append("-c "); // NOI18N
        }
        if (isReportEverything()) {
            toReturn.append("-e "); // NOI18N
        }
        if (isReportTags()) {
            toReturn.append("-T "); // NOI18N
        }
        if (getForUsers() != null) {
            appendArrayToSwitches(toReturn, getForUsers(), "-u "); // NOI18N
        }
        if (getLastEventForFile() != null) {
            appendArrayToSwitches(toReturn, getLastEventForFile(), "-f "); // NOI18N
        }
        if (getReportEventType() != null) {
            toReturn.append("-x "); // NOI18N
            toReturn.append(getReportEventType());
            toReturn.append(" "); // NOI18N
        }
        if (getReportLastEventForModule() != null) {
            appendArrayToSwitches(toReturn, getReportLastEventForModule(), "-n "); // NOI18N
        }
        if (getReportOnModule() != null) {
            appendArrayToSwitches(toReturn, getReportOnModule(), "-m "); // NOI18N
        }
        if (getShowBackToRecordContaining() != null) {
            toReturn.append("-b "); // NOI18N
            toReturn.append(getShowBackToRecordContaining());
            toReturn.append(" "); // NOI18N
        }
        if (getSinceDate() != null) {
            toReturn.append("-D "); // NOI18N
            toReturn.append(getSinceDate());
            toReturn.append(" "); // NOI18N
        }
        if (getSinceRevision() != null) {
            toReturn.append("-r "); // NOI18N
            toReturn.append(getSinceRevision());
            toReturn.append(" "); // NOI18N
        }
        if (getSinceTag() != null) {
            toReturn.append("-t "); // NOI18N
            toReturn.append(getSinceTag());
            toReturn.append(" "); // NOI18N
        }
        if (getTimeZone() != null) {
            toReturn.append("-z "); // NOI18N
            toReturn.append(getTimeZone());
            toReturn.append(" "); // NOI18N
        }
        return toReturn.toString();
    }

    private void appendArrayToSwitches(final StringBuffer buff, final String[] arr, final String cvsSwitch) {
        if (arr == null) {
            return;
        }

        for (final String element : arr) {
            buff.append(cvsSwitch);
            buff.append(element);
            buff.append(" "); // NOI18N
        }
    }

    /**
     * Getter for property forAllUsers. (cvs switch -a)
     * 
     * @return Value of property forAllUsers.
     */
    public boolean isForAllUsers() {
        return forAllUsers;
    }

    /**
     * Setter for property forAllUsers. (cvs switch -a)
     * 
     * @param forAllUsers
     *            New value of property forAllUsers.
     */
    public void setForAllUsers(final boolean forAllUsers) {
        this.forAllUsers = forAllUsers;
    }

    /**
     * Getter for property goBackToRecord. (cvs switch -b)
     * 
     * @return Value of property goBackToRecord.
     */
    public String getShowBackToRecordContaining() {
        return showBackToRecordContaining;
    }

    /**
     * Setter for property goBackToRecord. (cvs switch -b)
     * 
     * @param goBackToRecord
     *            New value of property goBackToRecord.
     */
    public void setShowBackToRecordContaining(final String goBackToRecord) {
        showBackToRecordContaining = goBackToRecord;
    }

    /**
     * Getter for property reportCommits. (cvs switch -c)
     * 
     * @return Value of property reportCommits.
     */
    public boolean isReportCommits() {
        return reportCommits;
    }

    /**
     * Setter for property reportCommits. (cvs switch -b)
     * 
     * @param reportCommits
     *            New value of property reportCommits.
     */
    public void setReportCommits(final boolean reportCommits) {
        this.reportCommits = reportCommits;
    }

    /**
     * Getter for property sinceDate. (cvs switch -D)
     * 
     * @return Value of property sinceDate.
     */
    public String getSinceDate() {
        return sinceDate;
    }

    /**
     * Setter for property sinceDate. (cvs switch -D)
     * 
     * @param sinceDate
     *            New value of property sinceDate.
     */
    public void setSinceDate(final String sinceDate) {
        this.sinceDate = sinceDate;
    }

    /**
     * Getter for property reportEverything. (cvs switch -e)
     * 
     * @return Value of property reportEverything.
     */
    public boolean isReportEverything() {
        return reportEverything;
    }

    /**
     * Setter for property reportEverything. (cvs switch -e)
     * 
     * @param reportEverything
     *            New value of property reportEverything.
     */
    public void setReportEverything(final boolean reportEverything) {
        this.reportEverything = reportEverything;
    }

    /**
     * Getter for property lastEventOfProject. (cvs switch -l)
     * 
     * @return Value of property lastEventOfProject.
     */
    public boolean isLastEventOfProject() {
        return lastEventOfProject;
    }

    /**
     * Setter for property lastEventOfProject. (cvs switch -l)
     * 
     * @param lastEventOfProject
     *            New value of property lastEventOfProject.
     */
    public void setLastEventOfProject(final boolean lastEventOfProject) {
        this.lastEventOfProject = lastEventOfProject;
    }

    /**
     * Getter for property reportCheckout. (cvs switch -o)
     * 
     * @return Value of property reportCheckout.
     */
    public boolean isReportCheckouts() {
        return reportCheckouts;
    }

    /**
     * Setter for property reportCheckout. (cvs switch -o)
     * 
     * @param reportCheckout
     *            New value of property reportCheckout.
     */
    public void setReportCheckouts(final boolean reportCheckout) {
        reportCheckouts = reportCheckout;
    }

    /**
     * Getter for property sinceRevision. (cvs switch -r)
     * 
     * @return Value of property sinceRevision.
     */
    public String getSinceRevision() {
        return sinceRevision;
    }

    /**
     * Setter for property sinceRevision. (cvs switch -r)
     * 
     * @param sinceRevision
     *            New value of property sinceRevision.
     */
    public void setSinceRevision(final String sinceRevision) {
        this.sinceRevision = sinceRevision;
    }

    /**
     * Getter for property reportTags. (cvs switch -T)
     * 
     * @return Value of property reportTags.
     */
    public boolean isReportTags() {
        return reportTags;
    }

    /**
     * Setter for property reportTags. (cvs switch -T)
     * 
     * @param reportTags
     *            New value of property reportTags.
     */
    public void setReportTags(final boolean reportTags) {
        this.reportTags = reportTags;
    }

    /**
     * Getter for property sinceTag. (cvs switch -t)
     * 
     * @return Value of property sinceTag.
     */
    public String getSinceTag() {
        return sinceTag;
    }

    /**
     * Setter for property sinceTag. (cvs switch -t)
     * 
     * @param sinceTag
     *            New value of property sinceTag.
     */
    public void setSinceTag(final String sinceTag) {
        this.sinceTag = sinceTag;
    }

    /**
     * Getter for property forWorkingDirectory. (cvs switch -w)
     * 
     * @return Value of property forWorkingDirectory.
     */
    public boolean isForWorkingDirectory() {
        return forWorkingDirectory;
    }

    /**
     * Setter for property forWorkingDirectory. (cvs switch -w)
     * 
     * @param forWorkingDirectory
     *            New value of property forWorkingDirectory.
     */
    public void setForWorkingDirectory(final boolean forWorkingDirectory) {
        this.forWorkingDirectory = forWorkingDirectory;
    }

    /**
     * Getter for property reportEventType. (cvs switch -x)
     * 
     * @return Value of property reportEventType.
     */
    public String getReportEventType() {
        return reportEventType;
    }

    /**
     * Setter for property reportEventType. (cvs switch -x)
     * 
     * @param reportEventType
     *            New value of property reportEventType.
     */
    public void setReportEventType(final String reportEventType) {
        this.reportEventType = reportEventType;
    }

    /**
     * Getter for property timeZone. (cvs switch -z)
     * 
     * @return Value of property timeZone.
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Setter for property timeZone. (cvs switch -z)
     * 
     * @param timeZone
     *            New value of property timeZone.
     */
    public void setTimeZone(final String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Getter for property lastEventForFile. (cvs switch -f)
     * 
     * @return Value of property lastEventForFile.
     */
    public String[] getLastEventForFile() {
        return lastEventForFile;
    }

    /**
     * Setter for property lastEventForFile. (cvs switch -f)
     * 
     * @param lastEventForFile
     *            New value of property lastEventForFile.
     */
    public void setLastEventForFile(final String[] lastEventForFile) {
        this.lastEventForFile = lastEventForFile;
    }

    public void addLastEventForFile(final String newFile) {
        lastEventForFile = addNewValue(lastEventForFile, newFile);
    }

    /**
     * Getter for property reportOnModule. (cvs switch -m)
     * 
     * @return Value of property reportOnModule.
     */
    public String[] getReportOnModule() {
        return reportOnModule;
    }

    /**
     * Setter for property reportOnModule. (cvs switch -m)
     * 
     * @param reportOnModule
     *            New value of property reportOnModule.
     */
    public void setReportOnModule(final String[] reportOnModule) {
        this.reportOnModule = reportOnModule;
    }

    public void addReportOnModule(final String newReportOnModule) {
        reportOnModule = addNewValue(reportOnModule, newReportOnModule);
    }

    /**
     * Getter for property reportLastEventForModule. (cvs switch -n)
     * 
     * @return Value of property reportLastEventForModule.
     */
    public String[] getReportLastEventForModule() {
        return reportLastEventForModule;
    }

    /**
     * Setter for property reportLastEventForModule. (cvs switch -n)
     * 
     * @param reportLastEventForModule
     *            New value of property reportLastEventForModule.
     */
    public void setReportLastEventForModule(final String[] reportLastEventForModule) {
        this.reportLastEventForModule = reportLastEventForModule;
    }

    public void addReportLastEventForModule(final String newModule) {
        reportLastEventForModule = addNewValue(reportLastEventForModule, newModule);
    }

    /**
     * Getter for property forUsers. (cvs switch -u)
     * 
     * @return Value of property forUsers.
     */
    public String[] getForUsers() {
        return forUsers;
    }

    /**
     * Setter for property forUsers. (cvs switch -u)
     * 
     * @param forUsers
     *            New value of property forUsers.
     */
    public void setForUsers(final String[] forUsers) {
        this.forUsers = forUsers;
    }

    public void addForUsers(final String forUser) {
        forUsers = addNewValue(forUsers, forUser);
    }

    private String[] addNewValue(String[] arr, final String newVal) {
        if (arr == null) {
            arr = new String[] { newVal };
            return arr;
        }
        final String[] newValue = new String[arr.length + 1];
        for (int i = 0; i < arr.length; i++) {
            newValue[i] = arr[i];
        }
        newValue[newValue.length] = newVal;
        return newValue;
    }
}
