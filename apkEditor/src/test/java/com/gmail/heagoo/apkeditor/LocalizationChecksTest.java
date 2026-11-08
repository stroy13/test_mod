package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LocalizationChecksTest {
    @Test
    public void formatsPositionalAndImplicitPlaceholdersReadably() {
        assertEquals("нет", LocalizationChecks.formatTokens(LocalizationChecks.tokens("plain")));
        assertEquals("%1$s, %2$d", LocalizationChecks.formatTokens(LocalizationChecks.tokens("%1$s and %2$d")));
        assertEquals("%1$s, %2$d", LocalizationChecks.formatTokens(LocalizationChecks.tokens("%s and %d")));
        assertEquals("%1$tH, %1$tM", LocalizationChecks.formatTokens(LocalizationChecks.tokens("%1$tH:%1$tM")));
        assertEquals("%2$s, %10$d", LocalizationChecks.formatTokens(LocalizationChecks.tokens("%10$d %2$s")));
    }

    @Test
    public void positionalAndImplicitFormsCompareEqual() {
        assertEquals(LocalizationChecks.tokens("%s %d"), LocalizationChecks.tokens("%1$s %2$d"));
    }
}
