package com.gmail.heagoo.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.Test;

public class NaturalOrderComparatorTest {
    private final NaturalOrderComparator order = new NaturalOrderComparator(Locale.ROOT);

    @Test
    public void numbersAreComparedByValue() {
        List<String> names = new ArrayList<>(Arrays.asList("file10", "file2", "file1", "file20"));
        names.sort(order);
        assertEquals(Arrays.asList("file1", "file2", "file10", "file20"), names);
    }

    @Test
    public void cyrillicAndCaseUseLocaleOrder() {
        List<String> names = new ArrayList<>(Arrays.asList("Яблоко10", "яблоко2", "Абрикос3", "абрикос10"));
        names.sort(new NaturalOrderComparator(new Locale("ru")));
        assertEquals(Arrays.asList("Абрикос3", "абрикос10", "яблоко2", "Яблоко10"), names);
    }

    @Test
    public void hugeNumbersDoNotOverflowOrRecurse() {
        String a = "file" + repeat('9', 10000);
        String b = "file1" + repeat('0', 10000);
        assertTrue(order.compare(a, b) < 0);
        assertTrue(order.compare(b, a) > 0);
    }

    @Test
    public void leadingZerosAndCaseHaveDeterministicTieBreak() {
        assertTrue(order.compare("v01", "v1") != 0);
        assertEquals(Integer.signum(order.compare("v01", "v1")),
                -Integer.signum(order.compare("v1", "v01")));
        assertTrue(order.compare("File", "file") != 0);
        assertEquals(0, order.compare("file2", "file2"));
    }

    @Test
    public void nullNamesAreSafe() {
        assertEquals(0, order.compare(null, null));
        assertEquals(0, order.compare(null, ""));
        assertTrue(order.compare(null, "a") < 0);
    }

    @Test
    public void comparatorIsAntisymmetricAndTransitiveOnMixedNames() {
        List<String> names = Arrays.asList("", "a", "a0", "a00", "a0b", "a1", "a01b", "a2",
                "a10", "A2", "b1", "b01", "b2", "абрикос2", "абрикос10", "яблоко1");
        for (String a : names) {
            for (String b : names) {
                assertEquals(Integer.signum(order.compare(a, b)),
                        -Integer.signum(order.compare(b, a)));
                for (String c : names) {
                    if (order.compare(a, b) <= 0 && order.compare(b, c) <= 0) {
                        assertTrue(a + " <= " + b + " <= " + c, order.compare(a, c) <= 0);
                    }
                }
            }
        }
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(order);
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue(order.compare(sorted.get(i - 1), sorted.get(i)) <= 0);
        }
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
