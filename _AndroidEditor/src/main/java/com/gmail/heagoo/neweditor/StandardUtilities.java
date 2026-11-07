package com.gmail.heagoo.neweditor;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Stack;

public class StandardUtilities {
    public static final char DIR_SEPARATOR_WINDOWS = '\\';
    public static final DecimalFormat KB_FORMAT = new DecimalFormat("#.# kB");
    public static final DecimalFormat MB_FORMAT = new DecimalFormat("#.# MB");
    public static final char PACKAGE_SEPARATOR_CHAR = '.';

    private StandardUtilities() {
    }

    public static String charsToEscapes(String str) {
        return charsToEscapes(str, "\n\t\\\"'");
    }

    public static String charsToEscapes(String str, String toEscape) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (toEscape.indexOf(c) == -1) {
                buf.append(c);
            } else if (c == '\n') {
                buf.append("\\n");
            } else if (c == '\t') {
                buf.append("\\t");
            } else {
                buf.append(DIR_SEPARATOR_WINDOWS);
                buf.append(c);
            }
        }
        return buf.toString();
    }

    public static String getIndentString(String str) {
        StringBuilder indentString = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (!Character.isWhitespace(ch)) {
                break;
            }
            indentString.append(ch);
        }
        return indentString.toString();
    }

    public static int getLeadingWhiteSpace(String str) {
        return getLeadingWhiteSpace((CharSequence) str);
    }

    public static int getLeadingWhiteSpace(CharSequence str) {
        if (0 >= str.length()) {
            return 0;
        }
        switch (str.charAt(0)) {
            case '\t':
            case ' ':
                int whitespace = 0 + 1;
                return whitespace;
            default:
                return 0;
        }
    }

    public static int getTrailingWhiteSpace(String str) {
        int i = str.length() - 1;
        if (i < 0) {
            return 0;
        }
        switch (str.charAt(i)) {
            case '\t':
            case ' ':
                int whitespace = 0 + 1;
                int i2 = i - 1;
                return whitespace;
            default:
                return 0;
        }
    }

    public static int getLeadingWhiteSpaceWidth(String str, int tabSize) {
        return getLeadingWhiteSpaceWidth((CharSequence) str, tabSize);
    }

    public static int getLeadingWhiteSpaceWidth(CharSequence r3, int r4) {
        return 0;
    }

    public static String truncateWhiteSpace(int len, int tabSize, String indentStr) {
        StringBuilder buf = new StringBuilder();
        int indent = 0;
        for (int i = 0; indent < len && i < indentStr.length(); i++) {
            char c = indentStr.charAt(i);
            if (c == ' ') {
                indent++;
                buf.append(c);
            } else if (c == '\t') {
                int withTab = (indent + tabSize) - (indent % tabSize);
                if (withTab > len) {
                    while (indent < len) {
                        buf.append(' ');
                        indent++;
                    }
                } else {
                    indent = withTab;
                    buf.append(c);
                }
            }
        }
        return buf.toString();
    }

    public static int getVirtualWidth(Segment seg, int tabSize) {
        int virtualPosition = 0;
        for (int i = 0; i < seg.count; i++) {
            if (seg.array[seg.offset + i] == '\t') {
                virtualPosition += tabSize - (virtualPosition % tabSize);
            } else {
                virtualPosition++;
            }
        }
        return virtualPosition;
    }

    public static int getOffsetOfVirtualColumn(Segment seg, int tabSize, int column, int[] totalVirtualWidth) {
        int virtualPosition = 0;
        for (int i = 0; i < seg.count; i++) {
            if (seg.array[seg.offset + i] == '\t') {
                int tabWidth = tabSize - (virtualPosition % tabSize);
                if (virtualPosition >= column) {
                    return i;
                }
                virtualPosition += tabWidth;
            } else {
                if (virtualPosition >= column) {
                    return i;
                }
                virtualPosition++;
            }
        }
        if (totalVirtualWidth != null) {
            totalVirtualWidth[0] = virtualPosition;
            return -1;
        }
        return -1;
    }

    public static int compareStrings(String str1, String str2, boolean ignoreCase) {
        char[] char1 = str1.toCharArray();
        char[] char2 = str2.toCharArray();
        int len = Math.min(char1.length, char2.length);
        int i = 0;
        int j = 0;
        while (i < len && j < len) {
            char ch1 = char1[i];
            char ch2 = char2[j];
            if (!Character.isDigit(ch1) || !Character.isDigit(ch2) || ch1 == '0' || ch2 == '0') {
                if (ignoreCase) {
                    ch1 = Character.toLowerCase(ch1);
                    ch2 = Character.toLowerCase(ch2);
                }
                if (ch1 != ch2) {
                    return ch1 - ch2;
                }
            } else {
                int _i = i + 1;
                int _j = j + 1;
                while (_i < char1.length && Character.isDigit(char1[_i])) {
                    _i++;
                }
                while (_j < char2.length && Character.isDigit(char2[_j])) {
                    _j++;
                }
                int len1 = _i - i;
                int len2 = _j - j;
                if (len1 > len2) {
                    return 1;
                }
                if (len1 < len2) {
                    return -1;
                }
                for (int k = 0; k < len1; k++) {
                    char ch12 = char1[i + k];
                    char ch22 = char2[j + k];
                    if (ch12 != ch22) {
                        return ch12 - ch22;
                    }
                }
                i = _i - 1;
                j = _j - 1;
            }
            i++;
            j++;
        }
        return char1.length - char2.length;
    }

    public static boolean objectsEqual(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 != null) {
                return false;
            }
            return true;
        }
        if (o2 == null) {
            return false;
        }
        return o1.equals(o2);
    }

    public static String globToRE(String glob) {
        if (glob.startsWith("(re)")) {
            return glob.substring(4);
        }
        Object NEG = new Object();
        Object GROUP = new Object();
        Stack<Object> state = new Stack<>();
        StringBuilder buf = new StringBuilder();
        boolean backslash = false;
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (!backslash) {
                switch (c) {
                    case '(':
                    case ')':
                    case '+':
                    case '.':
                        buf.append(DIR_SEPARATOR_WINDOWS);
                        buf.append(c);
                        break;
                    case '*':
                        buf.append(".*");
                        break;
                    case ',':
                        if (!state.isEmpty() && state.peek() == GROUP) {
                            buf.append('|');
                            break;
                        } else {
                            buf.append(',');
                            break;
                        }
                    case '?':
                        buf.append(PACKAGE_SEPARATOR_CHAR);
                        break;
                    case '\\':
                        break;
                    case '{':
                        buf.append('(');
                        if (i + 1 != glob.length() && glob.charAt(i + 1) == '!') {
                            buf.append('?');
                            state.push(NEG);
                            break;
                        } else {
                            state.push(GROUP);
                            break;
                        }
                    case '|':
                        if (!backslash) {
                            buf.append('|');
                            break;
                        } else {
                            buf.append("\\|");
                            break;
                        }
                    case '}':
                        if (!state.isEmpty()) {
                            buf.append(')');
                            if (state.pop() != NEG) {
                                break;
                            } else {
                                buf.append(".*");
                                break;
                            }
                        } else {
                            buf.append('}');
                            break;
                        }
                    default:
                        buf.append(c);
                        break;
                }
            }
            buf.append(DIR_SEPARATOR_WINDOWS);
            buf.append(c);
            backslash = false;
        }
        return buf.toString();
    }

    public static boolean regionMatches(CharSequence seq, int toff, CharSequence other, int ooff, int len) {
        if (toff < 0 || ooff < 0 || len < 0) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (i + toff >= seq.length()) {
                return false;
            }
            char c1 = seq.charAt(i + toff);
            if (i + ooff >= other.length() || c1 != other.charAt(i + ooff)) {
                return false;
            }
        }
        return true;
    }

    public static boolean startsWith(CharSequence seq, String str) {
        for (int i = 0; i < str.length(); i++) {
            if (i >= seq.length() || seq.charAt(i) != str.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean getBoolean(Object obj, boolean def) {
        if (obj == null) {
            return def;
        }
        if (obj instanceof Boolean) {
            return ((Boolean) obj).booleanValue();
        }
        if ("true".equals(obj) || "yes".equals(obj) || "on".equals(obj)) {
            return true;
        }
        if ("false".equals(obj) || "no".equals(obj) || "off".equals(obj)) {
            return false;
        }
        return def;
    }

    public static String formatFileSize(long length) {
        if (length < 1024) {
            return String.valueOf(length) + " Bytes";
        }
        if (length < 1048576) {
            return KB_FORMAT.format(length / 1024.0d);
        }
        return MB_FORMAT.format((length / 1024.0d) / 1024.0d);
    }

    public static class StringCompare<E> implements Comparator<E> {
        private boolean icase;

        public StringCompare(boolean icase) {
            this.icase = icase;
        }

        @Override
        public int compare(E obj1, E obj2) {
            return StandardUtilities.compareStrings(obj1.toString(), obj2.toString(), this.icase);
        }
    }
}
