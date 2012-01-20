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
 * The Initial Developer of the Original Software is Milos Kleint.
 * Portions created by Milos Kleint are Copyright (C) 2000.
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

 * Contributor(s): Milos Kleint.
 *****************************************************************************/
package org.netbeans.lib.cvsclient.commandLine.command;

import java.io.File;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.netbeans.lib.cvsclient.command.Command;
import org.netbeans.lib.cvsclient.command.FileInfoContainer;
import org.netbeans.lib.cvsclient.command.GlobalOptions;
import org.netbeans.lib.cvsclient.command.annotate.AnnotateCommand;
import org.netbeans.lib.cvsclient.command.annotate.AnnotateInformation;
import org.netbeans.lib.cvsclient.command.annotate.AnnotateLine;
import org.netbeans.lib.cvsclient.commandLine.GetOpt;
import org.netbeans.lib.cvsclient.commandLine.ListenerProvider;
import org.netbeans.lib.cvsclient.event.CVSAdapter;
import org.netbeans.lib.cvsclient.event.CVSListener;

/**
 * The locbundlecheck command
 * 
 * @author Milos Kleint
 */
public class locbundlecheck extends CVSAdapter implements CommandProvider {

    /**
     * 
     */
    private static final long serialVersionUID = -9190064565949911055L;

    /**
     * A constructor that is used to create the CommandProvider.
     */
    public locbundlecheck() {
    }

    public String getName() {
        return "locbundlecheck"; // NOI18N
    }

    public String[] getSynonyms() {
        return new String[] { "lbch", "lbcheck" }; // NOI18N
    }

    public String getUsage() {
        return ResourceBundle.getBundle(CommandProvider.class.getPackage().getName() + ".Bundle").getString(
                        "locbundlecheck.usage"); // NOI18N
    }

    public void printShortDescription(final PrintStream out) {
        final String msg = ResourceBundle.getBundle(CommandProvider.class.getPackage().getName() + ".Bundle")
                        .getString("locbundlecheck.shortDescription"); // NOI18N
        out.print(msg);
    }

    public void printLongDescription(final PrintStream out) {
        final String msg = ResourceBundle.getBundle(CommandProvider.class.getPackage().getName() + ".Bundle")
                        .getString("locbundlecheck.longDescription"); // NOI18N
        out.println(msg);
    }

    public Command createCommand(final String[] args, final int index, final GlobalOptions gopt, String workDir) {
        final LocBundleAnnotateCommand command = new LocBundleAnnotateCommand();
        final String getOptString = command.getOptString();
        final GetOpt go = new GetOpt(args, getOptString + "i:");
        int ch = -1;
        go.optIndexSet(index);
        boolean usagePrint = false;
        String localization = null;
        while ((ch = go.getopt()) != GetOpt.optEOF) {
            if (ch == 'i') {
                localization = go.optArgGet();
                command.setLocalization(localization);
            } else {
                final boolean ok = command.setCVSCommand((char) ch, go.optArgGet());
                if (!ok) {
                    usagePrint = true;
                }
            }
        }
        if (usagePrint || (localization == null)) {
            throw new IllegalArgumentException(getUsage());
        }
        final int fileArgsIndex = go.optIndexGet();
        // test if we have been passed any file arguments
        if (fileArgsIndex < args.length) {
            final Collection<File> fls = new ArrayList<File>();

            // send the arguments as absolute paths
            if (workDir == null) {
                workDir = System.getProperty("user.dir");
            }
            command.setWorkDir(workDir);
            final File workingDir = new File(workDir);
            for (int i = fileArgsIndex; i < args.length; i++) {
                final File fl = new File(workingDir, args[i]);
                // System.out.println("file=" + fl);
                if (fl.exists() && fl.isDirectory()) {
                    addFilesInDir(fls, fl, localization);
                } else if (fl.exists() && fl.getName().endsWith(".properties")) {
                    addFiles(fls, fl, localization);
                } else {
                    throw new IllegalArgumentException();
                }
            }
            if (fls.size() > 0) {
                File[] fileArgs = new File[fls.size()];
                fileArgs = fls.toArray(fileArgs);
                command.setFiles(fileArgs);
            } else {
                throw new IllegalArgumentException(ResourceBundle.getBundle(
                                "org/netbeans/lib/cvsclient/commandLine/command/Bundle").getString(
                                "locbundlecheck.no_file_spec"));
            }
        }
        return command;
    }

    private static void addFiles(final Collection<File> fileList, final File origFile, final String localization) {
        final String origPath = origFile.getAbsolutePath();
        final String enarg = origPath.substring(0, origPath.length() - ".properties".length()) + "_" + localization
                        + ".properties";
        // System.out.println("enarg=" + enarg);
        final File addfl = new File(enarg);
        fileList.add(origFile);
        if (addfl.exists()) {
            fileList.add(addfl);
        } else {
            // TODO
        }
    }

    private static void addFilesInDir(final Collection<File> fileList, final File origFile, final String localization) {
        final File[] files = origFile.listFiles();
        if ((files != null) && (files.length > 0)) {
            for (final File file : files) {
                if (file.exists() && file.isDirectory()) {
                    addFilesInDir(fileList, file, localization);
                } else if (file.exists() && "Bundle.properties".equals(file.getName())) {
                    addFiles(fileList, file, localization);
                }
            }
        }
    }

    private PrintStream out;
    private int realEnd = 0;

    private HashMap<String, FileInfoContainer> originalBundles;
    private HashMap<String, FileInfoContainer> localizedBundles;
    private String local;
    private String workDir;

    /**
     * A constructor that is used to create the CVSAdapter.
     */
    locbundlecheck(final PrintStream stdout, final PrintStream stderr, final String localization, final String workDir) {
        out = stdout;
        originalBundles = new HashMap<String, FileInfoContainer>();
        localizedBundles = new HashMap<String, FileInfoContainer>();
        local = localization;
        this.workDir = workDir;
    }

    @Override
    public void fileInfoGenerated(final org.netbeans.lib.cvsclient.event.FileInfoEvent e) {
        // out.println("annotated " + e.getInfoContainer().getFile());
        final FileInfoContainer cont = e.getInfoContainer();
        if (cont.getFile().getName().indexOf("_" + local) >= 0) {
            localizedBundles.put(cont.getFile().getAbsolutePath(), cont);
        } else {
            originalBundles.put(cont.getFile().getAbsolutePath(), cont);
        }
        // out.println("orig size=" + originalBundles.keySet().size() +
        // " loc size=" + localizedBundles.keySet().size());
        if (realEnd == 2) {
            // generate output.
            // out.println("generating output....");
            generateOutput();
        }

    }

    @Override
    public void commandTerminated(final org.netbeans.lib.cvsclient.event.TerminationEvent e) {
        if (realEnd == 0) {
            // now the event is triggered because of the validresponses request
            realEnd = 1;
            return;
        }
        realEnd = 2;
        // the second time it's the real end. waiting for the last info object
        // to be received.
        // out.println("finish=" + e.isError());
    }

    private void generateOutput() {
        final Iterator<String> it = originalBundles.keySet().iterator();
        while (it.hasNext()) {
            final String origPath = it.next();
            final int dotIndex = origPath.lastIndexOf(".");
            if (dotIndex < 0) {
                throw new IllegalStateException(ResourceBundle.getBundle(
                                "org/netbeans/lib/cvsclient/commandLine/command/Bundle").getString(
                                "locbundlecheck.illegal_state"));
            }
            final String locPath = origPath.substring(0, dotIndex) + "_" + local + origPath.substring(dotIndex);
            // System.out.println("locpath=" + locPath);
            final AnnotateInformation origInfo = (AnnotateInformation) originalBundles.get(origPath);
            final AnnotateInformation locInfo = (AnnotateInformation) localizedBundles.get(locPath);
            if (locInfo == null) {
                out.println(MessageFormat
                                .format(ResourceBundle.getBundle(
                                                "org/netbeans/lib/cvsclient/commandLine/command/Bundle").getString(
                                                "locbundlecheck.noLocalizedFile"), new Object[] { origPath }));
                continue;
            }
            // remove from locl bundles to figure out what was removed in the
            // original..
            localizedBundles.remove(locPath);
            final HashMap<String, AnnotateLine> origPropMap = createPropMap(origInfo);
            final HashMap<String, AnnotateLine> locPropMap = createPropMap(locInfo);
            String printFile = origPath;
            if (origPath.startsWith(workDir)) {
                printFile = origPath.substring(workDir.length());
                if (printFile.startsWith("/") || printFile.startsWith("\\")) {
                    printFile = printFile.substring(1);
                }
            }
            out.println(MessageFormat.format(
                            ResourceBundle.getBundle("org/netbeans/lib/cvsclient/commandLine/command/Bundle")
                                            .getString("locbundlecheck.File"), new Object[] { printFile }));
            final Iterator<String> propIt = origPropMap.keySet().iterator();
            while (propIt.hasNext()) {
                final String prop = propIt.next();
                final AnnotateLine origLine = origPropMap.get(prop);
                final AnnotateLine locLine = locPropMap.get(prop);
                if (locLine == null) {
                    out.println(MessageFormat.format(
                                    ResourceBundle.getBundle("org/netbeans/lib/cvsclient/commandLine/command/Bundle")
                                                    .getString("locbundlecheck.propMissing"), new Object[] { prop }));
                    continue;
                }
                // System.out.println("prop=" + prop);
                // System.out.println("orig date:" + origLine.getDate());
                // System.out.println("loc date:" + locLine.getDate());
                if (origLine.getDate().compareTo(locLine.getDate()) > 0) {
                    out.println(MessageFormat.format(
                                    ResourceBundle.getBundle("org/netbeans/lib/cvsclient/commandLine/command/Bundle")
                                                    .getString("locbundlecheck.prop_updated"), new Object[] { prop }));
                }
            }

        }
        if (localizedBundles.size() > 0) {
            final Iterator<String> locIt = localizedBundles.keySet().iterator();
            while (locIt.hasNext()) {
                final String prop = locIt.next();
                out.println(MessageFormat.format(
                                ResourceBundle.getBundle("org/netbeans/lib/cvsclient/commandLine/command/Bundle")
                                                .getString("locbundlecheck.prop_removed"), new Object[] { prop }));
            }
        }
    }

    private HashMap<String, AnnotateLine> createPropMap(final AnnotateInformation info) {
        final HashMap<String, AnnotateLine> propMap = new HashMap<String, AnnotateLine>();
        AnnotateLine line = info.getFirstLine();
        while (line != null) {
            final String content = line.getContent();
            if (content.startsWith("#")) {
                // ignore commented lines.
                line = info.getNextLine();
                continue;
            }
            final int index = content.indexOf('=');
            if (index > 0) {
                final String key = content.substring(0, index);
                propMap.put(key, line);
            } else {
                // TODO.. for properties that span across multiple lines, one
                // should take all lines into account
            }
            line = info.getNextLine();
        }
        return propMap;
    }

    private static class LocBundleAnnotateCommand extends AnnotateCommand implements ListenerProvider {
        /**
         * 
         */
        private static final long serialVersionUID = -4579557097716639872L;
        private String loc;
        private String workDir;

        public CVSListener createCVSListener(final PrintStream stdout, final PrintStream stderr) {
            return new locbundlecheck(stdout, stderr, loc, workDir);
        }

        public void setLocalization(final String loc) {
            this.loc = loc;
        }

        public void setWorkDir(final String dir) {
            workDir = dir;
        }

    }

}
