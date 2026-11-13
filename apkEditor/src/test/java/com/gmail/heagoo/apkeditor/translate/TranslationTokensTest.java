package com.gmail.heagoo.apkeditor.translate;

import static org.junit.Assert.*;
import org.junit.Test;

public class TranslationTokensTest {
    @Test public void placeholdersCanBeReorderedButNotLost() {
        assertTrue(TranslationTokens.preserved("%1$s: %2$d", "%2$d — %1$s"));
        assertFalse(TranslationTokens.preserved("%s %s", "%s"));
        assertFalse(TranslationTokens.preserved("%1$s", "%1$d"));
        assertFalse(TranslationTokens.preserved("%s %d", "%d %s"));
    }
    @Test public void keepsAndroidTokens() {
        String source = "<b>%s</b> \\n &amp; @string/name";
        assertTrue(TranslationTokens.preserved(source, source));
        assertFalse(TranslationTokens.preserved(source, "<b>%s</b> &amp; @string/name"));
        assertFalse(TranslationTokens.preserved(source, "<i>%s</i> \\n &amp; @string/name"));
    }
    @Test public void rejectsNull() {
        assertFalse(TranslationTokens.preserved(null, ""));
        assertFalse(TranslationTokens.preserved("", null));
    }
}
