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
        Path stagedManifest = null;
        try {
            Path original = new File(resDir).toPath().toAbsolutePath();
            long repairStart = System.nanoTime();
            Map<Path, byte[]> repairs = Aapt2DummyResources.findRepairs(original, logger);
            long aliasesStart = System.nanoTime();
            Map<String, String> aliases = Aapt2SyntheticNames.find(original);
            if (logger != null) logger.log("AAPT2 synthetic name scan: " + elapsed(aliasesStart) + " ms; aliases=" + aliases.size());
            String compileDir = resDir;
            String linkManifest = manifest;
            if (!repairs.isEmpty() || !aliases.isEmpty()) {
                staged = Files.createTempDirectory(out.getAbsoluteFile().getParentFile().toPath(), "aapt2-res-");
                long copyStart = System.nanoTime();
                Aapt2DummyResources.stage(original, staged, repairs);
                if (logger != null) logger.log("AAPT2 temporary resource copy: " + elapsed(copyStart) + " ms");
                if (!aliases.isEmpty()) {
                    long rewriteStart = System.nanoTime();
                    Aapt2SyntheticNames.rewriteStage(staged, aliases);
                    if (logger != null) logger.log("AAPT2 synthetic name rewrite: " + elapsed(rewriteStart) + " ms");
                    String originalManifest = new String(Files.readAllBytes(new File(manifest).toPath()), java.nio.charset.StandardCharsets.UTF_8);
                    String rewrittenManifest = Aapt2SyntheticNames.rewriteXml(originalManifest, aliases);
                    if (!originalManifest.equals(rewrittenManifest)) {
                        stagedManifest = Files.createTempFile(out.getAbsoluteFile().getParentFile().toPath(), "aapt2-manifest-", ".xml");
                        Files.write(stagedManifest, rewrittenManifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        linkManifest = stagedManifest.toString();
                    }
                    if (logger != null) logger.log("AAPT2 temporary synthetic drawable aliases: " + aliases.size());
                }
                compileDir = staged.toString();
            }
            if (logger != null) logger.log("AAPT2 resource scan and staging: " + elapsed(repairStart) + " ms; repaired files=" + repairs.size());
            try {
                runTimed(new String[]{aapt2, "compile", "--legacy", "--no-crunch", "--dir", compileDir, "-o", flat.getAbsolutePath()}, logger);
            } catch (IOException e) {
                if (staged == null || e.getMessage() == null) throw e;
                throw new IOException(e.getMessage().replace(staged.toString(), original.toString()), e);
            }
            String[] linkArgs = {aapt2, "link", "--auto-add-overlay", "--no-version-vectors", "-I", framework, "--manifest", linkManifest, "-o", out.getAbsolutePath(), flat.getAbsolutePath()};
            try {
                runTimed(linkArgs, logger);
            } catch (IOException strictFailure) {
                if (!isKnownOldManifestValidationFailure(strictFailure.getMessage())) throw strictFailure;
                if (out.exists() && !out.delete()) throw new IOException("Cannot remove failed AAPT2 output", strictFailure);
                if (logger != null) logger.log("AAPT2: older manifest validator rejected modern elements; retrying with validation warnings. Original error: " + strictFailure.getMessage());
                String[] retry = new String[linkArgs.length + 1];
                System.arraycopy(linkArgs, 0, retry, 0, linkArgs.length);
                retry[linkArgs.length] = "--warn-manifest-validation";
                String warnings = runTimed(retry, logger);
                if (!safeRetryWarnings(warnings)) {
                    if (out.exists()) out.delete();
                    throw new IOException("AAPT2 manifest validation reported additional problems: " + warnings, strictFailure);
                }
            }
            if (!aliases.isEmpty()) {
                long restoreStart = System.nanoTime();
                Aapt2SyntheticNames.restoreApk(out, aliases);
                if (logger != null) logger.log("AAPT2 synthetic drawable names restored in resource table: " + elapsed(restoreStart) + " ms");
            }
            if (!out.isFile() || out.length() == 0) {
                throw new IOException("AAPT2 produced no output");
            }
            if (flat.exists()) {
                flat.delete();
            }
        } finally {
            if (staged != null) Aapt2DummyResources.deleteTree(staged);
            if (stagedManifest != null) Files.deleteIfExists(stagedManifest);
            if (flat.exists()) {
                flat.delete();
            }
        }
    }

    private static long elapsed(long started) {
        return (System.nanoTime() - started) / 1000000L;
    }

    static boolean isKnownOldManifestValidationFailure(String message) {
        if (message == null || message.length() >= 131072) return false;
        boolean found = false;
        for (String line : message.split("\n")) {
            if (!line.contains("error:")) continue;
            if (!knownManifestDiagnostic(line, "error")) return false;
            found = true;
        }
        return found;
    }

    private static boolean knownManifestDiagnostic(String line, String level) {
        return line.contains(level + ": unexpected element <queries> found in <manifest>.")
                || line.contains(level + ": unexpected element <profileable> found in <manifest><application>.")
                || line.contains(level + ": unexpected element <property> found in <manifest><application>.")
                || line.contains(level + ": unexpected element <property> found in <manifest><application><service>.")
                || line.contains(level + ": unexpected element <property> found in <manifest><application><activity>.")
                || line.contains(level + ": unexpected element <property> found in <manifest><application><receiver>.")
                || line.contains(level + ": unexpected element <property> found in <manifest><application><provider>.");
    }

    static boolean safeRetryWarnings(String message) {
        if (message == null || message.length() >= 131072) return false;
        for (String line : message.split("\n")) {
            if (line.contains("error:")) return false;
            if (line.contains("warning: unexpected element") && !knownManifestDiagnostic(line, "warning")) return false;
        }
        return true;
    }

    private static String runTimed(String[] args, ApkBuildPipeline.Logger logger) throws IOException, InterruptedException {
        long started = System.nanoTime();
        try {
            String output = run(args);
            if (logger != null && !output.trim().isEmpty()) logger.log("AAPT2 " + args[1] + " output: " + output);
            return output;
        } finally {
            if (logger != null) logger.log("AAPT2 " + args[1] + ": " + elapsed(started) + " ms");
        }
    }

    private static String run(String[] args) throws IOException, InterruptedException {
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
            synchronized (msg) {
                if (p.exitValue() != 0) throw new IOException("AAPT2 " + args[1] + ": " + msg.toString());
                return msg.toString();
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
