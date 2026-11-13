package com.gmail.heagoo.apkeditor.translate;

import static org.junit.Assert.*;
import org.junit.Test;

public class ResourceLanguageTagTest {
    @Test public void convertsExplicitLocaleWithoutInventingDefault() {
        assertEquals("en", ResourceLanguageTag.fromQualifier("-en"));
        assertEquals("pt-BR", ResourceLanguageTag.fromQualifier("-pt-rBR"));
        assertNull(ResourceLanguageTag.fromQualifier(""));
        assertNull(ResourceLanguageTag.fromQualifier(null));
        assertNull(ResourceLanguageTag.fromQualifier("-night"));
        assertNull(ResourceLanguageTag.fromQualifier("-en-land"));
    }
}
