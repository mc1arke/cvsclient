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
package org.netbeans.lib.cvsclient.command;

import java.io.File;
import java.io.IOException;

import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;

/**
 * Handles the building of "checkout with -p switch" information object and
 * storing of the checked out file to the temporary file and the firing of
 * events when complete objects are built.
 * 
 * @author Milos Kleint
 */
public class PipedFilesBuilder implements Builder, BinaryBuilder {

    private static final String ERR_CHECK = "Checking out "; // NOI18N
    private static final String ERR_RCS = "RCS:  "; // NOI18N
    private static final String ERR_VERS = "VERS: "; // NOI18N
    private static final String EXAM_DIR = ": Updating"; // NOI18N

    private static final byte[] lineSeparator = System.getProperty("line.separator").getBytes();

    /**
     * The module object that is currently being built.
     */
    private PipedFileInformation fileInformation;

    /**
     * The event manager to use.
     */
    private final EventManager eventManager;

    /**
     * The directory in which the file being processed lives. This is relative
     * to the local directory.
     */
    private String fileDirectory;

    private final BuildableCommand command;

    private final TemporaryFileCreator tempFileCreator;

    /**
     * Creates a new Builder for the PipeFileResponse.
     */
    public PipedFilesBuilder(final EventManager eventManager, final BuildableCommand command,
                    final TemporaryFileCreator tempFileCreator) {
        this.eventManager = eventManager;
        this.command = command;
        this.tempFileCreator = tempFileCreator;
    }

    public void outputDone() {
        if (fileInformation == null) {
            return;
        }

        try {
            fileInformation.closeTempFile();
        } catch (final IOException exc) {
            // TODO
        }
        eventManager.fireCVSEvent(new FileInfoEvent(this, fileInformation));
        fileInformation = null;
    }

    public void parseBytes(final byte[] bytes, final int len) {
        if (fileInformation == null) {
            // HOTFIX there is no header for :local: repositories (thereare two
            // copies in this source)
            // XXX it might be dangerous because PipedFileInformation stays
            // partialy unitialized
            try {
                fileInformation = new PipedFileInformation(File.createTempFile("checkout", null));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        try {
            fileInformation.addToTempFile(bytes, len);
        } catch (final IOException exc) {
            outputDone();
        }
    }

    public void parseLine(final String line, final boolean isErrorMessage) {
        if (isErrorMessage) {
            if (line.indexOf(EXAM_DIR) >= 0) {
                fileDirectory = line.substring(line.indexOf(EXAM_DIR) + EXAM_DIR.length()).trim();
            } else if (line.startsWith(ERR_CHECK)) {
                processFile(line);
            } else if (line.startsWith(ERR_RCS)) {
                if (fileInformation != null) {
                    final String repositoryName = line.substring(ERR_RCS.length()).trim();
                    fileInformation.setRepositoryFileName(repositoryName);
                }
            } else if (line.startsWith(ERR_VERS)) {
                if (fileInformation != null) {
                    final String repositoryRevision = line.substring(ERR_RCS.length()).trim();
                    fileInformation.setRepositoryRevision(repositoryRevision);
                }
            }
            // header stuff..
        } else {
            if (fileInformation == null) {
                // HOTFIX there is no header for :local: repositories (thereare
                // two copies in this source)
                // XXX it might be dangerous because PipedFileInformation stays
                // partialy unitialized
                try {
                    fileInformation = new PipedFileInformation(File.createTempFile("checkout", null));
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileInformation != null) {
                try {
                    fileInformation.addToTempFile(line.getBytes("ISO-8859-1")); // see
                                                                                // BuildableCommand
                    fileInformation.addToTempFile(lineSeparator);
                } catch (final IOException exc) {
                    outputDone();
                }
            }
        }
    }

    private void processFile(final String line) {
        outputDone();
        final String filename = line.substring(ERR_CHECK.length());
        try {
            final File temporaryFile = tempFileCreator.createTempFile(filename);
            fileInformation = new PipedFileInformation(temporaryFile);
        } catch (final IOException ex) {
            fileInformation = null;
            return;
        }
        fileInformation.setFile(createFile(filename));
    }

    private File createFile(final String fileName) {
        final File file = new File(command.getLocalDirectory(), fileName);
        return file;
    }

    public void parseEnhancedMessage(final String key, final Object value) {
    }
}
