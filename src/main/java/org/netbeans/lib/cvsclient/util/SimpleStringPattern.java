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
 * The Initial Developer of the Original Software is Thomas Singer.
 * Portions created by Thomas Singer Copyright (C) 2001.
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
 *
 * Contributor(s): Thomas Singer, Milos Kleint
 *****************************************************************************/
package org.netbeans.lib.cvsclient.util;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Thomas Singer
 */
public class SimpleStringPattern implements StringPattern {

    private static final char MATCH_EACH = '*';
    private static final char MATCH_ONE = '?';

    private final List<SubPattern> subPatterns = new LinkedList<SubPattern>();

    /**
     * Creates a SimpleStringPattern for the specified definition. The
     * definition might contain two special characters ('*' and '?').
     */
    public SimpleStringPattern(final String definition) {
        splitInSubPattern(definition);
    }

    /**
     * Returns whether the specified string matches thiz pattern.
     */
    public boolean doesMatch(final String string) {
        int index = 0;
        SubPattern subPattern = null;
        for (final SubPattern subPattern2 : subPatterns) {
            subPattern = subPattern2;
            index = subPattern.doesMatch(string, index);
            if (index < 0) {
                return false;
            }
        }

        if (index == string.length()) {
            return true;
        }
        if (subPattern == null) {
            return false;
        }
        return subPattern.checkEnding(string, index);
    }

    private void splitInSubPattern(final String definition) {
        char prevSubPattern = ' ';

        int prevIndex = 0;
        for (int index = 0; index >= 0;) {
            prevIndex = index;

            index = definition.indexOf(MATCH_EACH, prevIndex);
            if (index >= 0) {
                final String match = definition.substring(prevIndex, index);
                addSubPattern(match, prevSubPattern);
                prevSubPattern = MATCH_EACH;
                index++;
                continue;
            }
            index = definition.indexOf(MATCH_ONE, prevIndex);
            if (index >= 0) {
                final String match = definition.substring(prevIndex, index);
                addSubPattern(match, prevSubPattern);
                prevSubPattern = MATCH_ONE;
                index++;
                continue;
            }
        }
        final String match = definition.substring(prevIndex);
        addSubPattern(match, prevSubPattern);
    }

    private void addSubPattern(final String match, final char subPatternMode) {
        SubPattern subPattern = null;
        switch (subPatternMode) {
        case MATCH_EACH:
            subPattern = new MatchEachCharPattern(match);
            break;
        case MATCH_ONE:
            subPattern = new MatchOneCharPattern(match);
            break;
        default:
            subPattern = new MatchExactSubPattern(match);
            break;
        }

        subPatterns.add(subPattern);
    }

    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        for (final SubPattern subPattern2 : subPatterns) {
            final SubPattern subPattern = subPattern2;
            buffer.append(subPattern.toString());
        }
        return buffer.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof SimpleStringPattern)) {
            return false;
        }
        return subPatterns.equals(((SimpleStringPattern) obj).subPatterns);
    }

    @Override
    public int hashCode() {
        return -subPatterns.hashCode();
    }

    public static void main(final String[] arguments) {
        StringPattern sp = new SimpleStringPattern("a*b"); // NOI18N

        test(sp, "ab", true); // NOI18N
        test(sp, "aab", true); // NOI18N
        test(sp, "ba", false); // NOI18N
        test(sp, "abc", false); // NOI18N

        sp = new SimpleStringPattern("*.txt"); // NOI18N

        test(sp, "datei.txt", true); // NOI18N
        test(sp, ".txt", true); // NOI18N
        test(sp, "datei.tx", false); // NOI18N
        test(sp, "datei.txt.txt", true); // NOI18N

        sp = new SimpleStringPattern("datei*1*"); // NOI18N

        test(sp, "datei0.txt", false); // NOI18N
        test(sp, "datei1.txt", true); // NOI18N
        test(sp, "datei.tx", false); // NOI18N
        test(sp, "datei1.txt.txt", true); // NOI18N

        sp = new SimpleStringPattern("Makefile"); // NOI18N

        test(sp, "Makefile", true); // NOI18N
        test(sp, "Makefile.mak", false); // NOI18N
        test(sp, "Makefile1", false); // NOI18N
        test(sp, ".Makefile", false); // NOI18N
        test(sp, ".Makefile.", false); // NOI18N

        sp = new SimpleStringPattern("*~"); // NOI18N

        test(sp, "datei~", true); // NOI18N
        test(sp, "datei~1", false); // NOI18N
        test(sp, "datei~1~", true); // NOI18N

        // Equality:
        SimpleStringPattern pattern1 = new SimpleStringPattern("*.class");
        SimpleStringPattern pattern2 = new SimpleStringPattern("*.class");
        System.err.println(pattern1 + ".equals(" + pattern2 + ") = " + pattern1.equals(pattern2));

        pattern1 = new SimpleStringPattern("?.class");
        pattern2 = new SimpleStringPattern("*.class");
        System.err.println(pattern1 + ".equals(" + pattern2 + ") = " + pattern1.equals(pattern2));

        pattern1 = new SimpleStringPattern("*.clazz");
        pattern2 = new SimpleStringPattern("*.class");
        System.err.println(pattern1 + ".equals(" + pattern2 + ") = " + pattern1.equals(pattern2));
    }

    private static void test(final StringPattern sp, final String testString, final boolean shouldResult) {
        System.err.print('"' + sp.toString() + '"' + ": " + testString + " " + shouldResult); // NOI18N

        final boolean doesMatch = sp.doesMatch(testString);

        if (doesMatch == shouldResult) {
            System.err.println(" proved"); // NOI18N
        } else {
            System.err.println(" **denied**"); // NOI18N
        }
    }

    private static abstract class SubPattern {
        protected final String match;

        protected SubPattern(final String match) {
            this.match = match;
        }

        /**
         * @parameter string ... the whole string to test for matching
         * @parameter index ... the index in string where this' test should
         *            begin
         * @returns ... if successful the next test-position, if not -1
         */
        public abstract int doesMatch(String string, int index);

        public boolean checkEnding(final String string, final int index) {
            return false;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(this.getClass().isInstance(obj))) {
                return false;
            }
            return match.equals(((SubPattern) obj).match);
        }

        @Override
        public int hashCode() {
            return -match.hashCode();
        }
    }

    private static class MatchExactSubPattern extends SubPattern {
        public MatchExactSubPattern(final String match) {
            super(match);
        }

        @Override
        public int doesMatch(final String string, final int index) {
            if (!string.startsWith(match, index)) {
                return -1;
            }
            return index + match.length();
        }

        @Override
        public String toString() {
            return match;
        }
    }

    private static class MatchEachCharPattern extends SubPattern {
        public MatchEachCharPattern(final String match) {
            super(match);
        }

        @Override
        public int doesMatch(final String string, final int index) {
            final int matchIndex = string.indexOf(match, index);
            if (matchIndex < 0) {
                return -1;
            }
            return matchIndex + match.length();
        }

        @Override
        public boolean checkEnding(final String string, final int index) {
            return string.endsWith(match);
        }

        @Override
        public String toString() {
            return MATCH_EACH + match;
        }
    }

    private static class MatchOneCharPattern extends MatchExactSubPattern {
        public MatchOneCharPattern(final String match) {
            super(match);
        }

        @Override
        public int doesMatch(final String string, int index) {
            index++;
            if (string.length() < index) {
                return -1;
            }
            return super.doesMatch(string, index);
        }

        @Override
        public String toString() {
            return MATCH_ONE + match;
        }
    }
}
