package com.gmail.heagoo.apkeditor.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/* loaded from: /tmp/apkx/classes9.dex */
public final class Aapt2Pipeline {
    private Aapt2Pipeline() {
    }

    public static void build(String aapt2, String framework, String resDir, String manifest, String output) throws IOException, InterruptedException {
        build(aapt2, framework, resDir, manifest, output, null);
    }

    public static void build(String aapt2, String framework, String resDir, String manifest, String output,
                             ApkBuildPipeline.Logger logger) throws IOException, InterruptedException {
        File out = new File(output);
        File flat = new File(output + ".aapt2-flats.zip");
        if (flat.exists() && !flat.delete()) {
            throw new IOException("Cannot remove old AAPT2 intermediate");
        }
        if (out.exists() && !out.delete()) {
            throw new IOException("Cannot remove old AAPT2 output");
        }
        Path staged = null;
        try {
            Path original = new File(resDir).toPath().toAbsolutePath();
            long repairStart = System.nanoTime();
            Map<Path, byte[]> repairs = Aapt2DummyResources.findRepairs(original, logger);
            String compileDir = resDir;
            if (!repairs.isEmpty()) {
                staged = Files.createTempDirectory(out.getAbsoluteFile().getParentFile().toPath(), "aapt2-res-");
                Aapt2DummyResources.stage(original, staged, repairs);
                compileDir = staged.toString();
            }
            if (logger != null) logger.log("AAPT2 resource scan and staging: " + elapsed(repairStart) + " ms; repaired files=" + repairs.size());
            try {
                runTimed(new String[]{aapt2, "compile", "--legacy", "--no-crunch", "--dir", compileDir, "-o", flat.getAbsolutePath()}, logger);
            } catch (IOException e) {
                if (staged == null || e.getMessage() == null) throw e;
                throw new IOException(e.getMessage().replace(staged.toString(), original.toString()), e);
            }
            runTimed(new String[]{aapt2, "link", "--auto-add-overlay", "--no-version-vectors", "-I", framework, "--manifest", manifest, "-o", out.getAbsolutePath(), flat.getAbsolutePath()}, logger);
            if (!out.isFile() || out.length() == 0) {
                throw new IOException("AAPT2 produced no output");
            }
            if (flat.exists()) {
                flat.delete();
            }
        } finally {
            if (staged != null) Aapt2DummyResources.deleteTree(staged);
            if (flat.exists()) {
                flat.delete();
            }
        }
    }

    private static long elapsed(long started) {
        return (System.nanoTime() - started) / 1000000L;
    }

    private static void runTimed(String[] args, ApkBuildPipeline.Logger logger) throws IOException, InterruptedException {
        long started = System.nanoTime();
        try {
            run(args);
        } finally {
            if (logger != null) logger.log("AAPT2 " + args[1] + ": " + elapsed(started) + " ms");
        }
    }

    private static void run(String[] args) throws IOException, InterruptedException {
        final Process p = new ProcessBuilder(args).redirectErrorStream(true).start();
        final StringBuilder msg = new StringBuilder();
        Thread drain = new Thread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                Aapt2Pipeline.lambda$run$0(p, msg);
            }
        }, "aapt2-output");
        drain.setDaemon(true);
        drain.start();
        try {
            boolean finished = p.waitFor(300L, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                drain.join(1000L);
                throw new IOException("AAPT2 timed out");
            }
            drain.join(1000L);
            if (p.exitValue() != 0) {
                synchronized (msg) {
                    throw new IOException("AAPT2 " + args[1] + ": " + msg.toString());
                }
            }
        } catch (InterruptedException interrupted) {
            p.destroyForcibly();
            Thread.currentThread().interrupt();
            throw interrupted;
        }
    }

    static void lambda$run$0(Process p, StringBuilder msg) {
        try {
            Reader r = new InputStreamReader(p.getInputStream(), "UTF-8");
            try {
                char[] buf = new char[4096];
                while (true) {
                    int n = r.read(buf);
                    if (n == -1) {
                        r.close();
                        return;
                    } else {
                        synchronized (msg) {
                            if (msg.length() < 131072) {
                                msg.append(buf, 0, Math.min(n, 131072 - msg.length()));
                            }
                        }
                    }
                }
            } finally {
            }
        } catch (IOException e) {
        }
    }
}
