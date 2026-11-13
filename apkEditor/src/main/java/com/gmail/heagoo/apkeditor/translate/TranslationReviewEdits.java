package com.gmail.heagoo.apkeditor.translate;

import common.types.StringItem;
import java.util.List;
import java.util.Map;

/** Applies a user correction to the staged result without mutating its source. */
public final class TranslationReviewEdits {
    private TranslationReviewEdits() {}
    public static boolean editable(StringItem source) {
        return source != null && source.value != null && source.styledValue == null
                && !source.value.startsWith("@") && !source.value.startsWith("?");
    }
    public static boolean valid(StringItem source, String value) {
        if (!editable(source) || value == null || !TranslationTokens.preserved(source.value, value)) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') return false;
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
            } else if (Character.isLowSurrogate(c) || c == '\ufffe' || c == '\uffff') return false;
        }
        return true;
    }
    public static boolean apply(int position, List<StringItem> source, List<StringItem> result,
            Map<String, String> translated, List<String> rejected, String value) {
        StringItem item = source.get(position);
        if (!valid(item, value)) return false;
        result.set(position, new StringItem(item.name, value));
        translated.put(item.name, value);
        rejected.remove(item.name);
        return true;
    }
}
