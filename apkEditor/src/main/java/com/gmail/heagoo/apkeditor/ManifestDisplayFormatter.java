package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.ManifestSyntax;
import java.util.ArrayList;
import java.util.List;

public final class ManifestDisplayFormatter {

    public static final class Plan {
        public final List<Insertion> insertions = new ArrayList();
        public final List<Integer> noBreakSpaces = new ArrayList();
    }

    public interface Width {
        float measure(String str, int i, int i2);
    }

    public static final class Insertion {
        public final int offset;
        public final String text;

        Insertion(int offset, String text) {
            this.offset = offset;
            this.text = text;
        }
    }

    private static boolean whitespace(char c) {
        return c == ' ' || c == '\t';
    }

    private static boolean quoted(String text, ManifestSyntax.Span s) {
        return s.start < text.length() && (text.charAt(s.start) == '\"' || text.charAt(s.start) == '\'');
    }

    public static Plan plan(String text, List<ManifestSyntax.Span> spans, float width, Width measure) {
        List<ManifestSyntax.Span> attributes;
        List<ManifestSyntax.Span> list = spans;
        Width width2 = measure;
        Plan plan = new Plan();
        if (text.length() == 0 || width <= 0.0f || width2.measure(text, 0, text.length()) <= width) {
            return plan;
        }
        List<ManifestSyntax.Span> attributes2 = new ArrayList<>();
        for (ManifestSyntax.Span span : list) {
            if (span.kind == 2 && span.start > 0 && whitespace(text.charAt(span.start - 1))) {
                attributes2.add(span);
            }
        }
        float two = width2.measure("  ", 0, 2);
        float four = 2.0f * two;
        int lineStart = 0;
        float indent = 0.0f;
        int spanCursor = 0;
        int i = 0;
        while (i < attributes2.size()) {
            ManifestSyntax.Span attr = attributes2.get(i);
            int end = i + 1 < attributes2.size() ? attributes2.get(i + 1).start : text.length();
            if (width2.measure(text, lineStart, end) + indent <= width) {
                attributes = attributes2;
            } else {
                if (attr.start <= lineStart) {
                    attributes = attributes2;
                } else {
                    attributes = attributes2;
                    plan.insertions.add(new Insertion(attr.start, "\n  "));
                    lineStart = attr.start;
                    indent = two;
                }
                if (width2.measure(text, lineStart, end) + indent > width) {
                    while (spanCursor < list.size() && list.get(spanCursor).start < attr.end) {
                        spanCursor++;
                    }
                    int j = spanCursor;
                    while (true) {
                        if (j < list.size()) {
                            ManifestSyntax.Span value = list.get(j);
                            if (value.start < end && value.kind != 2 && value.kind != 1) {
                                if (value.kind == 3 && quoted(text, value)) {
                                    if (text.substring(attr.end, value.start).trim().equals("=")) {
                                        plan.insertions.add(new Insertion(value.start, "\n    "));
                                        int lineStart2 = value.start;
                                        lineStart = lineStart2;
                                        indent = four;
                                    }
                                } else {
                                    j++;
                                    list = spans;
                                }
                            }
                        }
                    }
                }
            }
            i++;
            list = spans;
            width2 = measure;
            attributes2 = attributes;
        }
        for (ManifestSyntax.Span s : spans) {
            if (s.kind == 5 && s.start < text.length() && text.charAt(s.start) == '>') {
                int close = s.start;
                int prev = close - 1;
                if (prev >= 0 && text.charAt(prev) == '/') {
                    plan.insertions.add(new Insertion(close, "\u2060"));
                    plan.insertions.add(new Insertion(prev, "\u2060"));
                    if (prev > 0 && text.charAt(prev - 1) == ' ') {
                        plan.noBreakSpaces.add(Integer.valueOf(prev - 1));
                    }
                } else if (prev >= 0 && (text.charAt(prev) == '\"' || text.charAt(prev) == '\'')) {
                    plan.insertions.add(new Insertion(close, "\u2060"));
                }
            }
        }
        return plan;
    }

    private ManifestDisplayFormatter() {
    }
}
