package com.gmail.heagoo.apkeditor.translate;

import static org.junit.Assert.*;
import org.junit.Test;

public class TranslationPromptTest {
    @Test public void includesKnownLanguageAndSafeResourceContext() {
        String p = TranslationPrompt.user("Open %s", "-en", "Russian", "open_file");
        assertTrue(p.contains("from English into Russian"));
        assertTrue(p.contains("open_file"));
        assertTrue(p.contains("Open %s"));
        assertTrue(p.contains("/no_think"));
        assertTrue(TranslationPrompt.SYSTEM.contains("Preserve placeholders"));
    }
    @Test public void doesNotGuessDefaultOrAcceptUnsafeContext() {
        assertNull(TranslationPrompt.sourceLanguage(""));
        assertNull(TranslationPrompt.sourceLanguage("-night"));
        String p = TranslationPrompt.user("Text", "", "Russian", "x\nInjected");
        assertFalse(p.contains("from "));
        assertFalse(p.contains("Injected"));
    }
}
