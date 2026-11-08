package com.gmail.heagoo.apkeditor.util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Full, human-readable build log written next to the generated APK.
 *
 * <p>The log is intentionally a visible file ({@code build-log.txt}), not a hidden
 * dot-file, and it is produced for successful, failed and cancelled builds alike.
 * It records the input parameters, every preparation/merge/sign/verify step and the
 * complete stack trace of any failure so a broken package can be diagnosed without
 * adb/logcat.
 */
public final class BuildLog implements Closeable, ApkBuildPipeline.Logger {

    private final File file;
    private final PrintWriter writer;
    private final long startedAt = System.currentTimeMillis();
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private boolean closed;
    private int lineCount;

    public BuildLog(File file) throws IOException {
        this.file = file.getAbsoluteFile();
        File parent = this.file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create log directory: " + parent);
        }
        this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(this.file, false), StandardCharsets.UTF_8), 32 * 1024));
        line("=== APK Editor Plus build log ===");
        line("Started: " + new Date());
        line("Log file: " + this.file.getAbsolutePath());
    }

    public File getFile() {
        return file;
    }

    public String getPath() {
        return file.getAbsolutePath();
    }

    @Override
    public synchronized void log(String message) {
        line(message);
    }

    public synchronized void section(String title) {
        line("");
        line("--- " + title + " ---");
        writer.flush();
    }

    public synchronized void key(String name, Object value) {
        line(name + ": " + value);
    }

    public synchronized void map(String title, Map<String, String> values) {
        section(title);
        if (values == null || values.isEmpty()) {
            line("(empty)");
            return;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            File f = entry.getValue() == null ? null : new File(entry.getValue());
            String size = f != null && f.isFile() ? " (" + f.length() + " bytes)" : "";
            line(entry.getKey() + " <- " + entry.getValue() + size);
        }
    }

    public synchronized void error(Throwable error) {
        line("ERROR: " + error);
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        line(trace.toString());
        Throwable cause = error.getCause();
        while (cause != null) {
            line("Caused by: " + cause);
            cause = cause.getCause();
        }
        writer.flush();
    }

    public synchronized void flush() {
        writer.flush();
    }

    private void line(String message) {
        if (closed) {
            return;
        }
        long elapsed = System.currentTimeMillis() - startedAt;
        writer.print('[');
        writer.print(clock.format(new Date()));
        writer.print(" +");
        writer.print(elapsed);
        writer.print("ms] ");
        writer.println(message == null ? "null" : message);
        // Periodic flush so a sudden process death still leaves a useful tail on disk.
        if (++lineCount % 64 == 0) {
            writer.flush();
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        line("Finished: " + new Date());
        line("Total elapsed: " + (System.currentTimeMillis() - startedAt) + " ms");
        closed = true;
        writer.flush();
        writer.close();
    }
}
