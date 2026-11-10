package com.gmail.heagoo.apkeditor;

/** Break attributes onto separate lines by changing only whitespace inside start tags.
 * Text, quoted values, comments, CDATA, declarations and processing instructions are untouched. */
final class ManifestTagFormatter {
    private ManifestTagFormatter() { }
    static String format(String source) {
        if (source.contains("<!DOCTYPE") || source.contains("<!ENTITY")) return source;
        StringBuilder out = new StringBuilder(source.length());
        int pos = 0;
        while (pos < source.length()) {
            int start = source.indexOf('<', pos);
            if (start < 0) { out.append(source, pos, source.length()); break; }
            out.append(source, pos, start);
            String endMarker = source.startsWith("<!--", start) ? "-->"
                    : source.startsWith("<![CDATA[", start) ? "]]>"
                    : source.startsWith("<?", start) ? "?>" : null;
            if (endMarker != null) {
                int end = source.indexOf(endMarker, start + 2);
                if (end < 0) { out.append(source, start, source.length()); break; }
                pos = end + endMarker.length();
                out.append(source, start, pos);
                continue;
            }
            int end = tagEnd(source, start);
            if (end < 0) { out.append(source, start, source.length()); break; }
            String tag = source.substring(start, end + 1);
            int line = source.lastIndexOf('\n', start - 1) + 1;
            int indent = 0;
            while (line + indent < start && source.charAt(line + indent) == ' ') indent++;
            out.append(formatTag(tag, Math.min(indent + 4, 80)));
            pos = end + 1;
        }
        return out.toString();
    }
    private static int tagEnd(String source, int start) {
        char quote = 0;
        for (int i = start + 1; i < source.length(); i++) {
            char c = source.charAt(i);
            if (quote != 0) { if (c == quote) quote = 0; }
            else if (c == '\'' || c == '"') quote = c;
            else if (c == '>') return i;
        }
        return -1;
    }
    private static boolean space(char c) { return c == ' ' || c == '\n' || c == '\r' || c == '\t'; }
    private static String formatTag(String tag, int indent) {
        if (tag.length() < 3 || tag.charAt(1) == '/' || tag.charAt(1) == '!') return tag;
        int i = 1, n = tag.length();
        while (i < n && !space(tag.charAt(i)) && tag.charAt(i) != '>' && tag.charAt(i) != '/') i++;
        if (i == 1) return tag;
        java.util.ArrayList<int[]> spaces = new java.util.ArrayList<>();
        int attributes = 0;
        boolean manifest = tag.substring(1, i).equals("manifest");
        while (i < n) {
            int ws = i;
            while (i < n && space(tag.charAt(i))) i++;
            if (i >= n || tag.charAt(i) == '>' || tag.charAt(i) == '/') break;
            if (i == ws) return tag;
            int attr = i;
            while (i < n && !space(tag.charAt(i)) && tag.charAt(i) != '=' && tag.charAt(i) != '>' && tag.charAt(i) != '/') i++;
            if (i == attr) return tag;
            while (i < n && space(tag.charAt(i))) i++;
            if (i >= n || tag.charAt(i++) != '=') return tag;
            while (i < n && space(tag.charAt(i))) i++;
            if (i >= n || (tag.charAt(i) != '\'' && tag.charAt(i) != '"')) return tag;
            char quote = tag.charAt(i++);
            int close = tag.indexOf(quote, i);
            if (close < 0) return tag;
            i = close + 1;
            attributes++;
            // Keep the first xmlns declaration next to <manifest as in the reference.
            boolean keepFirst = manifest && attributes == 1 && tag.startsWith("xmlns", attr);
            if (!keepFirst && tag.substring(ws, attr).indexOf('\n') < 0 && tag.substring(ws, attr).indexOf('\r') < 0)
                spaces.add(new int[]{ws, attr});
        }
        // A single attribute fits naturally beside its element name.
        if (attributes <= 1 || spaces.isEmpty()) return tag;
        StringBuilder result = new StringBuilder();
        int previous = 0;
        for (int[] range : spaces) {
            result.append(tag, previous, range[0]).append('\n');
            for (int j = 0; j < indent; j++) result.append(' ');
            previous = range[1];
        }
        return result.append(tag, previous, tag.length()).toString();
    }
}
