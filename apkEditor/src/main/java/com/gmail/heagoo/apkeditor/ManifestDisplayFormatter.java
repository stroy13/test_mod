package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.ManifestSyntax;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ManifestDisplayFormatter {

    /** MT Manager marks a soft-wrapped continuation with this arrow. */
    public static final String CONTINUATION = "\n\u21B3";

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
        float arrow = measure.measure("\u21B3", 0, 1);
        List<ManifestSyntax.Span> attributes = new ArrayList<>();
        for (ManifestSyntax.Span span : spans) {
            if (span.kind == ManifestSyntax.ATTRIBUTE && span.start > 0 && whitespace(text.charAt(span.start - 1))) {
                attributes.add(span);
            }
        }
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
                plan.insertions.add(new Insertion(attr.start, CONTINUATION));
                lineStart = attr.start;
                indent = arrow;
            }
            // If the attribute still does not fit on its own line, move its quoted
            // value onto a separate continuation line.
            if (measure.measure(text, lineStart, end) + indent > width) {
                while (spanCursor < spans.size() && spans.get(spanCursor).start < attr.end) {
                    spanCursor++;
                }
                // Bounded scan of this attribute's value spans. The previous
                // loop was `while (true)` and only advanced `j` in one
                // branch, so a matching `name="value"` kept adding insertions
                // forever until the heap was exhausted (OutOfMemoryError).
                for (int j = spanCursor; j < spans.size(); j++) {
                    ManifestSyntax.Span value = spans.get(j);
                    if (value.start >= end) {
                        break;
                    }
                    if (value.kind == ManifestSyntax.VALUE && quoted(text, value)
                            && text.substring(attr.end, value.start).trim().equals("=")) {
                        plan.insertions.add(new Insertion(value.start, CONTINUATION));
                        lineStart = value.start;
                        indent = arrow;
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
        addHardBreaks(text, plan, width, measure, arrow);
        return plan;
    }

    /**
     * Splits segments that are still too wide after the attribute-based breaks.
     *
     * <p>Without this a long unbreakable value (the {@code xmlns} URL, a permission
     * name) was left to the TextView, which broke it mid-word with no marker. Worse,
     * the arrow inserted before such a value ended up alone on its own line,
     * because the value could not fit next to it either. Measuring the break here
     * keeps the arrow attached to the text it introduces.
     */
    private static void addHardBreaks(String text, Plan plan, float width, Width measure, float arrow) {
        List<Integer> bounds = new ArrayList<>();
        for (Insertion ins : plan.insertions) {
            if (ins.text.indexOf('\n') >= 0) {
                bounds.add(Integer.valueOf(ins.offset));
            }
        }
        Collections.sort(bounds);
        List<Insertion> extra = new ArrayList<>();
        int segStart = 0;
        float prefix = 0.0f;
        for (int b = 0; b <= bounds.size(); b++) {
            int segEnd = b < bounds.size() ? bounds.get(b).intValue() : text.length();
            float avail = width - prefix;
            int start = segStart;
            while (start < segEnd && avail > 0.0f
                    && measure.measure(text, start, segEnd) > avail
                    && extra.size() < 512) {
                int fit = lastFit(text, start, segEnd, avail, measure);
                if (fit <= start) {
                    fit = start + 1; // always advance, even for a very narrow column
                }
                extra.add(new Insertion(fit, CONTINUATION));
                start = fit;
                avail = width - arrow;
            }
            segStart = segEnd;
            prefix = arrow;
        }
        plan.insertions.addAll(extra);
    }

    /** Furthest offset in (start, end] whose measured width still fits. */
    private static int lastFit(String text, int start, int end, float avail, Width measure) {
        int lo = start + 1;
        int hi = end;
        int fit = start;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (measure.measure(text, start, mid) <= avail) {
                fit = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return fit;
    }

    private ManifestDisplayFormatter() {
    }
}
