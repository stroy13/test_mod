package com.gmail.heagoo.apkeditor.translate;

import java.util.Locale;

/** Constructs a compact prompt without guessing the default resource language. */
public final class TranslationPrompt {
    public static final String SYSTEM = "You localize app interfaces. Translate naturally and concisely, not word for word. Preserve placeholders, escapes and tags exactly. Output only the translation.";
    private TranslationPrompt() {}

    public static String sourceLanguage(String qualifier) {
        if (qualifier == null || !qualifier.matches("-[a-z]{2,3}(?:-r[A-Z]{2})?")) return null;
        String code = qualifier.substring(1).split("-")[0];
        Locale locale = new Locale(code);
        try { locale.getISO3Language(); } catch (java.util.MissingResourceException e) { return null; }
        String name = locale.getDisplayLanguage(Locale.ENGLISH);
        return name.isEmpty() || name.equalsIgnoreCase(code) ? null : name;
    }

    public static String user(String value, String sourceQualifier, String target, String resourceName) {
        String source = sourceLanguage(sourceQualifier);
        StringBuilder prompt = new StringBuilder("Translate ");
        if (source != null) prompt.append("from ").append(source).append(' ');
        prompt.append("into ").append(target).append(". Use natural, concise UI wording.\n");
        if (resourceName != null && resourceName.matches("[A-Za-z0-9_]{1,80}"))
            prompt.append("Resource key (context only): ").append(resourceName).append("\n");
        return prompt.append("Text: ").append(value).append("\n/no_think").toString();
    }
}
