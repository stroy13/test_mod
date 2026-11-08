package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResourceLocalesTest {
    @Test
    public void showsDefaultAndNamedLocalesWithFolder() {
        assertEquals("По умолчанию", ResourceLocales.label(""));
        assertEquals("res/values", ResourceLocales.folder(""));
        assertEquals("res/values-af", ResourceLocales.folder("-af"));
        String af = ResourceLocales.label("-af");
        assertTrue(af, af.contains("Африкаанс") || af.contains("Afrikaans"));
        assertTrue(af.contains("(-af)"));
        assertTrue(ResourceLocales.choice("-zh-rCN").contains("res/values-zh-rCN"));
        assertTrue(ResourceLocales.label("-b+en+US").contains("(-b+en+US)"));
    }

    @Test
    public void unknownQualifiersRemainVisible() {
        assertTrue(ResourceLocales.label("-zz").contains("(-zz)"));
        assertEquals("", ResourceLocales.sortKey(""));
    }
}
