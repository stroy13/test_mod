package com.gmail.heagoo.apkeditor.patch;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The patch window used to disappear at the end of a run.
 *
 * <p>Root cause: {@code findTargetRule} returns -1 for an unknown GOTO name, the
 * old loop assigned that straight to {@code index} and continued, so the next
 * {@code rules.get(-1)} threw on the worker thread. Nothing caught it, so the
 * uncaught exception killed the process and took the dialog with it. The worker
 * had no try/catch at all, so any failing rule did the same, and every early
 * return skipped {@code patchFinished()}, leaving the dialog stuck on
 * "Applying patch...".
 *
 * <p>These are source-level guards: the executor needs an Android activity, so it
 * cannot be exercised in a plain JVM test.
 */
public class PatchRobustnessTest {

    private static Path root() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir);
        return dir;
    }

    private static String read(String relative) throws Exception {
        return new String(Files.readAllBytes(root().resolve(relative)), StandardCharsets.UTF_8);
    }

    private static String executor() throws Exception {
        return read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/patch/PatchExecutor.java");
    }

    @Test
    public void unknownGotoTargetCannotIndexTheRuleList() throws Exception {
        String src = executor();
        assertTrue("the GOTO result must be checked before use",
                src.contains("int target = findTargetRule(rules, nextRule);"));
        assertTrue(src.contains("if (target < 0) {"));
        assertTrue(src.contains("patchContext.error(R.string.patch_error_no_rule, nextRule);"));
        assertFalse("assigning the raw lookup result to index is the crash",
                src.contains("index = findTargetRule(rules, nextRule);"));
        assertTrue("the loop must also guard its lower bound",
                src.contains("while (index >= 0 && index < rules.size())"));
    }

    @Test
    public void workerAlwaysReleasesTheDialog() throws Exception {
        String src = executor();
        assertTrue("a failing rule must be reported, not kill the process",
                src.contains("} catch (Throwable t) {"));
        assertTrue(src.contains("patchContext.error(R.string.patch_error_rule_failed, describe(t));"));
        assertTrue("patchFinished must run even on failure", src.contains("} finally {"));
        int finallyAt = src.indexOf("} finally {");
        int finishedAt = src.indexOf("patchContext.patchFinished();", finallyAt);
        assertTrue("patchFinished must be inside the finally block",
                finallyAt > 0 && finishedAt > finallyAt);
    }

    @Test
    public void cyclicGotoIsBounded() throws Exception {
        String src = executor();
        assertTrue(src.contains("int maxHops = Math.max(1000, rules.size() * 100);"));
        assertTrue(src.contains("patchContext.error(R.string.patch_error_goto_loop);"));
    }

    @Test
    public void missingPatchEntryReturnsInsteadOfThrowing() throws Exception {
        String src = executor();
        int errorAt = src.indexOf("patchContext.error(R.string.patch_error_no_entry");
        int streamAt = src.indexOf("InputStream input = sourceZip.getInputStream(entry);");
        assertTrue(errorAt > 0 && streamAt > errorAt);
        String between = src.substring(errorAt, streamAt);
        assertTrue("a missing patch.txt must return, not fall through to a null stream",
                between.contains("return;"));
        assertTrue(between.contains("patchContext.patchFinished();"));
    }

    @Test
    public void plainMessagesAreNotRunThroughStringFormat() throws Exception {
        String src = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/PatchDialog.java");
        // A literal '%' in a file path made String.format throw on the worker.
        assertFalse(src.contains("if (args != null) {"));
        assertTrue(src.contains("if (args != null && args.length > 0) {"));
        assertTrue(src.contains("private static String safeFormat(String format, Object[] args) {"));
    }

    @Test
    public void logEntriesUseTheStructuredFormat() throws Exception {
        String src = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/patch/PatchRule_MatchReplace.java");
        assertTrue(src.contains("PatchLogFormat.replacedFile(targetFile,"));
        assertTrue("counts must be plural-aware",
                src.contains("R.plurals.patch_matches_replaced"));
        assertFalse("the old '<path>: <count>' blob must be gone",
                src.contains("targetFile + \": \" + String.format"));
    }

    @Test
    public void russianPluralsCoverEveryCategory() throws Exception {
        String ru = read("apkEditor/src/main/res/values-ru/strings.xml");
        int start = ru.indexOf("<plurals name=\"patch_matches_replaced\">");
        assertTrue(start > 0);
        String block = ru.substring(start, ru.indexOf("</plurals>", start));
        for (String quantity : new String[]{"one", "few", "many", "other"}) {
            assertTrue("missing Russian plural: " + quantity,
                    block.contains("quantity=\"" + quantity + "\""));
        }
        assertTrue("1 must read as a singular", block.contains("%d \u0441\u043E\u0432\u043F\u0430\u0434\u0435\u043D\u0438\u0435 \u0437\u0430\u043C\u0435\u043D\u0435\u043D\u043E"));
    }
}
