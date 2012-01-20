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
 *
 * The Original Software is the CVS Client Library.
 * The Initial Developer of the Original Software is Robert Greig.
 *
 * Contributor(s): Robert Greig, Milos Kleint.
 *****************************************************************************/
package org.netbeans.lib.cvsclient.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.netbeans.lib.cvsclient.request.GzipStreamRequest;
import org.netbeans.lib.cvsclient.request.Request;

/**
 * Handles the reading and writing of Compressed files to and from the server.
 * 
 * @author Milos Kleint
 */
public class GzippedFileHandler extends DefaultFileHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -7374926849219239145L;

    /**
     * Get any requests that must be sent before commands are sent, to init this
     * file handler.
     * 
     * @return an array of Requests that must be sent
     */
    @Override
    public Request[] getInitialisationRequests() {
        return new Request[] { new GzipStreamRequest() };
    }

    @Override
    protected Reader getProcessedReader(final File f) throws IOException {
        return new InputStreamReader(new GZIPInputStream(new FileInputStream(f)));
    }

    @Override
    protected InputStream getProcessedInputStream(final File f) throws IOException {
        return new GZIPInputStream(new FileInputStream(f));
    }
}
