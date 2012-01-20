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
package org.netbeans.lib.cvsclient.command.log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.netbeans.lib.cvsclient.command.FileInfoContainer;
import org.netbeans.lib.cvsclient.util.BugLog;

/**
 * Describes log information for a file. This is the result of doing a cvs log
 * command. The fields in instances of this object are populated by response
 * handlers.
 * 
 * @author Milos Kleint
 */
public class LogInformation extends FileInfoContainer {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"); // NOI18N
    private File file;
    private String repositoryFilename;
    private String headRevision;
    private String branch;
    private String accessList;
    private String keywordSubstitution;
    private String totalRevisions;
    private String selectedRevisions;
    private String description;
    private String locks;
    private final List<Revision> revisions = new ArrayList<Revision>();
    private final List<SymName> symbolicNames = new ArrayList<SymName>();

    public LogInformation() {
    }

    /**
     * Getter for property file.
     * 
     * @return Value of property file.
     */
    @Override
    public File getFile() {
        return file;
    }

    /**
     * Setter for property file.
     * 
     * @param file
     *            New value of property file.
     */
    public void setFile(final File file) {
        this.file = file;
    }

    /**
     * Getter for property repositoryFilename.
     * 
     * @return Value of property repositoryFilename.
     */
    public String getRepositoryFilename() {
        return repositoryFilename;
    }

    /**
     * Setter for property repositoryFilename.
     * 
     * @param repositoryFilename
     *            New value of property repositoryFilename.
     */
    public void setRepositoryFilename(final String repositoryFilename) {
        this.repositoryFilename = repositoryFilename;
    }

    /**
     * Getter for property headRevision.
     * 
     * @return Value of property headRevision.
     */
    public String getHeadRevision() {
        return headRevision;
    }

    /**
     * Setter for property headRevision.
     * 
     * @param headRevision
     *            New value of property headRevision.
     */
    public void setHeadRevision(final String headRevision) {
        this.headRevision = headRevision;
    }

    /**
     * Getter for property branch.
     * 
     * @return Value of property branch.
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Setter for property branch.
     * 
     * @param branch
     *            New value of property branch.
     */
    public void setBranch(final String branch) {
        this.branch = branch;
    }

    /**
     * Getter for property accessList.
     * 
     * @return Value of property accessList.
     */
    public String getAccessList() {
        return accessList;
    }

    /**
     * Setter for property accessList.
     * 
     * @param accessList
     *            New value of property accessList.
     */
    public void setAccessList(final String accessList) {
        this.accessList = accessList;
    }

    /**
     * Getter for property keywordSubstitution.
     * 
     * @return Value of property keywordSubstitution.
     */
    public String getKeywordSubstitution() {
        return keywordSubstitution;
    }

    /**
     * Setter for property keywordSubstitution.
     * 
     * @param keywordSubstitution
     *            New value of property keywordSubstitution.
     */
    public void setKeywordSubstitution(final String keywordSubstitution) {
        this.keywordSubstitution = keywordSubstitution;
    }

    /**
     * Getter for property totalRevisions.
     * 
     * @return Value of property totalRevisions.
     */
    public String getTotalRevisions() {
        return totalRevisions;
    }

    /**
     * Setter for property totalRevisions.
     * 
     * @param totalRevisions
     *            New value of property totalRevisions.
     */
    public void setTotalRevisions(final String totalRevisions) {
        this.totalRevisions = totalRevisions;
    }

    /**
     * Getter for property selectedRevisions.
     * 
     * @return Value of property selectedRevisions.
     */
    public String getSelectedRevisions() {
        return selectedRevisions;
    }

    /**
     * Setter for property selectedRevisions.
     * 
     * @param selectedRevisions
     *            New value of property selectedRevisions.
     */
    public void setSelectedRevisions(final String selectedRevisions) {
        this.selectedRevisions = selectedRevisions;
    }

    /**
     * Getter for property description.
     * 
     * @return Value of property description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter for property description.
     * 
     * @param description
     *            New value of property description.
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Getter for property locks.
     * 
     * @return Value of property locks.
     */
    public String getLocks() {
        return locks;
    }

    /**
     * Setter for property locks.
     * 
     * @param locks
     *            New value of property locks.
     */
    public void setLocks(final String locks) {
        this.locks = locks;
    }

    /**
     * adds a revision info to the LogInformation instance
     */

    public void addRevision(final LogInformation.Revision newRevision) {
        revisions.add(newRevision);
    }

    /**
     * return the all revisions attached to this log (if more sophisticated
     * method are supplied, this might get obsolete)
     */
    public List<Revision> getRevisionList() {
        return revisions;
    }

    /**
     * Search the revisions by number of revision. If not found, return null.
     */
    public LogInformation.Revision getRevision(final String number) {
        final Iterator<Revision> it = revisions.iterator();
        LogInformation.Revision item;
        while (it.hasNext()) {
            item = it.next();
            if (item.getNumber().equals(number)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Add a symbolic name to the list of names and attaches it to a revision
     * number.
     */
    public void addSymbolicName(final String symName, final String revisionNumber) {
        final SymName newName = new SymName();
        newName.setName(symName);
        newName.setRevision(revisionNumber);
        symbolicNames.add(newName);
    }

    public List<SymName> getAllSymbolicNames() {
        return symbolicNames;
    }

    /**
     * Search the symbolic names by number of revision. If not found, return
     * null.
     */
    public List<SymName> getSymNamesForRevision(final String revNumber) {
        final Iterator<SymName> it = symbolicNames.iterator();
        LogInformation.SymName item;
        final List<SymName> list = new LinkedList<SymName>();
        while (it.hasNext()) {
            item = it.next();
            if (item.getRevision().equals(revNumber)) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Search the symbolic names by name of tag (symbolic name). If not found,
     * return null.
     */
    public LogInformation.SymName getSymName(final String symName) {
        final Iterator<SymName> it = symbolicNames.iterator();
        LogInformation.SymName item;
        while (it.hasNext()) {
            item = it.next();
            if (item.getName().equals(symName)) {
                return item;
            }
        }
        return null;
    }

    public Revision createNewRevision(final String number) {
        final Revision rev = new Revision();
        rev.setNumber(number);
        return rev;
    }

    /**
     * Return a string representation of this object. Useful for debugging.
     */
    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer(30);
        buf.append("\nFile: " + ((file != null) ? file.getAbsolutePath() : "null")); // NOI18N
        buf.append("\nRepositoryFile: " + repositoryFilename); // NOI18N
        buf.append("\nHead revision: " + headRevision); // NOI18N
        return buf.toString();
    }

    public class SymName {
        private String name;
        private String revision;

        public SymName() {
        }

        public String getName() {
            return name;
        }

        public void setName(final String symName) {
            name = symName;
        }

        public void setRevision(final String rev) {
            revision = rev;
        }

        public String getRevision() {
            return revision;
        }

        /**
         * Determines if given name represents a branch tag test is based on
         * revision num parsing and looking for trailing <tt>0.#</tt> (1.1.0.2,
         * 1.2.4.5.0.6, ,..).
         */
        public final boolean isBranch() {
            boolean branch = false;
            final String[] nums = revision.split("\\.");
            if ((nums.length > 2) && ((nums.length % 2) == 0)) {
                final String lastButOne = nums[nums.length - 2];
                branch = "0".equals(lastButOne); // NOI18N
            }
            return branch;
        }
    }

    public class Revision {

        /**
         * The revision number.
         */
        private String number;
        /**
         * The parsed date.
         */
        private Date date;
        /**
         * The String representation of the date.
         */
        private String dateString;
        /**
         * The author of the revision.
         */
        private String author;
        /**
         * The state.
         */
        private String state;
        /**
         * The added/removed lines.
         */
        private String lines;
        /**
         * The commit ID, as generated and reported by some servers.
         */
        private String commitID;
        /**
         * The commit log-message.
         */
        private String message;
        /**
         * The branches for this revision.
         */
        private String branches;

        public Revision() {
            /**
             * Since these have to be initialized when correctly parsing the
             * command's output, then initializing them to empty strings is a
             * safety measure against bad parsing errors. what about backward
             * compatibility here??
             * 
             * state = ""; lines = ""; message = ""; branches = "";
             */
        }

        public LogInformation getLogInfoHeader() {
            return LogInformation.this;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(final String number) {
            this.number = number;
        }

        public Date getDate() {
            return date;
        }

        public String getDateString() {
            return dateString;
        }

        /**
         * @deprecated This method uses a static parser to parse dates which is
         *             not thread safe, use #setDate instead
         */
        @Deprecated
        public void setDateString(String dateString) {
            this.dateString = dateString;

            if (dateString == null) {
                date = null;
                return;
            }

            // Parse the date ...
            try {
                // some servers use dashes to separate date components, so
                // replace with slashes
                // also add a default GMT timezone at the end, if the server
                // already put one in this one will be ignored by the parser
                dateString = dateString.replace('/', '-') + " +0000";
                date = DATE_FORMAT.parse(dateString);
            } catch (final Exception ex) {
                BugLog.getInstance().bug("Couldn't parse date " + dateString);
            }
        }

        public void setDate(final Date date, final String dateString) {
            this.dateString = dateString;
            this.date = date;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(final String author) {
            this.author = author;
        }

        public String getState() {
            return state;
        }

        public void setState(final String state) {
            this.state = state;
        }

        public String getLines() {
            return lines;
        }

        public void setLines(final String lines) {
            this.lines = lines;
        }

        public String getCommitID() {
            return commitID;
        }

        public void setCommitID(final String commitID) {
            this.commitID = commitID;
        }

        /**
         * Returns how many lines were added in this revision.
         */
        public int getAddedLines() {
            if (lines != null) {
                final int start = lines.indexOf('+');
                final int end = lines.indexOf(' ');
                if ((start >= 0) && (end > start)) {
                    final String added = lines.substring(start + 1, end);
                    try {
                        final int toReturn = Integer.parseInt(added);
                        return toReturn;
                    } catch (final NumberFormatException exc) {
                        // TODO BUGLog..
                    }
                }
            }
            return 0;
        }

        public int getRemovedLines() {
            if (lines != null) {
                final int start = lines.indexOf('-');
                if (start >= 0) {
                    final String removed = lines.substring(start + 1);
                    try {
                        final int toReturn = Integer.parseInt(removed);
                        return toReturn;
                    } catch (final NumberFormatException exc) {
                        // TODO BUGLog..
                    }
                }
            }
            return 0;

        }

        public String getMessage() {
            return message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }

        public String getBranches() {
            return branches;
        }

        public void setBranches(final String branches) {
            this.branches = branches;
        }
    }
}
