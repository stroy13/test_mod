package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.patch.PatchLogFormat;
import com.gmail.heagoo.apkeditor.patch.PatchLogPalette;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * "The text is still white, you did not finish the job."
 *
 * <p>The palette and the renderer knew about file entries and phase notices, but
 * the buffer between them clamped every style above {@code SUCCESS} back to
 * {@code NORMAL}. Rule headings stayed violet because they were inside the old
 * range; the per-file entries and the phase line arrived as plain text and were
 * drawn in the default white. Reopening a saved log lost its colours too: the
 * restored segments were parsed into a local deque that was never installed.
 */
public class PatchLogBufferTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String ENTRY =
            PatchLogFormat.replacedFile("smali/com/facebook/internal/security/OidcSecurityUtil.smali",
                    "1 совпадение заменено");

    private PatchLogBuffer start() throws IOException {
        return PatchLogBuffer.start(tmp.getRoot());
    }

    private static PatchLogBuffer.Range covering(PatchLogBuffer.Preview preview, String needle) {
        int at = preview.text.indexOf(needle);
        assertTrue("missing text: " + needle, at >= 0);
        for (PatchLogBuffer.Range range : preview.ranges) {
            if (range.start <= at && at < range.end) {
                return range;
            }
        }
        return null;
    }

    @Test
    public void everyPaletteStyleSurvivesTheBuffer() throws IOException {
        PatchLogBuffer log = start();
        int[] styles = {PatchLogPalette.RULE, PatchLogPalette.ERROR, PatchLogPalette.DONE,
                PatchLogPalette.FILE_ENTRY, PatchLogPalette.PHASE};
        for (int style : styles) {
            log.append("line " + style + "\n", style);
        }
        PatchLogBuffer.Preview preview = log.styledPreview("");
        for (int style : styles) {
            PatchLogBuffer.Range range = covering(preview, "line " + style);
            assertNotNull("style " + style + " was dropped", range);
            assertEquals(style, range.style);
        }
    }

    /** The exact regression: the entry rendered white while headings did not. */
    @Test
    public void fileEntryKeepsItsStyle() throws IOException {
        PatchLogBuffer log = start();
        log.append("\nПрименение правила patch в строке 6...\n", PatchLogPalette.RULE);
        log.append(ENTRY, PatchLogPalette.FILE_ENTRY);
        PatchLogBuffer.Preview preview = log.styledPreview("");
        PatchLogBuffer.Range name = covering(preview, "OidcSecurityUtil.smali");
        PatchLogBuffer.Range dir = covering(preview, "smali/com/facebook/internal/security/");
        assertNotNull("file name lost its colour", name);
        assertNotNull("directory lost its colour", dir);
        assertEquals(PatchLogPalette.FILE_ENTRY, name.style);
        assertEquals(PatchLogPalette.FILE_ENTRY, dir.style);
        // One segment covers the whole two-line block, so the renderer can
        // colour the name, the count and the directory differently.
        assertEquals(name.start, dir.start);
        assertEquals(name.end, dir.end);
    }

    @Test
    public void phaseNoticeKeepsItsStyle() throws IOException {
        PatchLogBuffer log = start();
        log.append("Анализ правил внутри patch.txt...\n", PatchLogPalette.PHASE);
        PatchLogBuffer.Range range = covering(log.styledPreview(""), "Анализ правил");
        assertNotNull(range);
        assertEquals(PatchLogPalette.PHASE, range.style);
    }

    @Test
    public void unknownStylesStillFallBackToPlain() throws IOException {
        PatchLogBuffer log = start();
        log.append("plain\n", 99);
        log.append("negative\n", -3);
        assertTrue(log.styledPreview("").ranges.isEmpty());
    }

    @Test
    public void booleanAppendStillMeansError() throws IOException {
        PatchLogBuffer log = start();
        log.append("boom\n", true);
        log.append("fine\n", false);
        PatchLogBuffer.Preview preview = log.styledPreview("");
        assertEquals(1, preview.ranges.size());
        assertEquals(PatchLogPalette.ERROR, preview.ranges.get(0).style);
    }

    @Test
    public void adjacentHeadingsMerge() throws IOException {
        PatchLogBuffer log = start();
        log.append("a", PatchLogPalette.RULE);
        log.append("b", PatchLogPalette.RULE);
        assertEquals(1, log.styledPreview("").ranges.size());
    }

    /** Each entry must reach the renderer as its own colourable block. */
    @Test
    public void consecutiveFileEntriesDoNotMerge() throws IOException {
        PatchLogBuffer log = start();
        String second = PatchLogFormat.replacedFile("smali/com/example/JWTDecoder.smali",
                "2 совпадения заменено");
        log.append(ENTRY, PatchLogPalette.FILE_ENTRY);
        log.append(second, PatchLogPalette.FILE_ENTRY);
        PatchLogBuffer.Preview preview = log.styledPreview("");
        assertEquals(2, preview.ranges.size());
        for (PatchLogBuffer.Range range : preview.ranges) {
            assertEquals(PatchLogPalette.FILE_ENTRY, range.style);
            String slice = preview.text.substring(range.start, range.end);
            boolean named = false;
            for (PatchLogFormat.Part part : PatchLogFormat.parts(slice)) {
                if (part.kind == PatchLogFormat.PART_NAME) {
                    named = true;
                }
            }
            assertTrue("every entry needs a colourable file name", named);
        }
        assertEquals(PatchLogPalette.FILE_ENTRY, covering(preview, "OidcSecurityUtil.smali").style);
        assertEquals(PatchLogPalette.FILE_ENTRY, covering(preview, "JWTDecoder.smali").style);
    }

    /** Saved colours must come back, not just the text. */
    @Test
    public void coloursSurviveFinishAndReopen() throws IOException {
        PatchLogBuffer log = start();
        log.append("Применение правила patch в строке 6...\n", PatchLogPalette.RULE);
        log.append(ENTRY, PatchLogPalette.FILE_ENTRY);
        log.append("Анализ правил внутри patch.txt...\n", PatchLogPalette.PHASE);
        log.append("В AndroidManifest.xml совпадений не найдено.\n", PatchLogPalette.ERROR);
        log.finish();

        File saved = new File(tmp.getRoot(), "last_patch_log.txt");
        assertTrue(saved.isFile());
        PatchLogBuffer reopened = PatchLogBuffer.read(saved);
        PatchLogBuffer.Preview preview = reopened.styledPreview("");
        assertEquals(PatchLogPalette.FILE_ENTRY, covering(preview, "OidcSecurityUtil.smali").style);
        assertEquals(PatchLogPalette.FILE_ENTRY,
                covering(preview, "smali/com/facebook/internal/security/").style);
        assertEquals(PatchLogPalette.PHASE, covering(preview, "Анализ правил").style);
        assertEquals(PatchLogPalette.RULE, covering(preview, "Применение правила").style);
        assertEquals(PatchLogPalette.ERROR, covering(preview, "совпадений не найдено").style);
    }

    /** Logs written before the sidecar existed are still colourable. */
    @Test
    public void legacyLogWithoutSidecarIsColouredFromItsShape() throws IOException {
        File saved = new File(tmp.getRoot(), "old_log.txt");
        String text = "Применение правила patch в строке 6...\n" + ENTRY
                + "В AndroidManifest.xml совпадений не найдено.\n";
        Files.write(Paths.get(saved.getAbsolutePath()), text.getBytes(StandardCharsets.UTF_8));
        assertFalse(new File(tmp.getRoot(), "old_log.txt.colors").exists());
        PatchLogBuffer.Preview preview = PatchLogBuffer.read(saved).styledPreview("");
        assertEquals(PatchLogPalette.FILE_ENTRY, covering(preview, "OidcSecurityUtil.smali").style);
        assertEquals(PatchLogPalette.FILE_ENTRY,
                covering(preview, "smali/com/facebook/internal/security/").style);
        assertEquals(PatchLogPalette.ERROR, covering(preview, "совпадений не найдено").style);
    }

    /** A sidecar describing a different file must not paint the wrong ranges. */
    @Test
    public void staleSidecarIsIgnored() throws IOException {
        PatchLogBuffer log = start();
        log.append(ENTRY, PatchLogPalette.FILE_ENTRY);
        log.finish();
        File saved = new File(tmp.getRoot(), "last_patch_log.txt");
        File side = new File(tmp.getRoot(), "last_patch_log.txt.colors");
        assertTrue(side.isFile());
        // Same byte count, different content and a different trailer.
        Files.write(Paths.get(side.getAbsolutePath()), "0 5 1\nEND 1 1 1\n".getBytes(StandardCharsets.UTF_8));
        PatchLogBuffer.Preview preview = PatchLogBuffer.read(saved).styledPreview("");
        PatchLogBuffer.Range range = covering(preview, "OidcSecurityUtil.smali");
        assertNotNull(range);
        assertEquals(PatchLogPalette.FILE_ENTRY, range.style);
    }

    /** The first coloured release saved headings but dropped entry styles. */
    @Test
    public void oldSidecarWithoutEntryStylesKeepsInferredEntries() throws IOException {
        File saved = new File(tmp.getRoot(), "v193_log.txt");
        String heading = "Применение правила patch в строке 6...\n";
        String error = "В AndroidManifest.xml совпадений не найдено.\n";
        String text = heading + ENTRY + error;
        Files.write(Paths.get(saved.getAbsolutePath()), text.getBytes(StandardCharsets.UTF_8));
        int entryStart = heading.length();
        int errorStart = entryStart + ENTRY.length();
        String side = "0 " + entryStart + " " + PatchLogPalette.RULE + "\n"
                + errorStart + " " + text.length() + " " + PatchLogPalette.ERROR + "\n"
                + "END " + saved.length() + " " + saved.lastModified() + " " + text.length() + "\n";
        Files.write(Paths.get(new File(tmp.getRoot(), "v193_log.txt.colors").getAbsolutePath()),
                side.getBytes(StandardCharsets.UTF_8));

        PatchLogBuffer.Preview preview = PatchLogBuffer.read(saved).styledPreview("");
        assertEquals(PatchLogPalette.RULE, covering(preview, "Применение правила").style);
        assertEquals(PatchLogPalette.FILE_ENTRY, covering(preview, "OidcSecurityUtil.smali").style);
        assertEquals(PatchLogPalette.FILE_ENTRY,
                covering(preview, "smali/com/facebook/internal/security/").style);
        assertEquals(PatchLogPalette.ERROR, covering(preview, "совпадений не найдено").style);
    }

    @Test
    public void corruptSidecarDoesNotBreakReading() throws IOException {
        File saved = new File(tmp.getRoot(), "log.txt");
        Files.write(Paths.get(saved.getAbsolutePath()), ("hello\n" + ENTRY).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(new File(tmp.getRoot(), "log.txt.colors").getAbsolutePath()),
                "not-a-number\n".getBytes(StandardCharsets.UTF_8));
        PatchLogBuffer.Preview preview = PatchLogBuffer.read(saved).styledPreview("");
        assertTrue(preview.text.contains("OidcSecurityUtil.smali"));
        assertEquals(PatchLogPalette.FILE_ENTRY, covering(preview, "OidcSecurityUtil.smali").style);
    }

    /** Every cut of an entry must still produce spans inside the preview. */
    @Test
    public void truncatedEntryStaysInBounds() throws IOException {
        for (int cut = 0; cut <= ENTRY.length(); cut++) {
            PatchLogBuffer log = PatchLogBuffer.start(tmp.getRoot());
            log.append(ENTRY.substring(0, cut), PatchLogPalette.FILE_ENTRY);
            PatchLogBuffer.Preview preview = log.styledPreview("");
            for (PatchLogBuffer.Range range : preview.ranges) {
                assertTrue("cut " + cut, range.start >= 0);
                assertTrue("cut " + cut, range.end <= preview.text.length());
                assertTrue("empty span at cut " + cut, range.start < range.end);
                String slice = preview.text.substring(range.start, range.end);
                for (PatchLogFormat.Part part : PatchLogFormat.parts(slice)) {
                    assertTrue("cut " + cut, part.start < part.end);
                    assertTrue("cut " + cut, part.end <= slice.length());
                }
            }
            log.finish();
        }
    }

    @Test
    public void tailKeepsOnlyVisibleSegments() throws IOException {
        PatchLogBuffer log = start();
        StringBuilder filler = new StringBuilder();
        while (filler.length() < PatchLogBuffer.TAIL_LIMIT + 500) {
            filler.append("filler line\n");
        }
        log.append("head\n", PatchLogPalette.RULE);
        log.append(filler.toString(), PatchLogPalette.PLAIN);
        log.append(ENTRY, PatchLogPalette.FILE_ENTRY);
        String notice = "…";
        PatchLogBuffer.Preview preview = log.styledPreview(notice);
        assertEquals(notice + "\n\n", preview.text.substring(0, notice.length() + 2));
        assertTrue(preview.text.length() <= PatchLogBuffer.TAIL_LIMIT + notice.length() + 2);
        for (PatchLogBuffer.Range range : preview.ranges) {
            assertTrue(range.start >= 0);
            assertTrue(range.end <= preview.text.length());
            assertTrue(range.start < range.end);
        }
        assertEquals(PatchLogPalette.FILE_ENTRY, covering(preview, "OidcSecurityUtil.smali").style);
    }

    @Test
    public void sourcePointsAtTheFinishedLogAfterFinish() throws IOException {
        PatchLogBuffer log = start();
        log.append("x\n", PatchLogPalette.PLAIN);
        assertEquals(new File(tmp.getRoot(), "current_patch_log.txt"), log.source());
        log.finish();
        assertEquals(new File(tmp.getRoot(), "last_patch_log.txt"), log.source());
    }

    @Test
    public void reopenedBufferSharesTheFileItRead() throws IOException {
        File saved = new File(tmp.getRoot(), "last_patch_log.txt");
        Files.write(Paths.get(saved.getAbsolutePath()), "x\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(saved, PatchLogBuffer.read(saved).source());
    }

    @Test
    public void paletteAndBufferAgreeOnTheStyleRange() {
        assertEquals(PatchLogPalette.FILE_ENTRY, PatchLogBuffer.FILE_ENTRY);
        assertEquals(PatchLogPalette.PHASE, PatchLogBuffer.PHASE);
        assertEquals(PatchLogPalette.PHASE, PatchLogBuffer.MAX_STYLE);
        List<PatchLogFormat.Part> parts = PatchLogFormat.parts(ENTRY);
        assertFalse(parts.isEmpty());
    }
}
