package com.gmail.heagoo.apkeditor.translate;

import static org.junit.Assert.*;
import common.types.StringItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class TranslationReviewEditsTest {
    @Test public void correctionStagesFailedStringWithoutChangingSource() {
        StringItem input = new StringItem("welcome", "Hello %s");
        List<StringItem> source = new ArrayList<>(Arrays.asList(input));
        List<StringItem> result = new ArrayList<>(Arrays.asList(input));
        List<String> rejected = new ArrayList<>(Arrays.asList("welcome"));
        Map<String, String> updates = new HashMap<>();
        assertFalse(TranslationReviewEdits.apply(0, source, result, updates, rejected, "Привет"));
        assertTrue(rejected.contains("welcome"));
        assertTrue(updates.isEmpty());
        assertTrue(TranslationReviewEdits.apply(0, source, result, updates, rejected, "Привет %s"));
        assertEquals("Hello %s", source.get(0).value);
        assertEquals("Привет %s", result.get(0).value);
        assertEquals("Привет %s", updates.get("welcome"));
        assertTrue(rejected.isEmpty());
    }
    @Test public void rejectsStyledReferencesAndInvalidXml() {
        assertFalse(TranslationReviewEdits.editable(new StringItem("x", "abc", "<b>abc</b>")));
        assertFalse(TranslationReviewEdits.editable(new StringItem("x", "@string/a")));
        assertFalse(TranslationReviewEdits.valid(new StringItem("x", "hello"), "bad" + (char) 1));
        assertTrue(TranslationReviewEdits.valid(new StringItem("x", "hello"), "Привет 🌟"));
    }
}
