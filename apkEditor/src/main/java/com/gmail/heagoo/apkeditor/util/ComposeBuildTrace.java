package com.gmail.heagoo.apkeditor.util;

import java.io.File;
import java.io.IOException;

/** One log for a complete compose operation, including nested signing. */
public final class ComposeBuildTrace implements AutoCloseable {
    private final BuildLog log;
    private String phase;
    private long started;

    public ComposeBuildTrace(File target, String mode) {
        BuildLog output = null;
        try {
            File parent = target.getAbsoluteFile().getParentFile();
            if (parent != null) output = new BuildLog(new File(parent, "build-log.txt"));
        } catch (IOException | SecurityException ignored) { }
        log = output;
        if (log != null) log.log("Full compose mode: " + mode + "; target=" + target);
    }
    public ApkBuildPipeline.Logger logger() { return log; }
    public void begin(String name) {
        end();
        phase = name;
        started = System.nanoTime();
        if (log != null) { log.log("Compose stage start: " + name); log.flush(); }
    }
    public void end() {
        if (phase != null) {
            long millis = (System.nanoTime() - started) / 1000000L;
            if (log != null) { log.log("Compose stage end: " + phase + " (" + millis + " ms)"); log.flush(); }
            phase = null;
        }
    }
    public void result(boolean success, boolean stopped, String error) {
        end();
        if (log != null) log.log("Compose result: " + (success ? "success" : stopped ? "cancelled" : "failed")
                + (error == null ? "" : "; " + error));
    }
    @Override public void close() {
        end();
        if (log != null) log.close();
    }
}
