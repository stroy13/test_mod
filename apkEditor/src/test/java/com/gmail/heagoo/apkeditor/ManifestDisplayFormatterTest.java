package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Регрессия на краш ru.apkeditor.plus:
 * java.lang.OutOfMemoryError в ManifestDisplayFormatter.plan(...):81.
 *
 * <p>Внутренний цикл был {@code while (true)} и увеличивал индекс
 * {@code j} только в одной из веток, поэтому для длинной строки манифеста с атрибутом
 * {@code name="value"} вставки добавлялись бесконечно, пока не заканчивалась куча.
 *
 * <p>Тесты используют только чистые Java-классы {@link ManifestSyntax} и
 * {@link ManifestDisplayFormatter} (без Android), поэтому выполняются как локальные
 * unit-тесты. {@code timeout} гарантирует, что при возврате бага тест упадёт быстро,
 * а не повесит/уронит CI по памяти.
 */
public class ManifestDisplayFormatterTest {

    private static ManifestDisplayFormatter.Width charWidth(final float px) {
        return new ManifestDisplayFormatter.Width() {
            @Override
            public float measure(String str, int i, int i2) {
                return (i2 - i) * px;
            }
        };
    }

    private static List<ManifestSyntax.Span> tokenize(String line) {
        return ManifestSyntax.tokenize(line, new ManifestSyntax.State());
    }

    private static void assertBounded(String line, ManifestDisplayFormatter.Plan plan) {
        assertNotNull(plan);
        assertTrue("ожидались вставки переноса для длинной строки", plan.insertions.size() > 0);
        assertTrue("число вставок должно быть ограничено", plan.insertions.size() < 100);
        for (ManifestDisplayFormatter.Insertion ins : plan.insertions) {
            assertTrue("смещение вне строки: " + ins.offset, ins.offset >= 0 && ins.offset <= line.length());
        }
    }

    @Test(timeout = 5000)
    public void longAttributeLine_terminatesAndWraps() {
        String line = "<activity android:name=\"com.example.myapplication.MainActivity\" android:exported=\"true\">";
        ManifestDisplayFormatter.Plan plan =
                ManifestDisplayFormatter.plan(line, tokenize(line), 200f, charWidth(7f));
        assertBounded(line, plan);
    }

    @Test(timeout = 5000)
    public void selfClosingLongLine_terminates() {
        String line = "<uses-permission android:name=\"android.permission.INTERNET\"/>";
        ManifestDisplayFormatter.Plan plan =
                ManifestDisplayFormatter.plan(line, tokenize(line), 200f, charWidth(7f));
        assertBounded(line, plan);
    }

    @Test(timeout = 5000)
    public void manyAttributes_terminates() {
        StringBuilder sb = new StringBuilder("<service");
        for (int i = 0; i < 40; i++) {
            sb.append(" android:attr").append(i).append("=\"value").append(i).append('"');
        }
        sb.append("/>");
        String line = sb.toString();
        ManifestDisplayFormatter.Plan plan =
                ManifestDisplayFormatter.plan(line, tokenize(line), 120f, charWidth(7f));
        assertNotNull(plan);
        assertTrue("число вставок должно быть ограничено", plan.insertions.size() < 1000);
    }

    @Test(timeout = 5000)
    public void shortLine_noWrapping() {
        String line = "<activity android:name=\"a\"/>";
        ManifestDisplayFormatter.Plan plan =
                ManifestDisplayFormatter.plan(line, tokenize(line), 2000f, charWidth(7f));
        assertEquals(0, plan.insertions.size());
        assertEquals(0, plan.noBreakSpaces.size());
    }

    @Test(timeout = 5000)
    public void emptyLine_noWrapping() {
        ManifestDisplayFormatter.Plan plan =
                ManifestDisplayFormatter.plan("", tokenize(""), 200f, charWidth(7f));
        assertEquals(0, plan.insertions.size());
        assertEquals(0, plan.noBreakSpaces.size());
    }
}
