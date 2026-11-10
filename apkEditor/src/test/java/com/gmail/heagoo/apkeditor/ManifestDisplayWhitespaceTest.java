package com.gmail.heagoo.apkeditor;

import org.junit.Test;
import static org.junit.Assert.*;

public class ManifestDisplayWhitespaceTest {
    @Test public void indentationIsNonBreakingOnlyInDisplay() {
        String original = "  <tag\n    android:name=\"a b\" />\r\n\t x\n";
        StringBuilder displayed = new StringBuilder();
        for (int i = 0; i < original.length(); i++) displayed.append(ManifestDisplayWhitespace.at(original, i));
        assertEquals("\u00a0\u00a0<tag\n\u00a0\u00a0\u00a0\u00a0android:name=\"a b\" />\r\n\t\u00a0x\n", displayed.toString());
        assertEquals(original.length(), displayed.length());
        assertEquals("  <tag\n    android:name=\"a b\" />\r\n\t x\n", original);
    }
    @Test public void readsLiveEditsWithoutChangingText() {
        StringBuilder source = new StringBuilder("  x");
        assertEquals('\u00a0', ManifestDisplayWhitespace.at(source, 1));
        source.insert(0, 'a');
        assertEquals(' ', ManifestDisplayWhitespace.at(source, 1));
        source.setCharAt(0, '\n');
        assertEquals('\u00a0', ManifestDisplayWhitespace.at(source, 1));
        assertEquals("\n  x", source.toString());
    }
}
