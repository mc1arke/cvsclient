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
package org.netbeans.lib.cvsclient.response;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.util.LoggedDataInputStream;

/**
 * This Template response allows the server to send a template file that is used
 * when committing changes. The client tools can read the Template file which is
 * stored in CVS/Template and display it to the user to be used as a prompt for
 * commit comments.
 * 
 * @author Robert Greig
 */
class TemplateResponse implements Response {
    /**
     * A reference to an uncompressed file handler
     */
    /*
     * // TODO: Should this be taken from ResponseServices??? protected static
     * FileHandler uncompressedFileHandler;
     */

    /**
     * 
     */
    private static final long serialVersionUID = -114151126684247398L;

    /**
     * The local path of the new file
     */
    protected String localPath;

    /**
     * The full repository path of the file
     */
    protected String repositoryPath;

    /**
     * Creates new TemplateResponse
     */
    public TemplateResponse() {
    }

    /*
     * // TODO: replace this with a call to
     * ResponseSerivices::getUncompr....ler? protected static FileHandler
     * getUncompressedFileHandler() { if (uncompressedFileHandler == null) {
     * uncompressedFileHandler = new DefaultFileHandler(); } return
     * uncompressedFileHandler; }
     */

    /**
     * Process the data for the response.
     * 
     * @param dis
     *            the data inputstream allowing the client to read the server's
     *            response. Note that the actual response name has already been
     *            read and the input stream is positioned just before the first
     *            argument, if any.
     */
    public void process(final LoggedDataInputStream dis, final ResponseServices services) throws ResponseException {
        try {
            localPath = dis.readLine();
            repositoryPath = dis.readLine();

            final int length = Integer.parseInt(dis.readLine());

            // now read in the file
            final String filePath = services.convertPathname(localPath, repositoryPath) + "CVS/Template"; // NOI18N

            // #69639 write metadata directly
            // XXX possibly add a method to AdminHandler
            OutputStream out = null;
            final File file = new File(filePath);
            file.getParentFile().mkdirs();
            try {
                out = new FileOutputStream(file);
                out = new BufferedOutputStream(out);
                final byte[] lineSeparator = System.getProperty("line.separator").getBytes(); // NOI18N
                final byte[] data = dis.readBytes(length);
                for (final byte ch : data) {
                    if (ch == '\n') {
                        out.write(lineSeparator);
                    } else {
                        out.write(ch);
                    }
                }
            } catch (final EOFException eof) {
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (final IOException alreadyClosed) {
                    }
                }
            }
        } catch (final EOFException ex) {
            final String localMessage = CommandException.getLocalMessage("CommandException.EndOfFile"); // NOI18N
            throw new ResponseException(ex, localMessage);
        } catch (final IOException ex) {
            throw new ResponseException(ex);
        }
    }

    /**
     * Is this a terminal response, i.e. should reading of responses stop after
     * this response. This is true for responses such as OK or an error response
     */
    public boolean isTerminalResponse() {
        return false;
    }
}
