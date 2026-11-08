package com.gmail.heagoo.apkeditor;

import java.util.Locale;

/** Human-readable names for Android string-resource qualifiers such as {@code -af} or {@code -zh-rCN}. */
public final class ResourceLocales {
    private static final Locale DISPLAY_LOCALE = Locale.forLanguageTag("ru");

    private ResourceLocales() {
    }

    public static String folder(String qualifier) {
        return "res/values" + (qualifier == null ? "" : qualifier);
    }

    public static String label(String qualifier) {
        if (qualifier == null || qualifier.isEmpty()) {
            return "По умолчанию";
        }
        String tag = languageTag(qualifier);
        if (tag.isEmpty()) {
            return "Конфигурация " + qualifier;
        }
        Locale locale = Locale.forLanguageTag(tag);
        String name = locale.getDisplayName(DISPLAY_LOCALE);
        if (name == null || name.isEmpty() || name.equalsIgnoreCase(tag)) {
            name = tag;
        } else {
            name = name.substring(0, 1).toUpperCase(DISPLAY_LOCALE) + name.substring(1);
        }
        return name + " (" + qualifier + ")";
    }

    public static String choice(String qualifier) {
        return label(qualifier) + "\n" + folder(qualifier);
    }

    public static String sortKey(String qualifier) {
        if (qualifier == null || qualifier.isEmpty()) {
            return "";
        }
        return label(qualifier).toLowerCase(Locale.ROOT) + "\u0000" + qualifier;
    }

    private static String languageTag(String qualifier) {
        String value = qualifier.startsWith("-") ? qualifier.substring(1) : qualifier;
        if (value.startsWith("b+")) {
            return value.substring(2).replace('+', '-');
        }
        String[] parts = value.split("-");
        if (parts.length == 0 || !parts[0].matches("[A-Za-z]{2,3}")) {
            return "";
        }
        StringBuilder tag = new StringBuilder(parts[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.matches("r[A-Za-z]{2}|r[A-Za-z]{3}")) {
                tag.append('-').append(part.substring(1).toUpperCase(Locale.ROOT));
            } else if (part.matches("[A-Za-z]{4}")) {
                tag.append('-').append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                        .append(part.substring(1).toLowerCase(Locale.ROOT));
            } else if (part.matches("[A-Za-z]{2,3}|[A-Za-z0-9]{5,8}")) {
                tag.append('-').append(part);
            } else {
                break;
            }
        }
        return tag.toString();
    }
}
