package com.gmail.heagoo.apkeditor;

import java.util.regex.Pattern;

/** Validates a changed application ID without depending on the editor's own package name. */
public final class PackageNameValidator {
    public static final int MAX_LENGTH = 127;
    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private PackageNameValidator() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Returns a user-facing error, or {@code null} when the value may be saved.
     * An unchanged legacy identifier is accepted so other APK properties can still be edited.
     */
    public static String validationError(String value, String original) {
        String name = normalize(value);
        if (name.isEmpty()) {
            return "Введите название пакета, например com.example.app.";
        }
        if (original != null && name.equals(original)) {
            return null;
        }
        if (name.length() > MAX_LENGTH) {
            return "Название пакета слишком длинное: максимум " + MAX_LENGTH + " символов.";
        }
        if (!name.chars().allMatch(c -> c < 128)) {
            return "Используйте только латинские буквы, цифры, точку и подчёркивание.";
        }
        String[] segments = name.split("\\.", -1);
        if (segments.length < 2) {
            return "Название должно состоять минимум из двух частей через точку, например com.example.app.";
        }
        for (String segment : segments) {
            if (segment.isEmpty()) {
                return "Между точками должно быть имя: без пустых частей, двойных точек и точки в конце.";
            }
            if (!SEGMENT.matcher(segment).matches()) {
                if (Character.isDigit(segment.charAt(0))) {
                    return "Каждая часть после точки должна начинаться с латинской буквы, не с цифры.";
                }
                return "В названии пакета допустимы латинские буквы, цифры и подчёркивание; части разделяются точками.";
            }
        }
        return null;
    }
}
