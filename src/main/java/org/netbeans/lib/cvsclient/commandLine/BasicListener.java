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
package org.netbeans.lib.cvsclient.commandLine;

import java.io.PrintStream;

import org.netbeans.lib.cvsclient.event.BinaryMessageEvent;
import org.netbeans.lib.cvsclient.event.CVSAdapter;
import org.netbeans.lib.cvsclient.event.EnhancedMessageEvent;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;
import org.netbeans.lib.cvsclient.event.MessageEvent;

/**
 * A basic implementation of a CVS listener. Is really only interested in
 * message events. This listener is suitable for command line clients and
 * clients that don't "persist".
 * 
 * @author Robert Greig
 */
public class BasicListener extends CVSAdapter {
    /**
     * 
     */
    private static final long serialVersionUID = 4746765590780487188L;
    private final StringBuffer taggedLine = new StringBuffer();
    private final PrintStream stdout;
    private final PrintStream stderr;

    public BasicListener() {
        this(System.out, System.err);
    }

    public BasicListener(final PrintStream stdout, final PrintStream stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
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
        final String line = e.getMessage();
        if (e instanceof EnhancedMessageEvent) {
            return;
        }
        final PrintStream stream = e.isError() ? stderr : stdout;

        if (e.isTagged()) {
            final String message = MessageEvent.parseTaggedMessage(taggedLine, e.getMessage());
            if (message != null) {
                stream.println(message);
            }
        } else {
            stream.println(line);
        }
    }

    /**
     * Called when the server wants to send a binary message to be displayed to
     * the user. The message is only for information purposes and clients can
     * choose to ignore these messages if they wish.
     * 
     * @param e
     *            the event
     */
    @Override
    public void messageSent(final BinaryMessageEvent e) {
        final byte[] bytes = e.getMessage();
        final int len = e.getMessageLength();
        stdout.write(bytes, 0, len);
    }

    /**
     * Called when file status information has been received
     */
    @Override
    public void fileInfoGenerated(final FileInfoEvent e) {
        // FileInfoContainer fileInfo = e.getInfoContainer();
        // if (fileInfo.getClass().equals(StatusInformation.class)) {
        // System.err.println("A file status event was received.");
        // System.err.println("The status information object is: " +
        // fileInfo);
        // }
    }
}
