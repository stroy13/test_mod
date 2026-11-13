package com.gmail.heagoo.apkeditor.translate;

import java.util.List;
import java.util.Map;

/** Never silently substitute the default language for the user's selection. */
public final class TranslationSource {
    private TranslationSource() {}
    /** ASCII numbers and punctuation have no words to translate. Keep them verbatim. */
    public static boolean isInvariant(String value) {
        if (value == null || value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 0x7e || c < 0x20 || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) return false;
        }
        return true;
    }
    public static <T> List<T> selected(Map<String, ? extends List<T>> languages, String qualifier) {
        if (languages == null || qualifier == null) return null;
        return languages.get(qualifier);
    }
}
