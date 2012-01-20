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

package org.netbeans.lib.cvsclient.command.annotate;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.netbeans.lib.cvsclient.ClientServices;
import org.netbeans.lib.cvsclient.command.BasicCommand;
import org.netbeans.lib.cvsclient.command.Builder;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.TerminationEvent;
import org.netbeans.lib.cvsclient.request.ArgumentRequest;
import org.netbeans.lib.cvsclient.request.CommandRequest;
import org.netbeans.lib.cvsclient.request.EntryRequest;
import org.netbeans.lib.cvsclient.request.Request;

/**
 * The annotate command shows all lines of the file and annotates each line with
 * cvs-related info.
 * 
 * @author Milos Kleint
 */
public class AnnotateCommand extends BasicCommand {
    /**
     * 
     */
    private static final long serialVersionUID = -5863346686639332500L;

    /**
     * The event manager to use
     */
    protected EventManager eventManager;

    /**
     * Use head revision if a revision meeting criteria set by switches -r/-D
     * (tag/date) is not found.
     */
    private boolean useHeadIfNotFound;

    /**
     * equals the -D switch of command line cvs.
     */
    private String annotateByDate;

    /**
     * Equals the -r switch of command-line cvs.
     */
    private String annotateByRevision;

    /**
     * Construct a new diff command
     */
    public AnnotateCommand() {
    }

    /**
     * Create a builder for this command.
     * 
     * @param eventMan
     *            the event manager used to receive events.
     */
    @Override
    public Builder createBuilder(final EventManager eventMan) {
        return new AnnotateBuilder(eventMan, this);
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
        eventManager = em;

        client.ensureConnection();

        super.execute(client, em);

        excludeBinaryFiles(requests);

        try {
            if (useHeadIfNotFound) {
                requests.add(1, new ArgumentRequest("-f")); // NOI18N
            }
            if ((annotateByDate != null) && (annotateByDate.length() > 0)) {
                requests.add(1, new ArgumentRequest("-D")); // NOI18N
                requests.add(2, new ArgumentRequest(getAnnotateByDate()));
            }
            if ((annotateByRevision != null) && (annotateByRevision.length() > 0)) {
                requests.add(1, new ArgumentRequest("-r")); // NOI18N
                requests.add(2, new ArgumentRequest(getAnnotateByRevision()));
            }
            addRequestForWorkingDirectory(client);
            addArgumentRequests();
            addRequest(CommandRequest.ANNOTATE);
            client.processRequests(requests);
        } catch (final CommandException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CommandException(ex, ex.getLocalizedMessage());
        } finally {
            requests.clear();
        }
    }

    private void excludeBinaryFiles(final List<Request> requests) {
        final Iterator<Request> it = requests.iterator();
        while (it.hasNext()) {
            final Object obj = it.next();
            if (obj instanceof EntryRequest) {
                final EntryRequest req = (EntryRequest) obj;
                if (req.getEntry().isBinary()) {
                    it.remove();
                    if (it.hasNext()) {
                        // removes also the follwoing modified/unchanged request
                        it.next();
                        it.remove();
                    }
                }
            }
        }
    }

    /**
     * called when server responses with "ok" or "error", (when the command
     * finishes)
     */
    @Override
    public void commandTerminated(final TerminationEvent e) {
        if (builder != null) {
            builder.outputDone();
        }
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
     * Getter for property annotateByDate.
     * 
     * @return Value of property annotateByDate.
     */
    public String getAnnotateByDate() {
        return annotateByDate;
    }

    /**
     * Setter for property annotateByDate.
     * 
     * @param annotateByDate
     *            New value of property annotateByDate.
     */
    public void setAnnotateByDate(final String annotateByDate) {
        this.annotateByDate = annotateByDate;
    }

    /**
     * Getter for property annotateByRevision.
     * 
     * @return Value of property annotateByRevision.
     */
    public String getAnnotateByRevision() {
        return annotateByRevision;
    }

    /**
     * Setter for property annotateByRevision.
     * 
     * @param annotateByRevision
     *            New value of property annotateByRevision.
     */
    public void setAnnotateByRevision(final String annotateByRevision) {
        this.annotateByRevision = annotateByRevision;
    }

    /**
     * This method returns how the command would looklike when typed on the
     * command line. Each command is responsible for constructing this
     * information.
     * 
     * @returns <command's name> [<parameters>] files/dirs. Example: checkout -p
     *          CvsCommand.java
     */
    @Override
    public String getCVSCommand() {
        final StringBuffer toReturn = new StringBuffer("annotate "); // NOI18N
        toReturn.append(getCVSArguments());
        final File[] files = getFiles();
        if (files != null) {
            for (final File file : files) {
                toReturn.append(file.getName() + " "); // NOI18N
            }
        }
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
        if (opt == 'R') {
            setRecursive(true);
        } else if (opt == 'l') {
            setRecursive(false);
        } else if (opt == 'r') {
            setAnnotateByRevision(optArg);
        } else if (opt == 'D') {
            setAnnotateByDate(optArg);
        } else if (opt == 'f') {
            setUseHeadIfNotFound(true);
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
        return "Rlr:D:f"; // NOI18N
    }

    /**
     * resets all switches in the command. After calling this method, the
     * command should have no switches defined and should behave defaultly.
     */
    @Override
    public void resetCVSCommand() {
        setRecursive(true);
        setAnnotateByDate(null);
        setAnnotateByRevision(null);
        setUseHeadIfNotFound(false);
    }

    /**
     * Returns the arguments of the command in the command-line style. Similar
     * to getCVSCommand() however without the files and command's name
     */
    @Override
    public String getCVSArguments() {
        final StringBuffer toReturn = new StringBuffer(""); // NOI18N
        if (!isRecursive()) {
            toReturn.append("-l "); // NOI18N
        }
        if (getAnnotateByRevision() != null) {
            toReturn.append("-r "); // NOI18N
            toReturn.append(getAnnotateByRevision());
            toReturn.append(" "); // NOI18N
        }
        if (getAnnotateByDate() != null) {
            toReturn.append("-D "); // NOI18N
            toReturn.append(getAnnotateByDate());
            toReturn.append(" "); // NOI18N
        }
        if (isUseHeadIfNotFound()) {
            toReturn.append("-f "); // NOI18N
        }
        return toReturn.toString();
    }

}
