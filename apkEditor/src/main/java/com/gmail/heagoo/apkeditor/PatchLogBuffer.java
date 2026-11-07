package com.gmail.heagoo.apkeditor;

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
        while (true) {
            try {
                String line = reader.readLine();
                if (line == null) {
                    reader.close();
                    log.restoreStyles(file);
                    return log;
                }
                log.record(line + "\n", looksLikeError(line) ? 2 : 0);
            } catch (Throwable th) {
                try {
                    reader.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    private static boolean looksLikeError(String line) {
        String s = line.toLowerCase(Locale.ROOT);
        return s.contains("error") || s.contains("exception") || s.contains("failed") || s.contains("not found") || s.contains("ошиб") || s.contains("не уда") || s.contains("не найд") || s.contains("не указан") || s.contains("не задан") || s.contains("не определ") || s.contains("недействитель") || s.contains("неподдерж") || s.contains("не существ") || s.contains("отсутствует") || s.contains("no match") || s.contains("invalid") || s.contains("unsupported");
    }

    public synchronized void append(String text, boolean error) {
        append(text, error ? 2 : 0);
    }

    public synchronized void append(String text, int style) {
        if (style < 1 || style > 3) {
            style = 0;
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
            if (last != null && last.style == style && last.end == start) {
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

    /* JADX WARN: Code restructure failed: missing block: B:18:0x005e, code lost:
    
        if (java.lang.Long.parseLong(r0[1]) != r27.length()) goto L74;
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x006c, code lost:
    
        if (java.lang.Long.parseLong(r0[2]) != r27.lastModified()) goto L74;
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x007a, code lost:
    
        if (java.lang.Long.parseLong(r0[3]) != r26.total) goto L74;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x007c, code lost:
    
        r15 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x0082, code lost:
    
        if (r0.readLine() == null) goto L25;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x0084, code lost:
    
        r4 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x00e4, code lost:
    
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x00e7, code lost:
    
        if (r4 == false) goto L94;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x00e9, code lost:
    
        r26.styles.clear();
        r26.styles.addAll(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x00f3, code lost:
    
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:?, code lost:
    
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x0086, code lost:
    
        r4 = r15;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x0088, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x0089, code lost:
    
        r4 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x00f7, code lost:
    
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:?, code lost:
    
        throw r4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00fb, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x00fc, code lost:
    
        r4.addSuppressed(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00ff, code lost:
    
        throw r4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:?, code lost:
    
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void restoreStyles(File text) {
        boolean valid;
        File side = colors(text);
        if (side.isFile()) {
            ArrayDeque<Segment> restored = new ArrayDeque<>();
            boolean valid2 = false;
            long offset = this.total - this.tail.length();
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(side), "UTF-8"));
                long previous = 0;
                while (true) {
                    try {
                        String line = in.readLine();
                        if (line == null) {
                            break;
                        }
                        String[] parts = line.split(" ");
                        valid = false;
                        if (parts.length == 4 && parts[0].equals("END")) {
                            break;
                        }
                        if (parts.length != 3) {
                            in.close();
                            return;
                        }
                        long begin = Long.parseLong(parts[0]);
                        long end = Long.parseLong(parts[1]);
                        int style = Integer.parseInt(parts[2]);
                        if (begin < previous || end < begin || end > this.total || style < 1) {
                            break;
                        }
                        if (style > 3) {
                            break;
                        }
                        previous = end;
                        if (end > offset) {
                            restored.addLast(new Segment(Math.max(begin, offset), end, style));
                        }
                    } catch (Throwable th) {
                        valid = false;
                        Throwable th2 = th;
                    }
                }
                in.close();
            } catch (IOException e) {
            } catch (RuntimeException e2) {
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
        File file;
        if (this.writer == null && (this.pending == null || !this.pending.exists())) {
            file = this.completed;
        }
        file = this.pending;
        return file;
    }
}
