package com.gmail.heagoo.apkeditor.patch;

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
    private static final String HEAD_INDENT = "  ";
    private static final String SUB_INDENT = "    ";

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
            sb.append(" \u2014 ").append(countText);
        }
        sb.append('\n');
        if (dir.length() > 0) {
            sb.append(SUB_INDENT).append(dir).append('\n');
        }
        return sb.toString();
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
