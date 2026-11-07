package com.gmail.heagoo.neweditor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenMarker {
    static final boolean $assertionsDisabled = false;
    private LineContext context;
    private KeywordMap keywords;
    private int lastOffset;
    private Segment line;
    private int lineLength;
    private ParserRuleSet mainRuleSet;
    private int pos;
    private boolean seenWhitespaceEnd;
    private TokenHandler tokenHandler;
    private int whitespaceEnd;
    private final Map<String, ParserRuleSet> ruleSets = new Hashtable(64);
    private final Segment pattern = new Segment();

    private static char[] substitute(Matcher match, char[] end) {
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < end.length) {
            char ch = end[i];
            if (ch == '$' || ch == '~') {
                if (i == end.length - 1) {
                    buf.append(ch);
                } else {
                    char digit = end[i + 1];
                    if (!Character.isDigit(digit)) {
                        buf.append(ch);
                    } else if (ch == '$') {
                        buf.append(match.group(digit - '0'));
                        i++;
                    } else {
                        String s = match.group(digit - '0');
                        if (s.length() == 1) {
                            char b = TextUtilities.getComplementaryBracket(s.charAt(0), null);
                            if (b == 0) {
                                b = s.charAt(0);
                            }
                            buf.append(b);
                        } else {
                            buf.append(ch);
                        }
                        i++;
                    }
                }
            } else {
                buf.append(ch);
            }
            i++;
        }
        int i2 = buf.length();
        char[] returnValue = new char[i2];
        buf.getChars(0, buf.length(), returnValue, 0);
        return returnValue;
    }

    public void addRuleSet(ParserRuleSet rules) {
        this.ruleSets.put(rules.getSetName(), rules);
        if (rules.getSetName().equals("MAIN")) {
            this.mainRuleSet = rules;
        }
    }

    public ParserRuleSet getMainRuleSet() {
        return this.mainRuleSet;
    }

    public ParserRuleSet getRuleSet(String setName) {
        return this.ruleSets.get(setName);
    }

    public ParserRuleSet[] getRuleSets() {
        return (ParserRuleSet[]) this.ruleSets.values().toArray(new ParserRuleSet[this.ruleSets.size()]);
    }

    public synchronized LineContext markTokens(LineContext prevContext, TokenHandler tokenHandler, Segment line) {
        ParserRule rule;
        this.tokenHandler = tokenHandler;
        this.line = line;
        this.lastOffset = line.offset;
        this.lineLength = line.count + line.offset;
        this.context = new LineContext();
        if (prevContext == null) {
            this.context.rules = getMainRuleSet();
            this.context.escapeRule = this.context.rules.getEscapeRule();
        } else {
            this.context.parent = prevContext.parent;
            this.context.setInRule(prevContext.inRule);
            this.context.rules = prevContext.rules;
            this.context.spanEndSubst = prevContext.spanEndSubst;
        }
        this.keywords = this.context.rules.getKeywords();
        this.seenWhitespaceEnd = false;
        this.whitespaceEnd = line.offset;
        int terminateChar = this.context.rules.getTerminateChar();
        this.pos = line.offset;
        boolean terminated = false;
        while (this.pos < this.lineLength) {
            if (terminateChar >= 0 && this.pos - line.offset >= terminateChar && !terminated) {
                this.context = new LineContext(ParserRuleSet.getStandardRuleSet(this.context.rules.getDefault()), this.context);
                this.keywords = this.context.rules.getKeywords();
                terminated = true;
            }
            if (this.context.escapeRule == null || !handleRuleStart(this.context.escapeRule)) {
                if (this.context.parent != null && this.context.parent.inRule != null && checkDelegateEnd(this.context.parent.inRule)) {
                    this.seenWhitespaceEnd = true;
                } else {
                    Character ch = Character.valueOf(line.array[this.pos]);
                    List<ParserRule> rules = this.context.rules.getRules(ch);
                    Iterator<ParserRule> it = rules.iterator();
                    while (true) {
                        if (it.hasNext()) {
                            if (handleRuleStart(it.next())) {
                                this.seenWhitespaceEnd = true;
                                break;
                            }
                        } else if (Character.isWhitespace(ch.charValue())) {
                            if (!this.seenWhitespaceEnd) {
                                this.whitespaceEnd = this.pos + 1;
                            }
                            if (this.context.inRule != null) {
                                handleRuleEnd(this.context.inRule);
                            }
                            handleNoWordBreak();
                            markKeyword(false);
                            if (this.lastOffset != this.pos) {
                                tokenHandler.handleToken(line, this.context.rules.getDefault(), this.lastOffset - line.offset, this.pos - this.lastOffset, this.context);
                            }
                            tokenHandler.handleToken(line, this.context.rules.getDefault(), this.pos - line.offset, 1, this.context);
                            this.lastOffset = this.pos + 1;
                        } else {
                            if (this.keywords != null || this.context.rules.getRuleCount() != 0) {
                                String noWordSep = this.context.rules.getNoWordSep();
                                if (!Character.isLetterOrDigit(ch.charValue()) && noWordSep.indexOf(ch.charValue()) == -1) {
                                    if (this.context.inRule != null) {
                                        handleRuleEnd(this.context.inRule);
                                    }
                                    handleNoWordBreak();
                                    markKeyword(true);
                                    tokenHandler.handleToken(line, this.context.rules.getDefault(), this.lastOffset - line.offset, 1, this.context);
                                    this.lastOffset = this.pos + 1;
                                }
                            }
                            this.seenWhitespaceEnd = true;
                        }
                    }
                }
            }
            this.pos++;
        }
        this.pos = this.lineLength;
        if (this.context.inRule != null) {
            handleRuleEnd(this.context.inRule);
        }
        handleNoWordBreak();
        markKeyword(true);
        while (this.context.parent != null && (((rule = this.context.parent.inRule) != null && (rule.action & ParserRule.NO_LINE_BREAK) == 512) || terminated)) {
            this.context = this.context.parent;
            this.keywords = this.context.rules.getKeywords();
            this.context.setInRule(null);
        }
        tokenHandler.handleToken(line, Token.END, this.pos - line.offset, 0, this.context);
        this.context = this.context.intern();
        tokenHandler.setLineContext(this.context);
        this.tokenHandler = null;
        this.line = null;
        return this.context;
    }

    private boolean checkDelegateEnd(ParserRule rule) {
        if (rule.end == null) {
            return false;
        }
        LineContext tempContext = this.context;
        this.context = this.context.parent;
        this.keywords = this.context.rules.getKeywords();
        boolean handled = handleRuleEnd(rule);
        this.context = tempContext;
        this.keywords = this.context.rules.getKeywords();
        if (!handled) {
            return false;
        }
        if (this.context.inRule != null) {
            handleRuleEnd(this.context.inRule);
        }
        markKeyword(true);
        this.context = (LineContext) this.context.parent.clone();
        this.tokenHandler.handleToken(this.line, matchToken(this.context.inRule, this.context.inRule, this.context), this.pos - this.line.offset, this.pattern.count, this.context);
        this.keywords = this.context.rules.getKeywords();
        this.context.setInRule(null);
        this.lastOffset = this.pos + this.pattern.count;
        this.pos += this.pattern.count - 1;
        return true;
    }

    private boolean offsetMatches(int offset, int posMatch) {
        return (posMatch & 2) == 2 ? offset == this.line.offset : (posMatch & 4) == 4 ? offset == this.whitespaceEnd : (posMatch & 8) != 8 || offset == this.lastOffset;
    }

    private boolean handleRuleStart(ParserRule checkRule) {
        CharSequence charSeq;
        Matcher match;
        int matchedChars;
        if (checkRule.upHashChars == null) {
            if (checkRule.upHashChar != null && this.pos + checkRule.upHashChar.length < this.line.array.length && !checkHashString(checkRule)) {
                return false;
            }
        } else if (-1 == Arrays.binarySearch(checkRule.upHashChars, Character.toUpperCase(this.line.array[this.pos]))) {
            return false;
        }
        int offset = (checkRule.action & 4) != 0 ? this.lastOffset : this.pos;
        if (!offsetMatches(offset, checkRule.startPosMatch)) {
            return false;
        }
        if ((checkRule.action & ParserRule.REGEXP) == 0) {
            this.pattern.array = checkRule.start;
            this.pattern.offset = 0;
            this.pattern.count = this.pattern.array.length;
            int matchedChars2 = this.pattern.count;
            if (!SyntaxUtilities.regionMatches(this.context.rules.getIgnoreCase(), this.line, this.pos, this.pattern.array)) {
                return false;
            }
            charSeq = null;
            match = null;
            matchedChars = matchedChars2;
        } else {
            CharSequence charSeq2 = new SegmentCharSequence(this.line, this.pos - this.line.offset, this.line.count - (this.pos - this.line.offset));
            Matcher match2 = checkRule.startRegexp.matcher(charSeq2);
            if (!match2.lookingAt()) {
                return false;
            }
            if (match2.start() != 0) {
                throw new InternalError("Can't happen");
            }
            int matchedChars3 = match2.end();
            if (matchedChars3 != 0) {
                charSeq = charSeq2;
                match = match2;
                matchedChars = matchedChars3;
            } else {
                charSeq = charSeq2;
                match = match2;
                matchedChars = 1;
            }
        }
        if ((checkRule.action & ParserRule.IS_ESCAPE) == 2048) {
            this.pos += this.pattern.count;
        } else {
            if (this.context.inRule != null) {
                handleRuleEnd(this.context.inRule);
            }
            markKeyword((checkRule.action & 4) != 4);
            switch (checkRule.action & ParserRule.MAJOR_ACTIONS) {
                case 0:
                    this.context.spanEndSubst = null;
                    if ((checkRule.action & ParserRule.REGEXP) != 0) {
                        handleTokenWithSpaces(this.tokenHandler, checkRule.token, this.pos - this.line.offset, matchedChars, this.context);
                    } else {
                        this.tokenHandler.handleToken(this.line, checkRule.token, this.pos - this.line.offset, matchedChars, this.context);
                    }
                    if (checkRule.delegate != null) {
                        this.context = new LineContext(checkRule.delegate, this.context.parent);
                        this.keywords = this.context.rules.getKeywords();
                        break;
                    }
                    break;
                case 2:
                case 16:
                    this.context.setInRule(checkRule);
                    byte tokenType = matchToken(checkRule, this.context.inRule, this.context);
                    if ((checkRule.action & ParserRule.REGEXP) != 0) {
                        handleTokenWithSpaces(this.tokenHandler, tokenType, this.pos - this.line.offset, matchedChars, this.context);
                    } else {
                        this.tokenHandler.handleToken(this.line, tokenType, this.pos - this.line.offset, matchedChars, this.context);
                    }
                    char[] spanEndSubst = null;
                    if (charSeq != null && checkRule.end != null) {
                        spanEndSubst = substitute(match, checkRule.end);
                    }
                    this.context.spanEndSubst = spanEndSubst;
                    this.context = new LineContext(checkRule.delegate, this.context);
                    this.keywords = this.context.rules.getKeywords();
                    break;
                case 4:
                    this.context.spanEndSubst = null;
                    if (this.pos != this.lastOffset) {
                        this.tokenHandler.handleToken(this.line, checkRule.token, this.lastOffset - this.line.offset, this.pos - this.lastOffset, this.context);
                    }
                    this.tokenHandler.handleToken(this.line, matchToken(checkRule, checkRule, this.context), this.pos - this.line.offset, this.pattern.count, this.context);
                    break;
                case 8:
                    this.tokenHandler.handleToken(this.line, matchToken(checkRule, checkRule, this.context), this.pos - this.line.offset, this.pattern.count, this.context);
                    this.context.spanEndSubst = null;
                    this.context.setInRule(checkRule);
                    break;
                default:
                    throw new InternalError("Unhandled major action");
            }
            this.pos += matchedChars - 1;
            this.lastOffset = this.pos + 1;
        }
        return true;
    }

    private boolean handleRuleEnd(ParserRule checkRule) {
        int offset = (checkRule.action & 4) != 0 ? this.lastOffset : this.pos;
        if (!offsetMatches(offset, checkRule.endPosMatch)) {
            return false;
        }
        if ((checkRule.action & 8) == 0) {
            if (this.context.spanEndSubst != null) {
                this.pattern.array = this.context.spanEndSubst;
            } else {
                this.pattern.array = checkRule.end;
            }
            this.pattern.offset = 0;
            this.pattern.count = this.pattern.array.length;
            if (!SyntaxUtilities.regionMatches(this.context.rules.getIgnoreCase(), this.line, this.pos, this.pattern.array)) {
                return false;
            }
        }
        if ((checkRule.action & ParserRule.IS_ESCAPE) != 0) {
            throw new AssertionError();
        }
        if ((this.context.inRule.action & 8) != 0) {
            if (this.pos != this.lastOffset) {
                this.tokenHandler.handleToken(this.line, this.context.inRule.token, this.lastOffset - this.line.offset, this.pos - this.lastOffset, this.context);
            }
            this.lastOffset = this.pos;
            this.context.setInRule(null);
            return true;
        }
        return true;
    }

    private void handleNoWordBreak() {
        ParserRule rule;
        if (this.context.parent != null && (rule = this.context.parent.inRule) != null && (this.context.parent.inRule.action & ParserRule.NO_WORD_BREAK) != 0) {
            if (this.pos != this.lastOffset) {
                this.tokenHandler.handleToken(this.line, rule.token, this.lastOffset - this.line.offset, this.pos - this.lastOffset, this.context);
            }
            this.lastOffset = this.pos;
            this.context = this.context.parent;
            this.keywords = this.context.rules.getKeywords();
            this.context.setInRule(null);
        }
    }

    private void handleTokenWithSpaces(TokenHandler tokenHandler, byte tokenType, int start, int len, LineContext context) {
        int end = start + len;
        int last = start;
        for (int i = start; i < end; i++) {
            if (Character.isWhitespace(this.line.array[this.line.offset + i])) {
                if (last != i) {
                    tokenHandler.handleToken(this.line, tokenType, last, i - last, context);
                }
                tokenHandler.handleToken(this.line, tokenType, i, 1, context);
                last = i + 1;
            }
        }
        if (last != end) {
            tokenHandler.handleToken(this.line, tokenType, last, end - last, context);
        }
    }

    private void markKeyword(boolean addRemaining) {
        byte id;
        int len = this.pos - this.lastOffset;
        if (len == 0) {
            return;
        }
        if (this.context.rules.getHighlightDigits()) {
            boolean digit = false;
            boolean mixed = false;
            for (int i = this.lastOffset; i < this.pos; i++) {
                char ch = this.line.array[i];
                if (Character.isDigit(ch)) {
                    digit = true;
                } else {
                    mixed = true;
                }
            }
            if (mixed) {
                Pattern digitRE = this.context.rules.getDigitRegexp();
                if (digit) {
                    if (digitRE == null) {
                        digit = false;
                    } else {
                        int oldCount = this.line.count;
                        int oldOffset = this.line.offset;
                        this.line.offset = this.lastOffset;
                        this.line.count = len;
                        CharSequence seq = new SegmentCharSequence(this.line);
                        digit = digitRE.matcher(seq).matches();
                        this.line.offset = oldOffset;
                        this.line.count = oldCount;
                    }
                }
            }
            if (digit) {
                this.tokenHandler.handleToken(this.line, (byte) 5, this.lastOffset - this.line.offset, len, this.context);
                this.lastOffset = this.pos;
                return;
            }
        }
        if (this.keywords != null && (id = this.keywords.lookup(this.line, this.lastOffset, len)) != 0) {
            this.tokenHandler.handleToken(this.line, id, this.lastOffset - this.line.offset, len, this.context);
            this.lastOffset = this.pos;
        } else if (addRemaining) {
            this.tokenHandler.handleToken(this.line, this.context.rules.getDefault(), this.lastOffset - this.line.offset, len, this.context);
            this.lastOffset = this.pos;
        }
    }

    private byte matchToken(ParserRule rule, ParserRule base, LineContext ctx) {
        switch (rule.matchType) {
            case -2:
                return base.token;
            case -1:
                return this.context.rules.getDefault();
            default:
                return rule.matchType;
        }
    }

    private boolean checkHashString(ParserRule rule) {
        for (int i = 0; i < rule.upHashChar.length; i++) {
            if (Character.toUpperCase(this.line.array[this.pos + i]) != rule.upHashChar[i]) {
                return false;
            }
        }
        return true;
    }

    public static class LineContext {
        private static final Map<LineContext, LineContext> intern = new HashMap();
        public ParserRule escapeRule;
        public ParserRule inRule;
        public LineContext parent;
        public ParserRuleSet rules;
        public char[] spanEndSubst;

        public LineContext(ParserRuleSet rs, LineContext lc) {
            this.rules = rs;
            this.parent = lc == null ? null : (LineContext) lc.clone();
            if (rs.getModeName() != null) {
                this.escapeRule = this.rules.getEscapeRule();
            } else {
                this.escapeRule = lc.escapeRule;
            }
        }

        public LineContext() {
        }

        private static boolean charArraysEqual(char[] c1, char[] c2) {
            if (c1 == null) {
                return c2 == null;
            }
            if (c2 == null || c1.length != c2.length) {
                return false;
            }
            for (int i = 0; i < c1.length; i++) {
                if (c1[i] != c2[i]) {
                    return false;
                }
            }
            return true;
        }

        public LineContext intern() {
            LineContext obj = intern.get(this);
            if (obj == null) {
                intern.put(this, this);
                return this;
            }
            return obj;
        }

        public int hashCode() {
            if (this.inRule != null) {
                return this.inRule.hashCode();
            }
            if (this.rules != null) {
                return this.rules.hashCode();
            }
            return 0;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof LineContext)) {
                return false;
            }
            LineContext lc = (LineContext) obj;
            return lc.inRule == this.inRule && lc.rules == this.rules && StandardUtilities.objectsEqual(this.parent, lc.parent) && charArraysEqual(this.spanEndSubst, lc.spanEndSubst);
        }

        public Object clone() {
            LineContext lc = new LineContext();
            lc.inRule = this.inRule;
            lc.rules = this.rules;
            lc.parent = this.parent == null ? null : (LineContext) this.parent.clone();
            lc.spanEndSubst = this.spanEndSubst;
            lc.escapeRule = this.escapeRule;
            return lc;
        }

        public void setInRule(ParserRule rule) {
            this.inRule = rule;
            if (rule != null && rule.escapeRule != null) {
                this.escapeRule = rule.escapeRule;
                return;
            }
            if (this.rules != null && this.rules.getModeName() != null) {
                this.escapeRule = this.rules.getEscapeRule();
            } else if (this.parent != null) {
                this.escapeRule = this.parent.escapeRule;
            } else {
                this.escapeRule = null;
            }
        }
    }
}
