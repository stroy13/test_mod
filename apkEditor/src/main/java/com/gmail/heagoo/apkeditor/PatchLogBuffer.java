package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.patch.PatchLogFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/* loaded from: /tmp/apkx/classes5.dex */
public final class PatchLogBuffer {
    public static final int ERROR = 2;
    private static final int ERROR_LIMIT = 6000;
    public static final int HEADING = 1;
    private static final int HEAD_LIMIT = 2000;
    public static final int NORMAL = 0;
    public static final int SUCCESS = 3;
    /** A structured per-file entry; mirrors {@code PatchLogPalette.FILE_ENTRY}. */
    public static final int FILE_ENTRY = 4;
    /** A phase notice; mirrors {@code PatchLogPalette.PHASE}. */
    public static final int PHASE = 5;
    /**
     * Highest style this buffer preserves.
     *
     * <p>The buffer used to clamp everything above {@link #SUCCESS} back to
     * {@link #NORMAL}, so the per-file entries and phase notices added with the
     * coloured log lost their style on the way in and rendered in the default
     * white -- the headings kept their colour only because they fell inside the
     * old range. Keep this in step with {@code PatchLogPalette}.
     */
    public static final int MAX_STYLE = PHASE;
    public static final int TAIL_LIMIT = 24000;
    private final File completed;
    private final File pending;
    private BufferedWriter styleWriter;
    private long total;
    private IOException writeFailure;
    private BufferedWriter writer;
    private final StringBuilder head = new StringBuilder();
    private final StringBuilder tail = new StringBuilder();
    private final StringBuilder errors = new StringBuilder();
    private final ArrayDeque<Segment> styles = new ArrayDeque<>();

    public static final class Range {
        public final int end;
        public final int start;
        public final int style;

        Range(int start, int end, int style) {
            this.start = start;
            this.end = end;
            this.style = style;
        }
    }

    public static final class Preview {
        public final List<Range> ranges;
        public final String text;

        Preview(String text, List<Range> ranges) {
            this.text = text;
            this.ranges = ranges;
        }
    }

    private static final class Segment {
        long end;
        long start;
        final int style;

        Segment(long start, long end, int style) {
            this.start = start;
            this.end = end;
            this.style = style;
        }
    }

    private PatchLogBuffer(File pending, File completed) {
        this.pending = pending;
        this.completed = completed;
    }

    private static File colors(File text) {
        return new File(text.getParentFile(), text.getName() + ".colors");
    }

    public static PatchLogBuffer start(File directory) throws IOException {
        File pending = new File(directory, "current_patch_log.txt");
        PatchLogBuffer log = new PatchLogBuffer(pending, new File(directory, "last_patch_log.txt"));
        log.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pending, false), "UTF-8"), 32768);
        try {
            log.styleWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(colors(pending), false), "UTF-8"), 8192);
        } catch (IOException e) {
            log.styleWriter = null;
        }
        return log;
    }

    public static PatchLogBuffer read(File file) throws IOException {
        PatchLogBuffer log = new PatchLogBuffer(null, file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        try {
            // A file entry spans two lines (name + directory). Group them before
            // recording so the whole block can be coloured in parts, including
            // for logs written before the styles sidecar existed.
            StringBuilder entry = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (isEntryHead(line)) {
                    if (entry != null) {
                        log.record(entry.toString(), FILE_ENTRY);
                    }
                    entry = new StringBuilder(line).append('\n');
                } else if (entry != null && isEntryDirectory(line)) {
                    entry.append(line).append('\n');
                    log.record(entry.toString(), FILE_ENTRY);
                    entry = null;
                } else {
                    if (entry != null) {
                        log.record(entry.toString(), FILE_ENTRY);
                        entry = null;
                    }
                    log.record(line + "\n", looksLikeError(line) ? ERROR : NORMAL);
                }
            }
            if (entry != null) {
                log.record(entry.toString(), FILE_ENTRY);
            }
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // The log was read in full; a failing close must not hide it.
            }
        }
        log.restoreStyles(file);
        return log;
    }

    /** "  • File.smali — N matches replaced". */
    private static boolean isEntryHead(String line) {
        return line.startsWith("  " + PatchLogFormat.BULLET);
    }

    /** The indented directory line that follows an entry head. */
    private static boolean isEntryDirectory(String line) {
        return line.startsWith("    ") && line.trim().length() > 0
                && !line.trim().startsWith(PatchLogFormat.BULLET);
    }

    private static boolean looksLikeError(String line) {
        String s = line.toLowerCase(Locale.ROOT);
        return s.contains("error") || s.contains("exception") || s.contains("failed") || s.contains("not found") || s.contains("ошиб") || s.contains("не уда") || s.contains("не найд") || s.contains("не указан") || s.contains("не задан") || s.contains("не определ") || s.contains("недействитель") || s.contains("неподдерж") || s.contains("не существ") || s.contains("отсутствует") || s.contains("no match") || s.contains("invalid") || s.contains("unsupported");
    }

    public synchronized void append(String text, boolean error) {
        append(text, error ? 2 : 0);
    }

    public synchronized void append(String text, int style) {
        if (style < NORMAL || style > MAX_STYLE) {
            style = NORMAL;
        }
        if (this.writer != null && this.writeFailure == null) {
            try {
                this.writer.write(text);
            } catch (IOException e) {
                this.writeFailure = e;
            }
        }
        if (this.styleWriter != null && style != 0 && !text.isEmpty()) {
            try {
                this.styleWriter.write(this.total + " " + (this.total + text.length()) + " " + style + "\n");
            } catch (IOException e2) {
                closeStyle();
            }
        }
        record(text, style);
    }

    private void record(String text, int style) {
        long start = this.total;
        this.total += text.length();
        int n = Math.min(2000 - this.head.length(), text.length());
        if (n > 0) {
            this.head.append((CharSequence) text, 0, n);
        }
        appendBounded(this.tail, text, TAIL_LIMIT);
        if (style == 2) {
            appendBounded(this.errors, text, ERROR_LIMIT);
        }
        if (style != 0 && !text.isEmpty()) {
            Segment last = this.styles.peekLast();
            // A FILE_ENTRY segment is one structured block. Merging consecutive
            // entries would hand the renderer several bullets in one range, and
            // PatchLogFormat.parts() colours only the first entry it finds.
            if (last != null && last.style == style && style != FILE_ENTRY && last.end == start) {
                last.end = this.total;
            } else {
                this.styles.addLast(new Segment(start, this.total, style));
            }
        }
        trimStyles(this.styles, this.total - this.tail.length());
    }

    private static void trimStyles(ArrayDeque<Segment> segments, long offset) {
        while (!segments.isEmpty() && segments.peekFirst().end <= offset) {
            segments.removeFirst();
        }
        if (segments.isEmpty() || segments.peekFirst().start >= offset) {
            return;
        }
        segments.peekFirst().start = offset;
    }

    private static void appendBounded(StringBuilder target, String text, int max) {
        if (text.length() >= max) {
            target.setLength(0);
            target.append((CharSequence) text, text.length() - max, text.length());
        } else {
            target.append(text);
            if (target.length() > max) {
                target.delete(0, target.length() - max);
            }
        }
        if (target.length() <= 0 || !Character.isLowSurrogate(target.charAt(0))) {
            return;
        }
        target.deleteCharAt(0);
    }

    public synchronized boolean isTruncated() {
        return this.total > 24000;
    }

    public synchronized String preview(String truncatedNotice) {
        return styledPreview(truncatedNotice).text;
    }

    public synchronized Preview styledPreview(String truncatedNotice) {
        String result;
        List<Range> ranges;
        String prefix = this.total > 24000 ? truncatedNotice + "\n\n" : "";
        result = prefix + ((Object) this.tail);
        long offset = this.total - this.tail.length();
        ranges = new ArrayList<>();
        Iterator<Segment> it = this.styles.iterator();
        while (it.hasNext()) {
            Segment s = it.next();
            int start = prefix.length() + ((int) (Math.max(s.start, offset) - offset));
            int end = prefix.length() + ((int) (Math.min(s.end, this.total) - offset));
            if (start < end && end <= result.length()) {
                ranges.add(new Range(start, end, s.style));
            }
        }
        return new Preview(result, ranges);
    }

    public synchronized String excerpt(String truncatedNotice, String errorsHeading, String tailHeading) {
        if (this.total <= 24000) {
            return this.tail.toString();
        }
        return truncatedNotice + "\n\n" + ((Object) this.head) + "\n\n" + errorsHeading + "\n" + ((Object) this.errors) + "\n" + tailHeading + "\n" + ((Object) this.tail);
    }

    /**
     * Reloads the colours saved beside a finished log.
     *
     * <p>The sidecar is only trusted when its trailer still describes the text
     * file it was written for; a log that was edited, truncated or replaced
     * since then falls back to the plain reading. Previously the parsed
     * segments were collected into a local deque that was never installed, so
     * reopening the last log always came back uncoloured.
     */
    private void restoreStyles(File text) {
        File side = colors(text);
        if (!side.isFile()) {
            return;
        }
        ArrayDeque<Segment> restored = new ArrayDeque<>();
        long offset = this.total - this.tail.length();
        boolean complete = false;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(side), "UTF-8"));
            long previous = 0;
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 4 && parts[0].equals("END")) {
                    complete = Long.parseLong(parts[1]) == text.length()
                            && Long.parseLong(parts[2]) == text.lastModified()
                            && Long.parseLong(parts[3]) == this.total;
                    break;
                }
                if (parts.length != 3) {
                    return;
                }
                long begin = Long.parseLong(parts[0]);
                long end = Long.parseLong(parts[1]);
                int style = Integer.parseInt(parts[2]);
                if (begin < previous || end < begin || end > this.total
                        || style < HEADING || style > MAX_STYLE) {
                    return;
                }
                previous = end;
                if (end > offset) {
                    restored.addLast(new Segment(Math.max(begin, offset), end, style));
                }
            }
        } catch (IOException e) {
            return;
        } catch (RuntimeException e) {
            return;
        } finally {
            closeQuietly(in);
        }
        if (complete) {
            boolean hasEntryStyle = false;
            for (Segment segment : restored) {
                if (segment.style == FILE_ENTRY) {
                    hasEntryStyle = true;
                    break;
                }
            }
            ArrayList<Segment> merged = new ArrayList<>(restored);
            if (!hasEntryStyle) {
                // Logs saved by the first coloured-log release kept headings in
                // the sidecar but dropped file entries on the way in. Keep the
                // entries inferred from their shape so reopening them is coloured
                // too, without overriding the authoritative heading/error spans.
                for (Segment segment : this.styles) {
                    if (segment.style == FILE_ENTRY) {
                        merged.add(new Segment(segment.start, segment.end, segment.style));
                    }
                }
            }
            merged.sort(Comparator.comparingLong(segment -> segment.start));
            this.styles.clear();
            this.styles.addAll(merged);
        }
    }

    private static void closeQuietly(BufferedReader in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // Nothing useful to do while restoring an optional sidecar.
            }
        }
    }

    private void closeStyle() {
        if (this.styleWriter != null) {
            try {
                this.styleWriter.close();
            } catch (IOException e) {
            } catch (Throwable th) {
                this.styleWriter = null;
                throw th;
            }
            this.styleWriter = null;
        }
    }

    public synchronized void finish() throws IOException {
        if (this.writer == null) {
            return;
        }
        try {
            this.writer.close();
        } catch (IOException e) {
            if (this.writeFailure == null) {
                this.writeFailure = e;
            }
        }
        this.writer = null;
        if (this.writeFailure != null) {
            closeStyle();
            throw this.writeFailure;
        }
        if (!this.pending.renameTo(this.completed)) {
            closeStyle();
            throw new IOException("Cannot replace completed log: " + this.completed);
        }
        File pendingColors = colors(this.pending);
        File completedColors = colors(this.completed);
        if (this.styleWriter != null) {
            try {
                this.styleWriter.write("END " + this.completed.length() + " " + this.completed.lastModified() + " " + this.total + "\n");
                this.styleWriter.close();
                this.styleWriter = null;
                if (!pendingColors.renameTo(completedColors)) {
                    pendingColors.delete();
                }
            } catch (IOException e2) {
                closeStyle();
                pendingColors.delete();
            }
        }
    }

    public synchronized File source() {
        if (this.writer == null && (this.pending == null || !this.pending.exists())) {
            return this.completed;
        }
        return this.pending;
    }
}
