package com.gmail.heagoo.neweditor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ParserRule {
    public static final int ACTION_HINTS = 65280;
    public static final int AT_LINE_START = 2;
    public static final int AT_WHITESPACE_END = 4;
    public static final int AT_WORD_START = 8;
    public static final int END_REGEXP = 16384;
    public static final int EOL_SPAN = 16;
    public static final int IS_ESCAPE = 2048;
    public static final int MAJOR_ACTIONS = 255;
    public static final int MARK_FOLLOWING = 8;
    public static final int MARK_PREVIOUS = 4;
    public static final byte MATCH_TYPE_CONTEXT = -1;
    public static final byte MATCH_TYPE_RULE = -2;
    public static final int NO_LINE_BREAK = 512;
    public static final int NO_WORD_BREAK = 1024;
    public static final int REGEXP = 8192;
    public static final int SEQ = 0;
    public static final int SPAN = 2;
    public final int action;
    public ParserRuleSet delegate;
    public final char[] end;
    public final int endPosMatch;
    public final Pattern endRegexp;
    public final ParserRule escapeRule;
    public final byte matchType;
    public final char[] start;
    public final int startPosMatch;
    public final Pattern startRegexp;
    public final byte token;
    public final char[] upHashChar;
    public final char[] upHashChars;

    private ParserRule(int action, String hashChar, int startPosMatch, char[] start, Pattern startRegexp, int endPosMatch, char[] end, Pattern endRegexp, ParserRuleSet delegate, byte token, byte matchType, String escape) {
        char[] charArray;
        ParserRule parserRule = null;
        this.action = action;
        if (hashChar == null) {
            charArray = null;
        } else {
            charArray = hashChar.toUpperCase().toCharArray();
        }
        this.upHashChar = charArray;
        this.upHashChars = null;
        this.startPosMatch = startPosMatch;
        this.start = start;
        this.startRegexp = startRegexp;
        this.endPosMatch = endPosMatch;
        this.end = end;
        this.endRegexp = endRegexp;
        this.delegate = delegate;
        this.token = token;
        this.matchType = matchType;
        if (escape != null && escape.length() > 0) {
            parserRule = createEscapeRule(escape);
        }
        this.escapeRule = parserRule;
        if (this.delegate == null && (action & MAJOR_ACTIONS) != 0) {
            this.delegate = ParserRuleSet.getStandardRuleSet(token);
        }
    }

    private ParserRule(char[] hashChars, int action, int startPosMatch, char[] start, Pattern startRegexp, int endPosMatch, char[] end, Pattern endRegexp, ParserRuleSet delegate, byte token, byte matchType, String escape) {
        this.action = action;
        ParserRule createEscapeRule = null;
        this.upHashChar = null;
        Set<Character> hashCharsSet = new HashSet<>();
        for (char c : hashChars) {
            hashCharsSet.add(Character.valueOf(Character.toUpperCase(c)));
        }
        int i = hashCharsSet.size();
        this.upHashChars = new char[i];
        int i2 = 0;
        for (Character c2 : hashCharsSet) {
            int i3 = i2 + 1;
            this.upHashChars[i2] = c2.charValue();
            i2 = i3;
        }
        Arrays.sort(this.upHashChars);
        this.startPosMatch = startPosMatch;
        this.start = start;
        this.startRegexp = startRegexp;
        this.endPosMatch = endPosMatch;
        this.end = end;
        this.endRegexp = endRegexp;
        this.delegate = delegate;
        this.token = token;
        this.matchType = matchType;
        if (escape != null && escape.length() > 0) {
            createEscapeRule = createEscapeRule(escape);
        }
        this.escapeRule = createEscapeRule;
        if (this.delegate == null && (action & MAJOR_ACTIONS) != 0) {
            this.delegate = ParserRuleSet.getStandardRuleSet(token);
        }
    }

    public static ParserRule createSequenceRule(int posMatch, String seq, ParserRuleSet delegate, byte id) {
        return new ParserRule(0, seq.substring(0, 1), posMatch, seq.toCharArray(), (Pattern) null, 0, (char[]) null, (Pattern) null, delegate, id, (byte) -1, (String) null);
    }

    public static ParserRule createRegexpSequenceRule(String hashChar, int posMatch, String seq, ParserRuleSet delegate, byte id, boolean ignoreCase) throws PatternSyntaxException {
        return new ParserRule(REGEXP, hashChar, posMatch, (char[]) null, Pattern.compile(seq, ignoreCase ? 2 : 0), 0, (char[]) null, (Pattern) null, delegate, id, (byte) -1, (String) null);
    }

    public static ParserRule createRegexpSequenceRule(int posMatch, char[] hashChars, String seq, ParserRuleSet delegate, byte id, boolean ignoreCase) throws PatternSyntaxException {
        return new ParserRule(hashChars, REGEXP, posMatch, (char[]) null, Pattern.compile(seq, ignoreCase ? 2 : 0), 0, (char[]) null, (Pattern) null, delegate, id, (byte) -1, (String) null);
    }

    public static ParserRule createSpanRule(int startPosMatch, String start, int endPosMatch, String end, ParserRuleSet delegate, byte id, byte matchType, boolean noLineBreak, boolean noWordBreak, String escape) {
        return new ParserRule((noLineBreak ? NO_LINE_BREAK : 0) | 2 | (noWordBreak ? NO_WORD_BREAK : 0), start.substring(0, 1), startPosMatch, start.toCharArray(), (Pattern) null, endPosMatch, end.toCharArray(), (Pattern) null, delegate, id, matchType, escape);
    }

    public static ParserRule createRegexpSpanRule(String hashChar, int startPosMatch, String start, int endPosMatch, String end, ParserRuleSet delegate, byte id, byte matchType, boolean noLineBreak, boolean noWordBreak, boolean ignoreCase, String escape, boolean endRegexp) throws PatternSyntaxException {
        int ruleAction;
        Pattern endRegexpPattern;
        char[] endArray;
        int i;
        int i2 = 0;
        int ruleAction2 = (noLineBreak ? NO_LINE_BREAK : 0) | 8194 | (noWordBreak ? NO_WORD_BREAK : 0);
        if (endRegexp) {
            int ruleAction3 = ruleAction2 | END_REGEXP;
            if (!ignoreCase) {
                i = 0;
            } else {
                i = 2;
            }
            Pattern endRegexpPattern2 = Pattern.compile(end, i);
            ruleAction = ruleAction3;
            endRegexpPattern = endRegexpPattern2;
            endArray = null;
        } else {
            ruleAction = ruleAction2;
            endRegexpPattern = null;
            endArray = end.toCharArray();
        }
        if (ignoreCase) {
            i2 = 2;
        }
        return new ParserRule(ruleAction, hashChar, startPosMatch, (char[]) null, Pattern.compile(start, i2), endPosMatch, endArray, endRegexpPattern, delegate, id, matchType, escape);
    }

    public static ParserRule createRegexpSpanRule(int startPosMatch, char[] hashChars, String start, int endPosMatch, String end, ParserRuleSet delegate, byte id, byte matchType, boolean noLineBreak, boolean noWordBreak, boolean ignoreCase, String escape, boolean endRegexp) throws PatternSyntaxException {
        int ruleAction;
        Pattern endRegexpPattern;
        char[] endArray;
        int i;
        int i2 = 0;
        int ruleAction2 = (noLineBreak ? NO_LINE_BREAK : 0) | 8194 | (noWordBreak ? NO_WORD_BREAK : 0);
        if (endRegexp) {
            int ruleAction3 = ruleAction2 | END_REGEXP;
            if (!ignoreCase) {
                i = 0;
            } else {
                i = 2;
            }
            Pattern endRegexpPattern2 = Pattern.compile(end, i);
            ruleAction = ruleAction3;
            endRegexpPattern = endRegexpPattern2;
            endArray = null;
        } else {
            ruleAction = ruleAction2;
            endRegexpPattern = null;
            endArray = end.toCharArray();
        }
        if (ignoreCase) {
            i2 = 2;
        }
        return new ParserRule(hashChars, ruleAction, startPosMatch, (char[]) null, Pattern.compile(start, i2), endPosMatch, endArray, endRegexpPattern, delegate, id, matchType, escape);
    }

    public static ParserRule createEOLSpanRule(int posMatch, String seq, ParserRuleSet delegate, byte id, byte matchType) {
        return new ParserRule(528, seq.substring(0, 1), posMatch, seq.toCharArray(), (Pattern) null, 0, (char[]) null, (Pattern) null, delegate, id, matchType, (String) null);
    }

    public static ParserRule createRegexpEOLSpanRule(String hashChar, int posMatch, String seq, ParserRuleSet delegate, byte id, byte matchType, boolean ignoreCase) throws PatternSyntaxException {
        return new ParserRule(8720, hashChar, posMatch, (char[]) null, Pattern.compile(seq, ignoreCase ? 2 : 0), 0, (char[]) null, (Pattern) null, delegate, id, matchType, (String) null);
    }

    public static ParserRule createRegexpEOLSpanRule(int posMatch, char[] hashChars, String seq, ParserRuleSet delegate, byte id, byte matchType, boolean ignoreCase) throws PatternSyntaxException {
        return new ParserRule(hashChars, 8720, posMatch, (char[]) null, Pattern.compile(seq, ignoreCase ? 2 : 0), 0, (char[]) null, (Pattern) null, delegate, id, matchType, (String) null);
    }

    public static ParserRule createMarkFollowingRule(int posMatch, String seq, byte id, byte matchType) {
        return new ParserRule(8, seq.substring(0, 1), posMatch, seq.toCharArray(), (Pattern) null, 0, (char[]) null, (Pattern) null, (ParserRuleSet) null, id, matchType, (String) null);
    }

    public static ParserRule createMarkPreviousRule(int posMatch, String seq, byte id, byte matchType) {
        return new ParserRule(4, seq.substring(0, 1), posMatch, seq.toCharArray(), (Pattern) null, 0, (char[]) null, (Pattern) null, (ParserRuleSet) null, id, matchType, (String) null);
    }

    public static ParserRule createEscapeRule(String seq) {
        return new ParserRule(IS_ESCAPE, seq.substring(0, 1), 0, seq.toCharArray(), (Pattern) null, 0, (char[]) null, (Pattern) null, (ParserRuleSet) null, (byte) 0, (byte) -1, (String) null);
    }

    public String toString() {
        String tokenToString;
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8;
        boolean z9;
        String str = null;
        boolean z22 = true;
        StringBuilder result = new StringBuilder();
        result.append(getClass().getName()).append("[action=");
        switch (this.action & MAJOR_ACTIONS) {
            case 0:
                result.append("SEQ");
                break;
            case 2:
                result.append("SPAN");
                break;
            case 4:
                result.append("MARK_PREVIOUS");
                break;
            case 8:
                result.append("MARK_FOLLOWING");
                break;
            case 16:
                result.append("EOL_SPAN");
                break;
            default:
                result.append("UNKNOWN");
                break;
        }
        int actionHints = this.action & ACTION_HINTS;
        StringBuilder append = result.append("[matchType=");
        if (this.matchType == -1) {
            tokenToString = "MATCH_TYPE_CONTEXT";
        } else {
            tokenToString = this.matchType == -2 ? "MATCH_TYPE_RULE" : Token.tokenToString(this.matchType);
        }
        append.append(tokenToString);
        StringBuilder append2 = result.append(",NO_LINE_BREAK=");
        if ((actionHints & NO_LINE_BREAK) != 0) {
            z = true;
        } else {
            z = false;
        }
        append2.append(z);
        StringBuilder append3 = result.append(",NO_WORD_BREAK=");
        if ((actionHints & NO_WORD_BREAK) != 0) {
            z2 = true;
        } else {
            z2 = false;
        }
        append3.append(z2);
        StringBuilder append4 = result.append(",IS_ESCAPE=");
        if ((actionHints & IS_ESCAPE) != 0) {
            z3 = true;
        } else {
            z3 = false;
        }
        append4.append(z3);
        StringBuilder append5 = result.append(",REGEXP=");
        if ((actionHints & REGEXP) != 0) {
            z4 = true;
        } else {
            z4 = false;
        }
        append5.append(z4);
        result.append("],upHashChar=").append(new String(this.upHashChar));
        result.append(",upHashChars=").append(Arrays.toString(this.upHashChars));
        result.append(",startPosMatch=");
        StringBuilder append6 = result.append("[AT_LINE_START=");
        if ((this.startPosMatch & 2) != 0) {
            z5 = true;
        } else {
            z5 = false;
        }
        append6.append(z5);
        StringBuilder append7 = result.append(",AT_WHITESPACE_END=");
        if ((this.startPosMatch & 4) != 0) {
            z6 = true;
        } else {
            z6 = false;
        }
        append7.append(z6);
        StringBuilder append8 = result.append(",AT_WORD_START=");
        if ((this.startPosMatch & 8) != 0) {
            z7 = true;
        } else {
            z7 = false;
        }
        append8.append(z7);
        result.append("],start=").append(this.start == null ? null : String.valueOf(this.start));
        result.append(",startRegexp=").append(this.startRegexp);
        result.append(",endPosMatch=");
        StringBuilder append9 = result.append("[AT_LINE_START=");
        if ((this.endPosMatch & 2) != 0) {
            z8 = true;
        } else {
            z8 = false;
        }
        append9.append(z8);
        StringBuilder append10 = result.append(",AT_WHITESPACE_END=");
        if ((this.endPosMatch & 4) != 0) {
            z9 = true;
        } else {
            z9 = false;
        }
        append10.append(z9);
        StringBuilder append22 = result.append(",AT_WORD_START=");
        if ((this.endPosMatch & 8) == 0) {
            z22 = false;
        }
        append22.append(z22);
        StringBuilder append23 = result.append("],end=");
        if (this.end != null) {
            str = String.valueOf(this.end);
        }
        append23.append(str);
        result.append(",delegate=").append(this.delegate);
        result.append(",escapeRule=").append(this.escapeRule);
        result.append(",token=").append(Token.tokenToString(this.token)).append(']');
        return result.toString();
    }
}
