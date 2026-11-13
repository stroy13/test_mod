package com.gmail.heagoo.apkeditor.translate;

import static org.junit.Assert.*;
import common.types.StringItem;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class TranslationBatchTest {
    private final List<StringItem> items = Arrays.asList(new StringItem("open", "Open %s"), new StringItem("cancel", "Cancel"));
    @Test public void mapsExplicitIdsAndValidatesPlaceholders() throws Exception {
        assertEquals(Arrays.asList("Открыть %s", "Отмена"), TranslationBatch.parse(items, "[{\"id\":1,\"text\":\"Отмена\"},{\"id\":0,\"text\":\"Открыть %s\"}]"));
        for (String bad : new String[]{
                "[{\"id\":0,\"text\":\"Открыть\"},{\"id\":1,\"text\":\"Отмена\"}]",
                "[{\"id\":0,\"text\":\"Открыть %s\"},{\"id\":0,\"text\":\"Отмена\"}]",
                "[{\"id\":0,\"text\":\"Открыть %s\"}]",
                "[{\"id\":0,\"text\":\"Открыть %s\",\"extra\":1},{\"id\":1,\"text\":\"Отмена\"}]",
                "[{\"id\":0,\"text\":\"Открыть %s\"},{\"id\":1,\"text\":\"Отмена\"}] trailing"
        }) {
            try { TranslationBatch.parse(items, bad); fail(bad); } catch (IOException expected) { }
        }
    }
    @Test public void escapesPromptAndAvoidsUnsafeInputs() {
        assertTrue(TranslationBatch.prompt(items, "-en", "Russian").contains("from English into Russian"));
        String value = "Quote \" and newline\n%s";
        String prompt = TranslationBatch.prompt(Arrays.asList(new StringItem("one", value), items.get(1)), "", "Russian");
        assertTrue(prompt.contains("\\\""));
        assertTrue(prompt.contains("\\n%s"));
        assertFalse(TranslationBatch.eligible(new StringItem("reference", "@string/abc")));
        assertFalse(TranslationBatch.eligible(new StringItem("styled", "Hello", "<b>Hello</b>")));
        assertFalse(TranslationBatch.eligible(new StringItem("number", "123")));
        assertFalse(TranslationBatch.eligible(new StringItem("long", new String(new char[151]))));
    }
}
