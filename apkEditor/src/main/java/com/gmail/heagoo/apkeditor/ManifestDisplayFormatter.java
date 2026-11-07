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
        Plan plan = new Plan();
        if (text.length() == 0 || width <= 0.0f || measure.measure(text, 0, text.length()) <= width) {
            return plan;
        }
        List<ManifestSyntax.Span> attributes = new ArrayList<>();
        for (ManifestSyntax.Span span : spans) {
            if (span.kind == ManifestSyntax.ATTRIBUTE && span.start > 0 && whitespace(text.charAt(span.start - 1))) {
                attributes.add(span);
            }
        }
        float two = measure.measure("  ", 0, 2);
        float four = 2.0f * two;
        int lineStart = 0;
        float indent = 0.0f;
        int spanCursor = 0;
        for (int i = 0; i < attributes.size(); i++) {
            ManifestSyntax.Span attr = attributes.get(i);
            int end = i + 1 < attributes.size() ? attributes.get(i + 1).start : text.length();
            if (measure.measure(text, lineStart, end) + indent <= width) {
                continue;
            }
            if (attr.start > lineStart) {
                plan.insertions.add(new Insertion(attr.start, "\n  "));
                lineStart = attr.start;
                indent = two;
            }
            // If the attribute still does not fit on its own line, move its quoted
            // value onto a separate, deeper-indented line.
            if (measure.measure(text, lineStart, end) + indent > width) {
                while (spanCursor < spans.size() && spans.get(spanCursor).start < attr.end) {
                    spanCursor++;
                }
                // Bounded scan of this attribute's value spans. The previous
                // decompiled loop was `while (true)` and only advanced `j` in one
                // branch, so a matching `name="value"` kept adding insertions
                // forever until the heap was exhausted (OutOfMemoryError).
                for (int j = spanCursor; j < spans.size(); j++) {
                    ManifestSyntax.Span value = spans.get(j);
                    if (value.start >= end) {
                        break;
                    }
                    if (value.kind == ManifestSyntax.VALUE && quoted(text, value)
                            && text.substring(attr.end, value.start).trim().equals("=")) {
                        plan.insertions.add(new Insertion(value.start, "\n    "));
                        lineStart = value.start;
                        indent = four;
                        break;
                    }
                }
            }
        }
        for (ManifestSyntax.Span s : spans) {
            if (s.kind == ManifestSyntax.SYMBOL && s.start < text.length() && text.charAt(s.start) == '>') {
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
