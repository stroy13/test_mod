package com.gmail.heagoo.apkeditor.translate;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class ExistingTranslationUpdateTest {
    @Test public void mergesAcrossFilesAndPreservesOtherResources() throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("strings.xml", "<resources><!-- keep --><string name=\"one\">old</string><string name=\"extra\">keep</string><plurals name=\"p\"><item quantity=\"one\">x</item></plurals></resources>");
        files.put("other.xml", "<resources><string name='two'>old two</string></resources>");
        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("one", "new & value"); updates.put("two", "new two"); updates.put("added", "third");
        Map<String, String> result = ExistingTranslationUpdate.plan(files, updates, java.util.Collections.emptySet());
        assertTrue(result.get("strings.xml").contains("<!-- keep -->"));
        assertTrue(result.get("strings.xml").contains("new &amp; value"));
        assertTrue(result.get("strings.xml").contains("<string name=\"extra\">keep</string>"));
        assertTrue(result.get("strings.xml").contains("<plurals"));
        assertTrue(result.get("other.xml").contains("new two"));
        assertTrue(result.get("strings.xml").contains("name=\"added\""));
    }
    @Test public void refusesAmbiguousAndFormattedEntries() throws Exception {
        Map<String, String> updates = new LinkedHashMap<>(); updates.put("x", "new");
        Map<String, String> files = new LinkedHashMap<>(); files.put("a.xml", "<resources><string name=\"x\"><b>old</b></string></resources>");
        try { ExistingTranslationUpdate.plan(files, updates, java.util.Collections.emptySet()); fail(); } catch (java.io.IOException expected) { }
        files.put("a.xml", "<resources><string name=\"x\">one</string><string name=\"x\">two</string></resources>");
        try { ExistingTranslationUpdate.plan(files, updates, java.util.Collections.emptySet()); fail(); } catch (java.io.IOException expected) { }
    }
    @Test public void refusesMissingIndexedStringAndAmbiguousComment() throws Exception {
        Map<String, String> updates = new LinkedHashMap<>(); updates.put("x", "new");
        Map<String, String> files = new LinkedHashMap<>(); files.put("a.xml", "<resources><string name=\"y\">old</string></resources>");
        try { ExistingTranslationUpdate.plan(files, updates, java.util.Collections.singleton("x")); fail(); } catch (java.io.IOException expected) { }
        files.put("a.xml", "<resources><!-- <string name=\"x\">example</string> --><string name=\"x\">old</string></resources>");
        try { ExistingTranslationUpdate.plan(files, updates, java.util.Collections.emptySet()); fail(); } catch (java.io.IOException expected) { }
    }
}
