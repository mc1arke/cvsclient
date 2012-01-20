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
package org.netbeans.lib.cvsclient.command.editors;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import org.netbeans.lib.cvsclient.command.Builder;
import org.netbeans.lib.cvsclient.command.FileInfoContainer;
import org.netbeans.lib.cvsclient.event.CVSEvent;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;

/**
 * @author Thomas Singer
 * @version Nov 11, 2001
 */
public class EditorsBuilder implements Builder {
    // Constants ==============================================================

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd hh:mm:ss yyyy");
    // private static final DateFormat DATE_FORMAT = new
    // SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy zzz");

    // Fields =================================================================

    private final EventManager eventManager;

    private String editorsFileName;

    // Setup ==================================================================

    EditorsBuilder(final EventManager eventManager) {
        editorsFileName = null;
        this.eventManager = eventManager;
    }

    // Implemented ============================================================

    public void parseLine(final String line, final boolean isErrorMessage) {
        if (!isErrorMessage) {
            parseLine(line);
        }
    }

    public void parseEnhancedMessage(final String key, final Object value) {
    }

    public void outputDone() {
    }

    // Utils ==================================================================

    private boolean parseLine(final String line) {
        final StringTokenizer tokenizer = new StringTokenizer(line, "\t");
        if (!tokenizer.hasMoreTokens()) {
            return false;
        }

        // check whether line is the first editors line for this file.
        // persist for later lines.
        if (!line.startsWith("\t")) {
            editorsFileName = tokenizer.nextToken();
            if (!tokenizer.hasMoreTokens()) {
                return false;
            }
        }
        // must have a filename associated with the line,
        // either from this line or a previous one
        else if (editorsFileName == null) {
            return false;
        }

        final String user = tokenizer.nextToken();
        if (!tokenizer.hasMoreTokens()) {
            return false;
        }

        final String dateString = tokenizer.nextToken();
        if (!tokenizer.hasMoreTokens()) {
            return false;
        }

        final String clientName = tokenizer.nextToken();
        if (!tokenizer.hasMoreTokens()) {
            return false;
        }

        final String localDirectory = tokenizer.nextToken();

        try {
            final FileInfoContainer fileInfoContainer = parseEntries(localDirectory, editorsFileName, user, dateString,
                            clientName);
            final CVSEvent event = new FileInfoEvent(this, fileInfoContainer);
            eventManager.fireCVSEvent(event);
            return true;
        } catch (final ParseException ex) {
            return false;
        }
    }

    private EditorsFileInfoContainer parseEntries(final String localDirectory, String fileName, final String user,
                    final String dateString, final String clientName) throws ParseException {
        final int lastSlashIndex = fileName.lastIndexOf('/');
        if (lastSlashIndex >= 0) {
            fileName = fileName.substring(lastSlashIndex + 1);
        }

        final Date date = parseDate(dateString);
        final File file = new File(localDirectory, fileName);
        return new EditorsFileInfoContainer(file, user, date, clientName);
    }

    private Date parseDate(String dateString) throws ParseException {
        final int firstSpaceIndex = Math.max(dateString.indexOf(' '), 0);
        final int lastSpaceIndex = Math.min(dateString.lastIndexOf(' '), dateString.length());

        // dateString = dateString.substring(0, lastSpaceIndex).trim();
        dateString = dateString.substring(firstSpaceIndex, lastSpaceIndex).trim();

        return DATE_FORMAT.parse(dateString);
    }
}
