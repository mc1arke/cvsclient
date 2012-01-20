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

 * Contributor(s): Robert Greig.
 *****************************************************************************/
package org.netbeans.lib.cvsclient.command.status;

import java.io.File;

import org.netbeans.lib.cvsclient.ClientServices;
import org.netbeans.lib.cvsclient.command.BasicCommand;
import org.netbeans.lib.cvsclient.command.Builder;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.TerminationEvent;
import org.netbeans.lib.cvsclient.request.ArgumentRequest;
import org.netbeans.lib.cvsclient.request.CommandRequest;

/**
 * The status command looks up the status of files in the repository
 * 
 * @author Robert Greig
 */
public class StatusCommand extends BasicCommand {
    /**
     * 
     */
    private static final long serialVersionUID = 2351137326139689665L;

    /**
     * Holds value of property includeTags.
     */
    private boolean includeTags;

    /**
     * Construct a new status command
     */
    public StatusCommand() {
    }

    /**
     * Create a builder for this command.
     * 
     * @param eventMan
     *            the event manager used to receive events.
     */
    @Override
    public Builder createBuilder(final EventManager eventManager) {
        return new StatusBuilder(eventManager, this);
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

        super.execute(client, em);

        try {
            // parameters come now..
            if (includeTags) {
                requests.add(1, new ArgumentRequest("-v")); // NOI18N
            }

            addRequestForWorkingDirectory(client);
            addArgumentRequests();
            addRequest(CommandRequest.STATUS);

            client.processRequests(requests);
        } catch (final CommandException ex) {
            throw ex;
        } catch (final Exception e) {
            throw new CommandException(e, e.getLocalizedMessage());
        } finally {
            requests.clear();
        }
    }

    /**
     * Getter for property includeTags.
     * 
     * @return Value of property includeTags.
     */
    public boolean isIncludeTags() {
        return includeTags;
    }

    /**
     * Setter for property includeTags.
     * 
     * @param includeTags
     *            New value of property includeTags.
     */
    public void setIncludeTags(final boolean inclTags) {
        includeTags = inclTags;
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
     * This method returns how the command would looklike when typed on the
     * command line. Each command is responsible for constructing this
     * information.
     * 
     * @returns <command's name> [<parameters>] files/dirs. Example: checkout -p
     *          CvsCommand.java
     */
    @Override
    public String getCVSCommand() {
        final StringBuffer toReturn = new StringBuffer("status "); // NOI18N
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
        } else if (opt == 'v') {
            setIncludeTags(true);
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
        return "Rlv"; // NOI18N
    }

    /**
     * resets all switches in the command. After calling this method, the
     * command should have no switches defined and should behave defaultly.
     */
    @Override
    public void resetCVSCommand() {
        setRecursive(true);
        setIncludeTags(false);
    }

    /**
     * Returns the arguments of the command in the command-line style. Similar
     * to getCVSCommand() however without the files and command's name
     */
    @Override
    public String getCVSArguments() {
        final StringBuffer toReturn = new StringBuffer(""); // NOI18N
        if (isIncludeTags()) {
            toReturn.append("-v "); // NOI18N
        }
        if (!isRecursive()) {
            toReturn.append("-l "); // NOI18N
        }
        return toReturn.toString();
    }

}
