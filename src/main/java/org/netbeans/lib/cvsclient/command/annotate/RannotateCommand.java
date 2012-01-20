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

import java.util.LinkedList;
import java.util.List;

import org.netbeans.lib.cvsclient.ClientServices;
import org.netbeans.lib.cvsclient.command.BasicCommand;
import org.netbeans.lib.cvsclient.command.Builder;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.ModuleExpansionEvent;
import org.netbeans.lib.cvsclient.event.TerminationEvent;
import org.netbeans.lib.cvsclient.request.ArgumentRequest;
import org.netbeans.lib.cvsclient.request.CommandRequest;
import org.netbeans.lib.cvsclient.request.DirectoryRequest;
import org.netbeans.lib.cvsclient.request.ExpandModulesRequest;
import org.netbeans.lib.cvsclient.request.Request;
import org.netbeans.lib.cvsclient.request.RootRequest;

/**
 * The rannotate command is similar to anootate, but doens't operate on
 * currently checked out sources.
 * 
 * 
 * @author MIlos Kleint
 */
public class RannotateCommand extends BasicCommand {

    /**
     * 
     */
    private static final long serialVersionUID = 1803697825415871937L;

    /**
     * The modules to checkout. These names are unexpanded and will be passed to
     * a module-expansion request.
     */
    private final List<String> modules = new LinkedList<String>();

    /**
     * The expanded modules.
     */
    private final List<String> expandedModules = new LinkedList<String>();

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

    public RannotateCommand() {
        resetCVSCommand();
    }

    /**
     * Set the modules to export.
     * 
     * @param theModules
     *            the names of the modules to export
     */
    public void setModule(final String module) {
        modules.add(module);
    }

    /**
     * clears the list of modules for export.
     */

    public void clearModules() {
        modules.clear();
    }

    /**
     * Set the modules to export.
     * 
     * @param theModules
     *            the names of the modules to export
     */
    public void setModules(final String[] modules) {
        clearModules();
        if (modules == null) {
            return;
        }
        for (final String module : modules) {
            this.modules.add(module);
        }
    }

    public String[] getModules() {
        String[] mods = new String[modules.size()];
        mods = modules.toArray(mods);
        return mods;
    }

    /**
     * Execute this command.
     * 
     * @param client
     *            the client services object that provides any necessary
     *            services to this command, including the ability to actually
     *            process all the requests
     */
    @Override
    public void execute(final ClientServices client, final EventManager em) throws CommandException,
                    AuthenticationException {

        client.ensureConnection();

        requests = new LinkedList<Request>();
        if (client.isFirstCommand()) {
            requests.add(new RootRequest(client.getRepository()));
        }
        for (final String string : modules) {
            final String module = string;
            requests.add(new ArgumentRequest(module));
        }
        expandedModules.clear();
        requests.add(new DirectoryRequest(".", client.getRepository())); // NOI18N
        requests.add(new ExpandModulesRequest());
        try {
            client.processRequests(requests);
        } catch (final CommandException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CommandException(ex, ex.getLocalizedMessage());
        }
        requests.clear();
        postExpansionExecute(client, em);
    }

    /**
     * This is called when the server has responded to an expand-modules
     * request.
     */
    @Override
    public void moduleExpanded(final ModuleExpansionEvent e) {
        expandedModules.add(e.getModule());
    }

    /**
     * Execute this command
     * 
     * @param client
     *            the client services object that provides any necessary
     *            services to this command, including the ability to actually
     *            process all the requests
     */
    private void postExpansionExecute(final ClientServices client, final EventManager em) throws CommandException,
                    AuthenticationException {

        // processExistingModules(client.getLocalPath());
        super.execute(client, em);

        //
        // moved modules code to the end of the other arguments --GAR
        //
        if (!isRecursive()) {
            requests.add(1, new ArgumentRequest("-l")); // NOI18N
        }
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

        for (final String string : modules) {
            final String module = string;
            requests.add(new ArgumentRequest(module));
        }

        requests.add(new DirectoryRequest(".", client.getRepository())); // NOI18N
        requests.add(CommandRequest.RANNOTATE);
        try {
            client.processRequests(requests);
            requests.clear();

        } catch (final CommandException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CommandException(ex, ex.getLocalizedMessage());
        }
    }

    @Override
    public String getCVSCommand() {
        final StringBuffer toReturn = new StringBuffer("rannotate "); // NOI18N
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

    @Override
    public void resetCVSCommand() {
        setRecursive(true);
        setAnnotateByDate(null);
        setAnnotateByRevision(null);
        setUseHeadIfNotFound(false);
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

}
