package com.gmail.heagoo.apkeditor;

import java.util.ArrayList;
import java.util.List;

public final class ManifestSyntax {
    public static final int ATTRIBUTE = 2;
    public static final int COMMENT = 4;
    public static final int SYMBOL = 5;
    public static final int TAG = 1;
    public static final int VALUE = 3;

    public static final class State {
        boolean comment;
        boolean expectTag;
        boolean inTag;
        char quote;
    }

    public static final class Span {
        public final int end;
        public final int kind;
        public final int start;

        Span(int start, int end, int kind) {
            this.start = start;
            this.end = end;
            this.kind = kind;
        }
    }

    private static boolean nameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '-' || c == '.';
    }

    private static void add(List<Span> spans, int start, int end, int kind) {
        if (end <= start || spans.size() >= 4096) {
            return;
        }
        spans.add(new Span(start, end, kind));
    }

    public static List<Span> tokenize(String text, State state) {
        int end;
        List<Span> out = new ArrayList<>();
        int i = 0;
        int n = text.length();
        while (true) {
            if (i >= n) {
                break;
            }
            if (state.comment) {
                int end2 = text.indexOf("-->", i);
                if (end2 < 0) {
                    add(out, i, n, 4);
                    break;
                }
                add(out, i, end2 + 3, 4);
                i = end2 + 3;
                state.comment = false;
            } else if (state.quote != 0) {
                int end3 = text.indexOf(state.quote, i);
                if (end3 < 0) {
                    add(out, i, n, 3);
                    break;
                }
                add(out, i, end3 + 1, 3);
                i = end3 + 1;
                state.quote = (char) 0;
            } else {
                if (text.startsWith("<!--", i)) {
                    state.comment = true;
                } else {
                    char c = text.charAt(i);
                    if (c == '<') {
                        add(out, i, i + 1, 5);
                        i++;
                        state.inTag = true;
                        state.expectTag = true;
                    } else if (state.inTag) {
                        if (c == '\"' || c == '\'') {
                            state.quote = c;
                            int end4 = text.indexOf(c, i + 1);
                            if (end4 < 0) {
                                add(out, i, n, 3);
                                break;
                            }
                            add(out, i, end4 + 1, 3);
                            int i2 = end4 + 1;
                            state.quote = (char) 0;
                            i = i2;
                        } else if (c == '>') {
                            add(out, i, i + 1, 5);
                            i++;
                            state.inTag = false;
                            state.expectTag = false;
                        } else if (nameChar(c)) {
                            int i3 = i + 1;
                            while (i3 < n && nameChar(text.charAt(i3))) {
                                i3++;
                            }
                            add(out, i, i3, state.expectTag ? 1 : 2);
                            state.expectTag = false;
                            i = i3;
                        } else {
                            if (c == '/' || c == '=' || c == '?' || c == '!') {
                                add(out, i, i + 1, 5);
                            }
                            i++;
                        }
                    } else if (c != '&' || (end = text.indexOf(59, i + 1)) < 0 || end - i >= 64) {
                        i++;
                    } else {
                        add(out, i, end + 1, 3);
                        i = end + 1;
                    }
                }
            }
        }
        return out;
    }
}
