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
package org.netbeans.lib.cvsclient.command.watch;

import org.netbeans.lib.cvsclient.ClientServices;
import org.netbeans.lib.cvsclient.command.BasicCommand;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.Watch;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.TerminationEvent;
import org.netbeans.lib.cvsclient.request.ArgumentRequest;

/**
 * @author Thomas Singer
 */
public class WatchCommand extends BasicCommand {
    /**
     * 
     */
    private static final long serialVersionUID = -6107153470303163315L;

    private WatchMode watchMode;

    private Watch watch;

    /**
     * Construct a new WatchCommand.
     */
    public WatchCommand() {
        resetCVSCommand();
    }

    /**
     * Executes this command.
     * 
     * @param client
     *            the client services object that provides any necessary
     *            services to this command, including the ability to actually
     *            process all the requests
     * @param eventManager
     *            the EventManager used for sending events
     * 
     * @throws IllegalStateException
     *             if the commands options aren't set correctly
     * @throws AuthenticationException
     *             if the connection could not be established
     * @throws CommandException
     *             if some other thing gone wrong
     */
    @Override
    public void execute(final ClientServices client, final EventManager eventManager) throws CommandException,
                    AuthenticationException {
        checkState();

        client.ensureConnection();

        try {
            super.execute(client, eventManager);

            if (getWatchMode().isWatchOptionAllowed()) {
                final String[] arguments = getWatchNotNull().getArguments();
                for (final String argument : arguments) {
                    addRequest(new ArgumentRequest("-a")); // NOI18N
                    addRequest(new ArgumentRequest(argument));
                }
            }

            addRequestForWorkingDirectory(client);
            addArgumentRequests();
            addRequest(getWatchMode().getCommand());

            client.processRequests(requests);
        } catch (final CommandException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CommandException(ex, ex.getLocalizedMessage());
        } finally {
            requests.clear();
        }
    }

    /**
     * If a builder was set-up, it's outputDone() method is called. This method
     * is called, when the server responses with "ok" or "error" (== when the
     * command finishes).
     */
    @Override
    public void commandTerminated(final TerminationEvent e) {
        if (builder != null) {
            builder.outputDone();
        }
    }

    /**
     * Uses the specified argument to set the appropriate properties. To be
     * mainly used for automatic settings (like parsing the .cvsrc file)
     * 
     * @return whether the option (switch) was recognized and set
     */
    @Override
    public boolean setCVSCommand(final char opt, final String optArg) {
        if (opt == 'R') {
            setRecursive(true);
        } else if (opt == 'l') {
            setRecursive(false);
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
        return "Rl"; // NOI18N
    }

    /**
     * Resets all switches in this command. After calling this method, the
     * command behaves like newly created.
     */
    @Override
    public void resetCVSCommand() {
        setRecursive(true);
        setWatch(null);
    }

    /**
     * Returns how this command would look like when typed on the command line.
     */
    @Override
    public String getCVSCommand() {
        final StringBuffer cvsCommand = new StringBuffer("watch "); // NOI18N
        cvsCommand.append(getCVSArguments());
        appendFileArguments(cvsCommand);
        return cvsCommand.toString();
    }

    /**
     * Returns the arguments of the command in the command-line style. Similar
     * to getCVSCommand() however without the files and command's name
     */
    @Override
    public String getCVSArguments() {
        checkState();

        final StringBuffer cvsArguments = new StringBuffer();
        cvsArguments.append(getWatchMode().toString());
        cvsArguments.append(' ');

        if (!isRecursive()) {
            cvsArguments.append("-l "); // NOI18N
        }

        if (getWatchMode().isWatchOptionAllowed()) {
            cvsArguments.append("-a ");
            cvsArguments.append(getWatchNotNull().toString());
        }
        return cvsArguments.toString();
    }

    /**
     * Returns the WatchMode.
     */
    public WatchMode getWatchMode() {
        return watchMode;
    }

    /**
     * Sets the WatchMode.
     */
    public void setWatchMode(final WatchMode watchMode) {
        this.watchMode = watchMode;
    }

    /**
     * Returns the watch.
     */
    public Watch getWatch() {
        return watch;
    }

    private Watch getWatchNotNull() {
        if (watch == null) {
            return Watch.ALL;
        }
        return watch;
    }

    /**
     * Sets the watch. If the WatchMode ADD or REMOVE is used, null is the same
     * as Watch.ALL. If the WatchMode ON or OFF is used, this option isn't used
     * at all.
     */
    public void setWatch(final Watch watch) {
        this.watch = watch;
    }

    private void checkState() {
        if (getWatchMode() == null) {
            throw new IllegalStateException("Watch mode expected!");
        }
    }
}
