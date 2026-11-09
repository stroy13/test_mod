package com.gmail.heagoo.apkeditor.patch;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The log was two-tone -- accent headings, default body -- so a long run looked
 * flat and nothing distinguished a rewritten file from a phase change. Each kind
 * of line now has a hue, and a file entry is coloured in parts.
 */
public class PatchLogPaletteTest {

    @Test
    public void everyLineKindHasItsOwnHue() {
        PatchLogPalette p = PatchLogPalette.dark();
        int[] hues = {p.text, p.rule, p.phase, p.error, p.done, p.fileName, p.count, p.directory, p.assign};
        for (int i = 0; i < hues.length; i++) {
            for (int j = i + 1; j < hues.length; j++) {
                assertTrue("colours " + i + " and " + j + " must differ", hues[i] != hues[j]);
            }
        }
    }

    @Test
    public void lineStylesMapToTheirColours() {
        PatchLogPalette p = PatchLogPalette.dark();
        assertEquals(p.rule, p.lineColor(PatchLogPalette.RULE));
        assertEquals(p.error, p.lineColor(PatchLogPalette.ERROR));
        assertEquals(p.done, p.lineColor(PatchLogPalette.DONE));
        assertEquals(p.phase, p.lineColor(PatchLogPalette.PHASE));
        assertEquals(p.assign, p.lineColor(PatchLogPalette.ASSIGN));
        assertEquals(p.text, p.lineColor(PatchLogPalette.PLAIN));
        assertEquals(p.text, p.lineColor(999));
    }

    @Test
    public void onlyHeadingsAreBold() {
        assertTrue(PatchLogPalette.isBold(PatchLogPalette.RULE));
        assertTrue(PatchLogPalette.isBold(PatchLogPalette.PHASE));
        assertTrue(PatchLogPalette.isBold(PatchLogPalette.ERROR));
        assertTrue(PatchLogPalette.isBold(PatchLogPalette.DONE));
        assertFalse(PatchLogPalette.isBold(PatchLogPalette.PLAIN));
        assertFalse(PatchLogPalette.isBold(PatchLogPalette.ASSIGN));
        assertFalse("entries bold only their file name", PatchLogPalette.isBold(PatchLogPalette.FILE_ENTRY));
    }

    @Test
    public void bothVariantsAreReadableOnTheirSurfaces() {
        // Default dark dialog surface and the light Sand dialog surface.
        assertTrue("dark log colours", PatchLogPalette.dark().isReadableOn(0xFF101721));
        assertTrue("light log colours", PatchLogPalette.light().isReadableOn(0xFFFAF6F0));
    }

    @Test
    public void selectionFollowsSurfaceBrightness() {
        assertTrue(PatchLogPalette.forDark(true).dark);
        assertFalse(PatchLogPalette.forDark(false).dark);
    }

    @Test
    public void entryPartsAreLocatedForColouring() {
        String entry = PatchLogFormat.replacedFile(
                "smali/com/android/apksig/internal/apk/ApkSigningBlockUtils.smali", "1 match replaced");
        List<PatchLogFormat.Part> parts = PatchLogFormat.parts(entry);
        assertEquals(4, parts.size());
        assertEquals(PatchLogFormat.PART_BULLET, parts.get(0).kind);
        assertEquals(PatchLogFormat.BULLET, entry.substring(parts.get(0).start, parts.get(0).end));
        assertEquals(PatchLogFormat.PART_NAME, parts.get(1).kind);
        assertEquals("ApkSigningBlockUtils.smali", entry.substring(parts.get(1).start, parts.get(1).end));
        assertEquals(PatchLogFormat.PART_COUNT, parts.get(2).kind);
        assertEquals("1 match replaced", entry.substring(parts.get(2).start, parts.get(2).end));
        assertEquals(PatchLogFormat.PART_DIRECTORY, parts.get(3).kind);
        assertEquals("smali/com/android/apksig/internal/apk/",
                entry.substring(parts.get(3).start, parts.get(3).end));
    }

    @Test
    public void rootLevelEntryHasNoDirectoryPart() {
        String entry = PatchLogFormat.replacedFile("AndroidManifest.xml", "2 matches replaced");
        List<PatchLogFormat.Part> parts = PatchLogFormat.parts(entry);
        assertEquals(3, parts.size());
        for (PatchLogFormat.Part part : parts) {
            assertTrue(part.kind != PatchLogFormat.PART_DIRECTORY);
        }
    }

    @Test
    public void truncatedEntriesStayInBounds() {
        // The preview is rebuilt from a bounded buffer and can cut mid-entry.
        String entry = PatchLogFormat.replacedFile("smali/com/example/Foo.smali", "1 match replaced");
        for (int cut = 0; cut <= entry.length(); cut++) {
            String piece = entry.substring(0, cut);
            for (PatchLogFormat.Part part : PatchLogFormat.parts(piece)) {
                assertTrue("start out of range at cut " + cut, part.start >= 0);
                assertTrue("end out of range at cut " + cut, part.end <= piece.length());
                assertTrue("empty span at cut " + cut, part.end > part.start);
            }
        }
    }

    @Test
    public void dialogRoutesEntriesAndPhasesToThePalette() throws Exception {
        java.nio.file.Path dir = java.nio.file.Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null && !java.nio.file.Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir);
        String src = new String(java.nio.file.Files.readAllBytes(dir.resolve(
                "apkEditor/src/main/java/com/gmail/heagoo/apkeditor/PatchDialog.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("entries must arrive as their own kind",
                src.contains("appendStyledText(text, PatchLogPalette.FILE_ENTRY);"));
        assertTrue("phase notices get their own hue",
                src.contains("resourceId == R.string.patch_start_parse || resourceId == R.string.decode_dex_file"));
        assertTrue("completion is a success colour", src.contains("style = PatchLogPalette.DONE;"));
        assertTrue("directories render monospace", src.contains("new TypefaceSpan(\"monospace\")"));
        assertFalse("the old two-tone helper must be gone", src.contains("private int logColor(int style)"));

        String rule = new String(java.nio.file.Files.readAllBytes(dir.resolve(
                "apkEditor/src/main/java/com/gmail/heagoo/apkeditor/patch/PatchRule_MatchReplace.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("replaced files must use the structured entry point",
                rule.contains("patchCtx.fileEntry(PatchLogFormat.replacedFile(targetFile,"));
    }

    @Test
    public void nonEntryTextYieldsNoParts() {
        assertTrue(PatchLogFormat.parts("Applying rule at line 6\n").isEmpty());
        assertTrue(PatchLogFormat.parts("").isEmpty());
        assertTrue(PatchLogFormat.parts(null).isEmpty());
    }
}
