package com.gmail.heagoo.apkeditor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The manifest code view reproduces MT Manager's editor (IntelliJ Darcula).
 * These values were sampled from a reference screenshot; pinning them keeps a
 * later theme refactor from silently drifting the code colours back to the app
 * chrome palette.
 */
public class ManifestEditorPaletteTest {

    @Test
    public void darkPaletteMatchesMtManager() {
        ManifestEditorPalette p = ManifestEditorPalette.dark();
        assertTrue(p.dark);
        assertEquals(0xFF2B2B2B, p.surface);
        assertEquals(0xFF37383A, p.gutter);
        assertEquals(0xFF888888, p.gutterText);
        assertEquals(0xFFE8BF6A, p.tag);
        assertEquals(0xFFBABABA, p.attribute);
        assertEquals(0xFF9876AA, p.namespace);
        assertEquals(0xFF6A8759, p.value);
        assertEquals(0xFF808080, p.comment);
    }

    @Test
    public void editorSurfaceIsNotTheAppChromeColour() {
        // The default app theme is a bluish #101721; the code view must keep its
        // own neutral surface instead of inheriting it.
        assertFalse(ManifestEditorPalette.dark().surface == 0xFF101721);
        assertFalse(ManifestEditorPalette.light().dark);
    }

    @Test
    public void angleBracketsShareTagColourButEqualsStaysNeutral() {
        ManifestEditorPalette p = ManifestEditorPalette.dark();
        assertEquals(p.tag, p.spanColor(ManifestSyntax.TAG, 'm'));
        assertEquals(p.tag, p.spanColor(ManifestSyntax.SYMBOL, '<'));
        assertEquals(p.tag, p.spanColor(ManifestSyntax.SYMBOL, '>'));
        assertEquals(p.tag, p.spanColor(ManifestSyntax.SYMBOL, '/'));
        assertEquals(p.value, p.spanColor(ManifestSyntax.SYMBOL, '='));
        assertEquals(p.attribute, p.spanColor(ManifestSyntax.ATTRIBUTE, 'a'));
        assertEquals(p.value, p.spanColor(ManifestSyntax.VALUE, '"'));
        assertEquals(p.comment, p.spanColor(ManifestSyntax.COMMENT, '<'));
        assertEquals(p.text, p.spanColor(999, 'x'));
    }

    @Test
    public void bothPalettesStayReadable() {
        assertTrue("dark editor colours must be legible", ManifestEditorPalette.dark().isReadable());
        assertTrue("light editor colours must be legible", ManifestEditorPalette.light().isReadable());
    }

    @Test
    public void selectionFollowsThemeBrightness() {
        assertTrue(ManifestEditorPalette.forDark(true).dark);
        assertFalse(ManifestEditorPalette.forDark(false).dark);
    }
}
