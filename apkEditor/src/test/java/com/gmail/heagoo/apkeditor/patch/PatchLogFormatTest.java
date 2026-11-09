package com.gmail.heagoo.apkeditor.patch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The patch log used to print "<full path>: <count>" for every file, so a run of
 * smali entries wrapped into an undifferentiated wall of text with the file name
 * buried at the end of a long path.
 */
public class PatchLogFormatTest {

    private static final String SMALI =
            "smali/com/android/apksig/internal/apk/ApkSigningBlockUtils.smali";

    @Test
    public void fileNameLeadsAndDirectoryFollows() {
        String entry = PatchLogFormat.replacedFile(SMALI, "1 match replaced");
        String[] lines = entry.split("\n");
        assertEquals(2, lines.length);
        assertEquals("  " + PatchLogFormat.BULLET + "ApkSigningBlockUtils.smali — 1 match replaced",
                lines[0]);
        assertEquals("    smali/com/android/apksig/internal/apk/", lines[1]);
        assertTrue("entry must end with a newline", entry.endsWith("\n"));
    }

    @Test
    public void entriesDoNotRunTogether() {
        String a = PatchLogFormat.replacedFile(SMALI, "1 match replaced");
        String b = PatchLogFormat.replacedFile("smali/com/android/apksig/internal/apk/v1/V1SchemeSigner.smali",
                "2 matches replaced");
        assertTrue(a.endsWith("\n"));
        assertTrue(b.startsWith("  " + PatchLogFormat.BULLET));
        assertFalse("the file name must not be glued to the previous path",
                a.contains("V1SchemeSigner"));
    }

    @Test
    public void splitsPathSegments() {
        assertEquals("Foo.smali", PatchLogFormat.fileName(SMALI.replace("ApkSigningBlockUtils", "Foo")));
        assertEquals("smali/com/android/apksig/internal/apk/", PatchLogFormat.directory(SMALI));
        assertEquals("AndroidManifest.xml", PatchLogFormat.fileName("AndroidManifest.xml"));
        assertEquals("", PatchLogFormat.directory("AndroidManifest.xml"));
    }

    @Test
    public void rootLevelFileHasNoDirectoryLine() {
        String entry = PatchLogFormat.replacedFile("AndroidManifest.xml", "3 matches replaced");
        assertEquals(1, entry.split("\n").length);
        assertTrue(entry.contains("AndroidManifest.xml"));
    }

    @Test
    public void toleratesNullAndWindowsSeparators() {
        assertEquals("", PatchLogFormat.fileName(null));
        assertEquals("", PatchLogFormat.directory(null));
        assertEquals("", PatchLogFormat.fileName(""));
        String entry = PatchLogFormat.replacedFile("smali\\com\\Foo.smali", "1 match replaced");
        assertTrue(entry.contains("Foo.smali"));
        assertTrue(entry.contains("smali/com/"));
    }
}
