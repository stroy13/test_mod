package com.gmail.heagoo.apkeditor.translate;

/** Resolve explicit Android locale qualifiers without guessing a default resource language. */
public final class ResourceLanguageTag {
    private ResourceLanguageTag() {}
    public static String fromQualifier(String qualifier) {
        if (qualifier == null || !qualifier.matches("-[a-z]{2,3}(?:-r[A-Z]{2})?")) return null;
        return qualifier.substring(1).replace("-r", "-");
    }
}
