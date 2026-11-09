package com.gmail.heagoo.apkeditor.patch;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout of the patch log.
 *
 * <p>Entries used to be emitted as {@code <full path>: <count>}, so a run of
 * smali files produced an unreadable wall of text: every long path wrapped over
 * two or three lines and butted straight against the next one, with the file
 * name -- the only part a reader actually scans for -- buried at the end.
 *
 * <p>Each entry is now a small block: the file name leads, the directory sits
 * underneath it, indented and separated. Pure Java so the layout can be tested
 * without Android.
 */
public final class PatchLogFormat {

    /** Marks a file that was actually rewritten. */
    public static final String BULLET = "\u2022 ";
    /** Separates the file name from the localised count. */
    public static final String SEPARATOR = " \u2014 ";
    private static final String HEAD_INDENT = "  ";
    private static final String SUB_INDENT = "    ";

    /** Colourable regions of an entry. */
    public static final int PART_BULLET = 0;
    public static final int PART_NAME = 1;
    public static final int PART_COUNT = 2;
    public static final int PART_DIRECTORY = 3;

    public static final class Part {
        public final int start;
        public final int end;
        public final int kind;

        Part(int start, int end, int kind) {
            this.start = start;
            this.end = end;
            this.kind = kind;
        }
    }

    private PatchLogFormat() {
    }

    /**
     * One replaced file.
     *
     * @param targetFile path relative to the decoded root, e.g.
     *                   {@code smali/com/android/apksig/Foo.smali}
     * @param countText  already localised, plural-aware summary, e.g.
     *                   {@code "1 совпадение заменено"}
     */
    public static String replacedFile(String targetFile, String countText) {
        String path = targetFile == null ? "" : targetFile.replace('\\', '/');
        String name = fileName(path);
        String dir = directory(path);
        StringBuilder sb = new StringBuilder();
        sb.append(HEAD_INDENT).append(BULLET).append(name);
        if (countText != null && countText.length() > 0) {
            sb.append(SEPARATOR).append(countText);
        }
        sb.append('\n');
        if (dir.length() > 0) {
            sb.append(SUB_INDENT).append(dir).append('\n');
        }
        return sb.toString();
    }

    /**
     * Locates the colourable parts of an entry produced by {@link #replacedFile}.
     *
     * <p>Derived from the text rather than carried alongside it, because the log
     * preview is rebuilt from a bounded buffer and may start or end mid-entry.
     * Anything unrecognised is simply left uncoloured.
     */
    public static List<Part> parts(String entry) {
        List<Part> out = new ArrayList<>();
        if (entry == null) {
            return out;
        }
        int bulletAt = entry.indexOf(BULLET);
        if (bulletAt < 0) {
            return out;
        }
        int nameStart = bulletAt + BULLET.length();
        int lineEnd = entry.indexOf('\n', nameStart);
        if (lineEnd < 0) {
            lineEnd = entry.length();
        }
        addPart(out, bulletAt, nameStart, PART_BULLET);
        int dash = entry.indexOf(SEPARATOR, nameStart);
        if (dash >= 0 && dash + SEPARATOR.length() <= lineEnd) {
            addPart(out, nameStart, dash, PART_NAME);
            // A preview cut exactly after the separator leaves an empty count;
            // an empty span would trip SpannableStringBuilder's bounds checks.
            addPart(out, dash + SEPARATOR.length(), lineEnd, PART_COUNT);
        } else {
            addPart(out, nameStart, lineEnd, PART_NAME);
        }
        if (lineEnd < entry.length()) {
            int dirStart = lineEnd + 1;
            int dirEnd = entry.indexOf('\n', dirStart);
            if (dirEnd < 0) {
                dirEnd = entry.length();
            }
            while (dirStart < dirEnd && entry.charAt(dirStart) == ' ') {
                dirStart++;
            }
            addPart(out, dirStart, dirEnd, PART_DIRECTORY);
        }
        return out;
    }

    /** Never emits an empty or reversed span. */
    private static void addPart(List<Part> out, int start, int end, int kind) {
        if (start >= 0 && end > start) {
            out.add(new Part(start, end, kind));
        }
    }

    /** Last path segment; the whole string when there is no separator. */
    public static String fileName(String path) {
        if (path == null || path.length() == 0) {
            return "";
        }
        String trimmed = path;
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        int slash = trimmed.lastIndexOf('/');
        return slash < 0 ? trimmed : trimmed.substring(slash + 1);
    }

    /** Everything before the last segment, keeping the trailing slash. */
    public static String directory(String path) {
        if (path == null) {
            return "";
        }
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash + 1);
    }
}
