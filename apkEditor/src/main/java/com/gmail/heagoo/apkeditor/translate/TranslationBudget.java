package com.gmail.heagoo.apkeditor.translate;

/** Bounded output for individual UI strings; hitting the limit is treated as failure. */
public final class TranslationBudget {
    private TranslationBudget() {}
    public static int maxTokens(String value) {
        return Math.min(768, Math.max(128, 96 + 2 * value.length()));
    }
}
