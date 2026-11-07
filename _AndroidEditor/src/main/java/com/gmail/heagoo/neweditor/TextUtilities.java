package com.gmail.heagoo.neweditor;

import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

public class TextUtilities {
    public static final int BRACKET_MATCH_LIMIT = 10000;
    public static final int LOWER_CASE = 1;
    public static final int MIXED = 0;
    public static final int SYMBOL = 2;
    public static final int TITLE_CASE = 3;
    public static final int UPPER_CASE = 2;
    public static final int WHITESPACE = 0;
    public static final int WORD_CHAR = 1;

    public static Token getTokenAtOffset(Token tokens, int offset) {
        if (offset != 0 || tokens.id != Byte.MAX_VALUE) {
            while (tokens.id != Byte.MAX_VALUE) {
                if (tokens.offset + tokens.length <= offset) {
                    tokens = tokens.next;
                }
            }
            throw new ArrayIndexOutOfBoundsException("offset > line length");
        }
        return tokens;
    }

    public static char getComplementaryBracket(char ch, boolean[] direction) {
        switch (ch) {
            case '(':
                if (direction != null) {
                    direction[0] = true;
                    return ')';
                }
                return ')';
            case ')':
                if (direction != null) {
                    direction[0] = false;
                    return '(';
                }
                return '(';
            case '<':
                if (direction != null) {
                    direction[0] = true;
                    return '>';
                }
                return '>';
            case '>':
                if (direction != null) {
                    direction[0] = false;
                    return '<';
                }
                return '<';
            case '[':
                if (direction != null) {
                    direction[0] = true;
                    return ']';
                }
                return ']';
            case ']':
                if (direction != null) {
                    direction[0] = false;
                    return '[';
                }
                return '[';
            case '{':
                if (direction != null) {
                    direction[0] = true;
                    return '}';
                }
                return '}';
            case '}':
                if (direction != null) {
                    direction[0] = false;
                    return '{';
                }
                return '{';
            default:
                return (char) 0;
        }
    }

    public static String join(Collection<?> c, String delim) {
        StringBuilder retval = new StringBuilder();
        Iterator<?> itr = c.iterator();
        if (!itr.hasNext()) {
            return "";
        }
        retval.append(itr.next());
        while (itr.hasNext()) {
            retval.append(delim);
            retval.append(itr.next());
        }
        return retval.toString();
    }

    public static int findWordStart(String line, int pos, String noWordSep) {
        return findWordStart(line, pos, noWordSep, true, false);
    }

    public static int findWordStart(CharSequence line, int pos, String noWordSep) {
        return findWordStart(line, pos, noWordSep, true, false, false);
    }

    public static int findWordStart(String line, int pos, String noWordSep, boolean joinNonWordChars) {
        return findWordStart(line, pos, noWordSep, joinNonWordChars, false);
    }

    public static int findWordStart(String line, int pos, String noWordSep, boolean joinNonWordChars, boolean eatWhitespace) {
        return findWordStart(line, pos, noWordSep, joinNonWordChars, false, eatWhitespace);
    }

    public static int findWordStart(String line, int pos, String noWordSep, boolean joinNonWordChars, boolean camelCasedWords, boolean eatWhitespace) {
        return findWordStart((CharSequence) line, pos, noWordSep, joinNonWordChars, camelCasedWords, eatWhitespace);
    }

    public static int findWordStart(CharSequence line, int pos, String noWordSep, boolean joinNonWordChars, boolean camelCasedWords, boolean eatWhitespace) {
        return findWordStart(line, pos, noWordSep, joinNonWordChars, camelCasedWords, eatWhitespace, false);
    }

    public static int findWordStart(CharSequence line, int pos, String noWordSep, boolean joinNonWordChars, boolean camelCasedWords, boolean eatWhitespace, boolean eatOnlyAfterWord) {
        char ch = line.charAt(pos);
        if (noWordSep == null) {
            noWordSep = "";
        }
        int type = getCharType(ch, noWordSep);
        int i = pos;
        while (i >= 0) {
            char lastCh = ch;
            ch = line.charAt(i);
            switch (type) {
                case 0:
                    if (!Character.isWhitespace(ch)) {
                        if (!eatOnlyAfterWord) {
                            break;
                        } else {
                            if (!Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1) {
                                type = 2;
                            } else {
                                type = 1;
                            }
                            i--;
                        }
                    } else {
                        continue;
                    }
                case 1:
                    if (camelCasedWords && Character.isUpperCase(ch) && !Character.isUpperCase(lastCh) && Character.isLetterOrDigit(lastCh)) {
                        break;
                    } else if ((!camelCasedWords || Character.isUpperCase(ch) || !Character.isUpperCase(lastCh)) && !Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1) {
                        if (Character.isWhitespace(ch) && eatWhitespace && !eatOnlyAfterWord) {
                            type = 0;
                            i--;
                        }
                    }
                case 2:
                    if (!joinNonWordChars && pos != i) {
                        break;
                    } else {
                        if (!Character.isWhitespace(ch)) {
                            if (Character.isLetterOrDigit(ch) || noWordSep.indexOf(ch) != -1) {
                                break;
                            }
                        } else if (!eatWhitespace || eatOnlyAfterWord) {
                            break;
                        } else {
                            type = 0;
                        }
                        i--;
                    }
                    break;
                default:
                    i--;
            }
            return i + 1;
        }
        return 0;
    }

    public static int findWordEnd(String line, int pos, String noWordSep) {
        return findWordEnd(line, pos, noWordSep, true);
    }

    public static int findWordEnd(CharSequence line, int pos, String noWordSep) {
        return findWordEnd(line, pos, noWordSep, true, false, false);
    }

    public static int findWordEnd(String line, int pos, String noWordSep, boolean joinNonWordChars) {
        return findWordEnd(line, pos, noWordSep, joinNonWordChars, false);
    }

    public static int findWordEnd(String line, int pos, String noWordSep, boolean joinNonWordChars, boolean eatWhitespace) {
        return findWordEnd(line, pos, noWordSep, joinNonWordChars, false, eatWhitespace);
    }

    public static int findWordEnd(String line, int pos, String noWordSep, boolean joinNonWordChars, boolean camelCasedWords, boolean eatWhitespace) {
        return findWordEnd((CharSequence) line, pos, noWordSep, joinNonWordChars, camelCasedWords, eatWhitespace);
    }

    public static int findWordEnd(CharSequence line, int pos, String noWordSep, boolean joinNonWordChars, boolean camelCasedWords, boolean eatWhitespace) {
        if (pos != 0) {
            pos--;
        }
        char ch = line.charAt(pos);
        if (noWordSep == null) {
            noWordSep = "";
        }
        int type = getCharType(ch, noWordSep);
        for (int i = pos; i < line.length(); i++) {
            char lastCh = ch;
            ch = line.charAt(i);
            switch (type) {
                case 0:
                    if (!Character.isWhitespace(ch)) {
                        return i;
                    }
                case 1:
                    if (camelCasedWords && i > pos + 1 && !Character.isUpperCase(ch) && Character.isLetterOrDigit(ch) && Character.isUpperCase(lastCh)) {
                        return i - 1;
                    }
                    if ((!camelCasedWords || !Character.isUpperCase(ch) || Character.isUpperCase(lastCh)) && !Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1) {
                        if (Character.isWhitespace(ch) && eatWhitespace) {
                            type = 0;
                            break;
                        } else {
                            return i;
                        }
                    } else {
                        return i;
                    }
                case 2:
                    if (!joinNonWordChars && i != pos) {
                        return i;
                    }
                    if (Character.isWhitespace(ch)) {
                        if (eatWhitespace) {
                            type = 0;
                            break;
                        } else {
                            return i;
                        }
                    } else if (!Character.isLetterOrDigit(ch)) {
                        if (noWordSep.indexOf(ch) != -1) {
                            return i;
                        }
                        break;
                    } else {
                        return i;
                    }
            }
        }
        return line.length();
    }

    public static int getCharType(char ch, String noWordSep) {
        if (Character.isWhitespace(ch)) {
            return 0;
        }
        if (Character.isLetterOrDigit(ch) || noWordSep.indexOf(ch) != -1) {
            return 1;
        }
        return 2;
    }

    public static String tabsToSpaces(String in, int tabSize) {
        StringBuilder buf = new StringBuilder();
        int width = 0;
        for (int i = 0; i < in.length(); i++) {
            switch (in.charAt(i)) {
                case '\t':
                    int count = tabSize - (width % tabSize);
                    int i2 = width + count;
                    while (true) {
                        count--;
                        if (count < 0) {
                            break;
                        } else {
                            buf.append(' ');
                        }
                    }
                case '\n':
                    break;
                default:
                    width++;
                    buf.append(in.charAt(i));
                    continue;
            }
            width = 0;
            buf.append(in.charAt(i));
        }
        return buf.toString();
    }

    public static String format(String text, int maxLineLength, int tabSize) {
        StringBuilder buf = new StringBuilder();
        int index = 0;
        while (true) {
            int newIndex = text.indexOf("\n\n", index);
            if (newIndex == -1) {
                break;
            }
            formatParagraph(text.substring(index, newIndex), maxLineLength, tabSize, buf);
            buf.append("\n\n");
            index = newIndex + 2;
        }
        if (index != text.length()) {
            formatParagraph(text.substring(index), maxLineLength, tabSize, buf);
        }
        return buf.toString();
    }

    public static int indexIgnoringWhitespace(String str, int index) {
        int j = 0;
        for (int i = 0; i < index; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                j++;
            }
        }
        return j;
    }

    public static int ignoringWhitespaceIndex(String str, int index) {
        int j = 0;
        int i = 0;
        while (true) {
            if (!Character.isWhitespace(str.charAt(i))) {
                j++;
            }
            if (j > index) {
                return i;
            }
            if (i == str.length() - 1) {
                return i + 1;
            }
            i++;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:32:0x0046, code lost:
    
        continue;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static int getStringCase(CharSequence str) {
        // Restored manually in recovery stage 3 from the --show-bad-code dump.
        if (str.length() == 0) {
            return 0;
        }
        int state = -1;
        char ch = str.charAt(0);
        if (Character.isLetter(ch)) {
            state = Character.isUpperCase(ch) ? 2 : 1;
        }
        for (int i = 1; i < str.length(); i++) {
            char ch2 = str.charAt(i);
            if (Character.isLetter(ch2)) {
                switch (state) {
                    case 1:
                    case 3:
                        if (Character.isUpperCase(ch2)) {
                            return 0;
                        }
                    case 2:
                        if (Character.isLowerCase(ch2)) {
                            if (i != 1) {
                                return 0;
                            }
                            state = 3;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return state;
    }

    public static int getStringCase(String str) {
        return getStringCase((CharSequence) str);
    }

    public static String toTitleCase(String str) {
        return str.length() == 0 ? str : String.valueOf(Character.toUpperCase(str.charAt(0))) + str.substring(1).toLowerCase();
    }

    public static String escapeText(String text) {
        return "\\Q" + text.replace("\\E", "\\\\E") + "\\E";
    }

    private static void formatParagraph(String text, int maxLineLength, int tabSize, StringBuilder buf) {
        String leadingWhitespace = text.substring(0, StandardUtilities.getLeadingWhiteSpace(text));
        int leadingWhitespaceWidth = StandardUtilities.getLeadingWhiteSpaceWidth(text, tabSize);
        buf.append(leadingWhitespace);
        int lineLength = leadingWhitespaceWidth;
        StringTokenizer st = new StringTokenizer(text);
        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            if (lineLength != leadingWhitespaceWidth) {
                if (word.length() + lineLength + 1 > maxLineLength) {
                    buf.append('\n');
                    buf.append(leadingWhitespace);
                    lineLength = leadingWhitespaceWidth;
                } else {
                    buf.append(' ');
                    lineLength++;
                }
            }
            buf.append(word);
            lineLength += word.length();
        }
    }

    public static void indexIgnoringWhitespace(String text, int maxLineLength, int tabSize, StringBuffer buf) {
        String leadingWhitespace = text.substring(0, StandardUtilities.getLeadingWhiteSpace(text));
        int leadingWhitespaceWidth = StandardUtilities.getLeadingWhiteSpaceWidth(text, tabSize);
        buf.append(leadingWhitespace);
        int lineLength = leadingWhitespaceWidth;
        StringTokenizer st = new StringTokenizer(text);
        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            if (lineLength != leadingWhitespaceWidth) {
                if (word.length() + lineLength + 1 > maxLineLength) {
                    buf.append('\n');
                    buf.append(leadingWhitespace);
                    lineLength = leadingWhitespaceWidth;
                } else {
                    buf.append(' ');
                    lineLength++;
                }
            }
            buf.append(word);
            lineLength += word.length();
        }
    }
}
