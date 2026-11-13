package com.gmail.heagoo.apkeditor.translate;

import java.util.List;
import java.util.Map;

/** Never silently substitute the default language for the user's selection. */
public final class TranslationSource {
    private TranslationSource() {}
    public static <T> List<T> selected(Map<String, ? extends List<T>> languages, String qualifier) {
        if (languages == null || qualifier == null) return null;
        return languages.get(qualifier);
    }
}
