package com.gmail.heagoo.common;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Locale-aware, case-insensitive name order with whole decimal numbers compared
 * numerically. Iterative comparison also handles very long names and numbers
 * without recursion or integer overflow. Equal natural names retain a stable
 * case/binary tie-breaker.
 */
public final class NaturalOrderComparator implements Comparator<String> {
    private final Collator collator;

    public NaturalOrderComparator() {
        this(Locale.getDefault());
    }

    public NaturalOrderComparator(Locale locale) {
        collator = Collator.getInstance(locale);
        collator.setStrength(Collator.SECONDARY);
    }

    @Override
    public int compare(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        int ia = 0;
        int ib = 0;
        while (ia < a.length() && ib < b.length()) {
            if (isDigit(a.charAt(ia)) && isDigit(b.charAt(ib))) {
                while (ia < a.length() && digit(a.charAt(ia)) == 0) ia++;
                while (ib < b.length() && digit(b.charAt(ib)) == 0) ib++;
                int endA = ia;
                int endB = ib;
                while (endA < a.length() && isDigit(a.charAt(endA))) endA++;
                while (endB < b.length() && isDigit(b.charAt(endB))) endB++;
                int lengthOrder = Integer.compare(endA - ia, endB - ib);
                if (lengthOrder != 0) return lengthOrder;
                while (ia < endA) {
                    int difference = Integer.compare(digit(a.charAt(ia++)), digit(b.charAt(ib++)));
                    if (difference != 0) return difference;
                }
            } else {
                int endA = ia;
                int endB = ib;
                while (endA < a.length() && !isDigit(a.charAt(endA))) endA++;
                while (endB < b.length() && !isDigit(b.charAt(endB))) endB++;
                int textOrder = collator.compare(a.substring(ia, endA), b.substring(ib, endB));
                if (textOrder != 0) return textOrder;
                ia = endA;
                ib = endB;
            }
        }
        int remaining = Integer.compare(a.length() - ia, b.length() - ib);
        if (remaining != 0) return remaining;
        int caseOrder = String.CASE_INSENSITIVE_ORDER.compare(a, b);
        return caseOrder != 0 ? caseOrder : a.compareTo(b);
    }

    private static boolean isDigit(char c) {
        return (c >= '0' && c <= '9') || Character.isDigit(c);
    }

    private static int digit(char c) {
        return c >= '0' && c <= '9' ? c - '0' : Character.digit(c, 10);
    }
}
