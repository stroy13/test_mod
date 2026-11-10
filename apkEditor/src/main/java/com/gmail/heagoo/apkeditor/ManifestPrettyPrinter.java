package com.gmail.heagoo.apkeditor;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a decoded manifest into one logical line per attribute, the way MT
 * Manager shows it.
 *
 * <p>Our AXML decoder emits a start tag and all of its attributes on a single
 * physical line, so {@code <manifest ...>} arrived as one enormous line: the
 * editor could only soft-wrap it, numbering stayed at 1, 2, 3 and every
 * attribute was prefixed with a continuation arrow. MT instead numbers each
 * attribute, which is only possible when each attribute really is its own line.
 *
 * <p>This operates on the display model, not on the file. Joining the produced
 * lines back with {@code '\n'} yields valid XML, because whitespace between
 * attributes is insignificant -- so editing a single line and saving still
 * round-trips.
 *
 * <p>Pure Java and bounded, so it is unit-testable and cannot run away on a
 * malformed manifest.
 */
public final class ManifestPrettyPrinter {

    /** Above this many attributes a tag is left alone; guards pathological input. */
    private static final int MAX_ATTRIBUTES = 4096;
    /** Hard ceiling on produced lines. */
    private static final int MAX_LINES = 200000;

    public static final class Line {
        public final String text;
        public final int indent;
        /** Index of the start-tag line this attribute belongs to, or -1. */
        public final int owner;
        /** True for comments, processing instructions, CDATA and text nodes. */
        public final boolean nonElement;

        Line(String text, int indent, int owner) {
            this(text, indent, owner, false);
        }

        Line(String text, int indent, int owner, boolean nonElement) {
            this.text = text;
            this.indent = indent;
            this.owner = owner;
            this.nonElement = nonElement;
        }
    }

    private ManifestPrettyPrinter() {
    }

    public static List<Line> format(String source) {
        List<Line> out = new ArrayList<>();
        if (source == null || source.length() == 0) {
            return out;
        }
        int depth = 0;
        int pos = 0;
        int n = source.length();
        while (pos < n && out.size() < MAX_LINES) {
            char c = source.charAt(pos);
            if (c == '<') {
                if (source.startsWith("<!--", pos)) {
                    int end = source.indexOf("-->", pos + 4);
                    int stop = end < 0 ? n : end + 3;
                    emitRaw(out, source.substring(pos, stop), depth);
                    pos = stop;
                    continue;
                }
                if (source.startsWith("<![CDATA[", pos)) {
                    int end = source.indexOf("]]>", pos + 9);
                    int stop = end < 0 ? n : end + 3;
                    emitRaw(out, source.substring(pos, stop), depth);
                    pos = stop;
                    continue;
                }
                if (source.startsWith("<?", pos)) {
                    int end = source.indexOf("?>", pos + 2);
                    int stop = end < 0 ? n : end + 2;
                    emitRaw(out, source.substring(pos, stop), depth);
                    pos = stop;
                    continue;
                }
                if (source.startsWith("<!", pos)) {
                    int stop = scanTagEnd(source, pos);
                    emitRaw(out, source.substring(pos, stop), depth);
                    pos = stop;
                    continue;
                }
                if (source.startsWith("</", pos)) {
                    int stop = scanTagEnd(source, pos);
                    depth = Math.max(0, depth - 1);
                    add(out, collapse(source.substring(pos, stop)), depth, -1);
                    pos = stop;
                    continue;
                }
                int stop = scanTagEnd(source, pos);
                String tag = source.substring(pos, stop);
                boolean selfClosing = tag.endsWith("/>");
                boolean closed = tag.endsWith(">");
                int consumed = emitStartTag(out, tag, depth);
                if (consumed < 0) {
                    // Not a parseable start tag: keep it verbatim.
                    emitRaw(out, tag, depth);
                } else if (closed && !selfClosing) {
                    depth++;
                }
                pos = stop;
                continue;
            }
            // Text node up to the next markup.
            int next = source.indexOf('<', pos);
            if (next < 0) {
                next = n;
            }
            emitRaw(out, source.substring(pos, next), depth);
            pos = next;
        }
        // Never expose a truncated model to the editor: callers can fall back
        // to the physical lines instead of overwriting the rest of the file.
        if (pos < n || out.size() >= MAX_LINES) return new ArrayList<>();
        return out;
    }

    /**
     * Emits a start tag, one attribute per line when it carries two or more.
     *
     * @return 0 on success, -1 when the tag could not be parsed
     */
    private static int emitStartTag(List<Line> out, String tag, int depth) {
        int n = tag.length();
        if (n < 2 || tag.charAt(0) != '<') {
            return -1;
        }
        int i = 1;
        while (i < n && !isSpace(tag.charAt(i)) && tag.charAt(i) != '>' && tag.charAt(i) != '/') {
            i++;
        }
        String name = tag.substring(1, i);
        if (name.length() == 0) {
            return -1;
        }
        boolean selfClosing = tag.endsWith("/>");
        int bodyEnd = n;
        if (selfClosing) {
            bodyEnd = n - 2;
        } else if (tag.charAt(n - 1) == '>') {
            bodyEnd = n - 1;
        }

        List<String> attributes = new ArrayList<>();
        int p = i;
        while (p < bodyEnd && attributes.size() < MAX_ATTRIBUTES) {
            while (p < bodyEnd && isSpace(tag.charAt(p))) {
                p++;
            }
            if (p >= bodyEnd) {
                break;
            }
            int start = p;
            while (p < bodyEnd && tag.charAt(p) != '=' && !isSpace(tag.charAt(p))) {
                p++;
            }
            if (p == start) {
                return -1;
            }
            String attrName = tag.substring(start, p);
            while (p < bodyEnd && isSpace(tag.charAt(p))) {
                p++;
            }
            if (p >= bodyEnd || tag.charAt(p) != '=') {
                // Bare attribute (not valid XML, but do not lose it).
                attributes.add(attrName);
                continue;
            }
            p++;
            while (p < bodyEnd && isSpace(tag.charAt(p))) {
                p++;
            }
            if (p >= bodyEnd) {
                return -1;
            }
            char quote = tag.charAt(p);
            String value;
            if (quote == '"' || quote == '\'') {
                int close = tag.indexOf(quote, p + 1);
                if (close < 0 || close > bodyEnd) {
                    return -1; // unterminated value: leave the tag alone
                }
                value = tag.substring(p, close + 1);
                p = close + 1;
            } else {
                int start2 = p;
                while (p < bodyEnd && !isSpace(tag.charAt(p))) {
                    p++;
                }
                value = tag.substring(start2, p);
            }
            attributes.add(attrName + "=" + value);
        }

        while (p < bodyEnd && isSpace(tag.charAt(p))) p++;
        if (p < bodyEnd) return -1; // exceeded the attribute limit: keep the entire tag

        String close = selfClosing ? " />" : ">";
        if (attributes.isEmpty()) {
            add(out, "<" + name + (selfClosing ? " />" : ">"), depth, -1);
            return 0;
        }
        if (attributes.size() == 1) {
            add(out, "<" + name + " " + attributes.get(0) + close, depth, -1);
            return 0;
        }
        int owner = out.size();
        add(out, "<" + name, depth, -1);
        for (int a = 0; a < attributes.size(); a++) {
            boolean last = a == attributes.size() - 1;
            add(out, attributes.get(a) + (last ? close : ""), depth + 1, owner);
        }
        return 0;
    }

    /** Index just past the '>' that closes this tag, honouring quoted values. */
    private static int scanTagEnd(String s, int from) {
        char quote = 0;
        for (int i = from + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '>') {
                return i + 1;
            }
        }
        return s.length();
    }

    /** Emits a comment / PI / text run, keeping its own line breaks. */
    private static void emitRaw(List<Line> out, String chunk, int depth) {
        int start = 0;
        for (int i = 0; i <= chunk.length(); i++) {
            if (i == chunk.length() || chunk.charAt(i) == '\n') {
                String piece = chunk.substring(start, i).trim();
                if (piece.length() > 0) {
                    // Comments may span lines and may contain '>'; they must not
                    // be mistaken for elements by the section builder.
                    out.add(new Line(piece, Math.max(0, depth), -1, true));
                }
                start = i + 1;
            }
        }
    }

    /** Collapses runs of whitespace outside quotes; values are left untouched. */
    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        char quote = 0;
        boolean pendingSpace = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                sb.append(c);
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                if (pendingSpace && sb.length() > 0) {
                    sb.append(' ');
                    pendingSpace = false;
                }
                quote = c;
                sb.append(c);
                continue;
            }
            if (isSpace(c)) {
                pendingSpace = true;
                continue;
            }
            if (pendingSpace && sb.length() > 0) {
                sb.append(' ');
            }
            pendingSpace = false;
            sb.append(c);
        }
        return sb.toString();
    }

    private static void add(List<Line> out, String text, int indent, int owner) {
        if (text.length() == 0 || out.size() >= MAX_LINES) {
            return;
        }
        out.add(new Line(text, Math.max(0, indent), owner));
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}
