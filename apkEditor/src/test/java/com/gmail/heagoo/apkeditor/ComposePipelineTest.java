package com.gmail.heagoo.apkeditor;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The build screen sat on "Step 1/6: Composing" forever.
 *
 * <p>Both compose threads had been gutted by the decompiler. In the resource
 * branch the code showed the first step, called {@code prepare()} and then
 * returned unconditionally -- it never built resource.apk, never assembled dex,
 * never merged, never signed, and never told the UI anything. The methods that
 * do that work, {@code composeResource()} and {@code signApk()}, were present but
 * had no callers at all. The tail of the method was an empty
 * {@code if (!stopFlag) {}} where the completion callback used to be, so even the
 * non-resource path finished silently.
 *
 * <p>These are source-level guards: the threads need an Android context, aapt
 * binaries and a decoded project, so they cannot run in a plain JVM test.
 */
public class ComposePipelineTest {

    private static Path root() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir);
        return dir;
    }

    private static String read(String name) throws Exception {
        Path path = root().resolve("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/" + name);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static String runBody(String src) {
        int start = src.indexOf("    public void run() {");
        assertTrue("run() must exist", start > 0);
        int end = src.indexOf("    /** True when the user asked to stop", start);
        assertTrue("run() must be followed by the stop helper", end > start);
        return src.substring(start, end);
    }

    private static int countOf(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    @Test
    public void composeTraceCoversSigningAndAllExitPaths() throws Exception {
        for (String name : new String[]{"ApkComposeThreadNew.java", "ApkComposeThread.java"}) {
            String source = read(name);
            String run = runBody(source);
            assertTrue(run.contains("new ComposeBuildTrace("));
            assertTrue(run.contains("this.buildTrace.begin(\"prepare and compile resources\")"));
            assertTrue(run.contains("this.buildTrace.begin(\"assemble \""));
            assertTrue(run.contains("this.buildTrace.result("));
            assertTrue(run.contains("this.buildTrace.close();"));
            assertTrue(source.contains("this.buildTrace.logger()"));
        }
    }

    @Test
    public void buildButtonDoesNotWalkAllResourcesOnUiThreadForDebugOnly() throws Exception {
        String src = read("ApkInfoActivity.java");
        int start = src.indexOf("    protected void build(final boolean bSign)");
        int end = src.indexOf("    void m49lambda$build$31", start);
        assertTrue(start >= 0 && end > start);
        assertFalse(src.substring(start, end).contains("recursiveModifiedTime"));
    }

    @Test
    public void resourceStageActuallyBuildsResources() throws Exception {
        for (String name : new String[]{"ApkComposeThreadNew.java", "ApkComposeThread.java"}) {
            String run = runBody(read(name));
            int prepare = run.indexOf("boolean prepared = prepare();");
            int guard = run.indexOf("if (!prepared) return;", prepare);
            int compile = run.indexOf("boolean compiled = composeResource();", guard);
            int check = run.indexOf("if (!compiled) return;", compile);
            assertTrue(name + ": prepare and resource compile must both succeed", prepare >= 0 && guard > prepare && compile > guard && check > compile);
        }
    }

    @Test
    public void signingStageActuallySigns() throws Exception {
        String fresh = runBody(read("ApkComposeThreadNew.java"));
        assertTrue("the sign step must call signApk()", fresh.contains("if (!signApk()) {"));
        String legacy = runBody(read("ApkComposeThread.java"));
        assertTrue(legacy.contains("if (!signApk()) {"));
        assertTrue("unsigned builds still have to write the apk",
                legacy.contains("} else if (!notSignApk()) {"));
    }

    @Test
    public void resourceBranchDoesNotReturnEarly() throws Exception {
        for (String name : new String[]{"ApkComposeThreadNew.java", "ApkComposeThread.java"}) {
            String run = runBody(read(name));
            int compose = run.indexOf("R.string.compose");
            int merge = run.indexOf("R.string.merge");
            assertTrue(name + ": merge must be reachable after compose", compose > 0 && merge > compose);
            String between = run.substring(compose, merge);
            assertFalse(name + ": the resource branch must not bail out before merge",
                    between.contains("taskSucceed())") || between.contains("return;\n            }\n            return;"));
        }
    }

    @Test
    public void everyExitReportsTheOutcome() throws Exception {
        for (String name : new String[]{"ApkComposeThreadNew.java", "ApkComposeThread.java"}) {
            String src = read(name);
            String run = runBody(src);
            assertTrue(name + ": run must end in finally", run.contains("} finally {"));
            int finallyAt = run.indexOf("} finally {");
            assertTrue(name + ": finishTask must run from finally",
                    run.indexOf("finishTask();", finallyAt) > finallyAt);
            assertTrue(name + ": unexpected throws must be caught",
                    run.contains("} catch (Throwable t) {"));
            assertTrue(name + ": success must be reported", src.contains("callback.taskSucceed();"));
            assertTrue(name + ": failure must be reported", src.contains("callback.taskFailed(message);"));
            assertEquals(name + ": exactly one completion helper", 1, countOf(src, "private void finishTask() {"));
        }
    }

    @Test
    public void noEmptyStopBlocksRemain() throws Exception {
        for (String name : new String[]{"ApkComposeThreadNew.java", "ApkComposeThread.java"}) {
            String src = read(name);
            // The decompiler left these behind where the real work used to be.
            assertFalse(name + ": empty if (!stopFlag) block",
                    src.contains("if (!this.stopFlag) {\n        }"));
            assertFalse(name + ": empty trailing if (stopFlag) block",
                    src.contains("if (this.stopFlag) {\n        }"));
        }
    }

    /** Obfuscated resource types must not strand the build on AAPT2. */
    @Test
    public void unknownResourceTypesFallBackToOriginalAapt() throws Exception {
        for (String name : new String[]{"ApkComposeThreadNew.java", "ApkComposeThread.java"}) {
            String src = read(name);
            int start = src.indexOf("    private boolean composeResource() {");
            assertTrue(name + ": composeResource() must exist", start > 0);
            int end = src.indexOf("    private void warnAapt2Fallback", start);
            if (end < 0) {
                end = src.indexOf("    public boolean prepare()", start);
            }
            assertTrue(name + ": fallback helper must follow composeResource()", end > start);
            String body = src.substring(start, end);
            assertTrue(name + ": preflight must inspect the decoded resources",
                    body.contains("Aapt2ResourceTypes.unsupportedIn("));
            assertTrue(name + ": compiler failure must be classified",
                    body.contains("Aapt2ResourceTypes.isUnknownTypeFailure(e.getMessage())"));
            assertTrue(name + ": the original AAPT path must remain reachable",
                    body.contains("CommandRunner cr = new CommandRunner();"));
            assertTrue(name + ": fallback must be explained to the user",
                    body.contains("warnAapt2Fallback("));
        }
    }

    @Test
    public void fallbackWarningIsLocalisedAndExplainsTheToolChange() throws Exception {
        String en = new String(Files.readAllBytes(root().resolve(
                "apkEditor/src/main/res/values/strings.xml")), StandardCharsets.UTF_8);
        String ru = new String(Files.readAllBytes(root().resolve(
                "apkEditor/src/main/res/values-ru/strings.xml")), StandardCharsets.UTF_8);
        for (String key : new String[]{"aapt2_fallback_types", "aapt2_fallback_generic"}) {
            assertTrue(key + " missing in English", en.contains("name=\"" + key + "\""));
            assertTrue(key + " missing in Russian", ru.contains("name=\"" + key + "\""));
        }
        assertTrue(en.contains("name=\"aapt2_fallback_types\">AAPT2 cannot compile the resource types %1$s"));
        assertTrue(ru.contains("name=\"aapt2_fallback_types\">AAPT2 не умеет собирать типы ресурсов %1$s"));
    }

    @Test
    public void failureWithoutReasonStillExplainsItself() throws Exception {
        for (String name : new String[]{"ApkComposeThreadNew.java", "ApkComposeThread.java"}) {
            String src = read(name);
            assertTrue(name + ": a null error must not reach the dialog",
                    src.contains("R.string.lab_build_no_reason"));
            assertTrue(name + ": exceptions with no message must be named",
                    src.contains("t.getClass().getSimpleName()"));
        }
        String en = new String(Files.readAllBytes(root().resolve(
                "apkEditor/src/main/res/values/strings.xml")), StandardCharsets.UTF_8);
        String ru = new String(Files.readAllBytes(root().resolve(
                "apkEditor/src/main/res/values-ru/strings.xml")), StandardCharsets.UTF_8);
        assertTrue(en.contains("name=\"lab_build_no_reason\""));
        assertTrue(ru.contains("name=\"lab_build_no_reason\""));
    }
}
