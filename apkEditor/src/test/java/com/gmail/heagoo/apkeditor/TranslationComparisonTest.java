package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import common.types.StringItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class TranslationComparisonTest {
    private static StringItem item(String name, String value) {
        return new StringItem(name, value);
    }

    @Test
    public void separatesMissingPlaceholderExtraAndOkRows() {
        List<StringItem> source = Arrays.asList(
                item("ok", "Hello %1$s"),
                item("missing", "Save"),
                item("placeholders", "You have %1$d files"),
                item("same", "OK"));
        List<StringItem> target = Arrays.asList(
                item("ok", "Привет %1$s"),
                item("placeholders", "Файлов: %1$s"),
                item("same", "OK"),
                item("extra", "Only target"));

        TranslationComparison.Result issues = TranslationComparison.compare(source, target, false);

        assertEquals(4, issues.sourceTotal);
        assertEquals(4, issues.targetTotal);
        assertEquals(2, issues.ok);
        assertEquals(1, issues.missing);
        assertEquals(1, issues.placeholderMismatches);
        assertEquals(1, issues.extra);
        assertEquals(3, issues.present());
        assertEquals(3, issues.issues());
        assertEquals(3, issues.rows.size());
        assertEquals(TranslationComparison.Status.MISSING, issues.rows.get(0).status);
        assertEquals("missing", issues.rows.get(0).name);
        assertNull(issues.rows.get(0).target);
        assertEquals(TranslationComparison.Status.PLACEHOLDERS, issues.rows.get(1).status);
        assertEquals("%1$d", issues.rows.get(1).sourceTokens);
        assertEquals("%1$s", issues.rows.get(1).targetTokens);
        assertEquals(TranslationComparison.Status.EXTRA, issues.rows.get(2).status);
        assertNull(issues.rows.get(2).source);

        TranslationComparison.Result all = TranslationComparison.compare(source, target, true);
        assertEquals(5, all.rows.size());
        assertEquals(TranslationComparison.Status.OK, all.rows.get(3).status);
        assertEquals(TranslationComparison.Status.OK, all.rows.get(4).status);
    }

    @Test
    public void keepsOriginalObjectsForEditingAndHandlesEmptyLists() {
        StringItem source = item("name", "Source");
        StringItem target = item("name", "Target");
        TranslationComparison.Result result = TranslationComparison.compare(
                Arrays.asList(source), Arrays.asList(target), true);
        assertSame(source, result.rows.get(0).source);
        assertSame(target, result.rows.get(0).target);

        TranslationComparison.Result empty = TranslationComparison.compare(null, new ArrayList<>(), false);
        assertEquals(0, empty.sourceTotal);
        assertEquals(0, empty.targetTotal);
        assertTrue(empty.rows.isEmpty());
        assertTrue(TranslationComparison.summary(empty, "", "-af").contains("По умолчанию"));
        assertTrue(TranslationComparison.summary(empty, "", "-af").contains("res/values-af"));
    }

    @Test
    public void duplicateNamesDoNotDuplicateRowsOrCounts() {
        TranslationComparison.Result result = TranslationComparison.compare(
                Arrays.asList(item("dup", "one"), item("dup", "two")),
                Arrays.asList(item("dup", "перевод")), true);
        assertEquals(1, result.sourceTotal);
        assertEquals(1, result.targetTotal);
        assertEquals(1, result.rows.size());
        assertEquals("one", result.rows.get(0).source.value);
    }
}
