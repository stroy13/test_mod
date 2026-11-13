package com.gmail.heagoo.apkeditor.translate;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class TranslationSourceTest {
    @Test public void selectedLanguageIsUsedNotDefault() {
        Map<String, List<String>> sources = new HashMap<>();
        sources.put("", Arrays.asList("default"));
        sources.put("-en", Arrays.asList("English"));
        assertEquals(Arrays.asList("English"), TranslationSource.selected(sources, "-en"));
        assertEquals(Arrays.asList("default"), TranslationSource.selected(sources, ""));
        assertNull(TranslationSource.selected(sources, "-fr"));
        assertNull(TranslationSource.selected(sources, null));
    }
}
