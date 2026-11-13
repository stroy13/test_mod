package com.gmail.heagoo.apkeditor.util;

import java.util.Locale;

/** Presentation only: preserve raw file, paths and external tool diagnostics. */
public final class BuildLogDisplay {
    private static final String[][] RU = {
        {"Full compose mode: ", "Режим сборки: "},
        {"Compose stage start: ", "Начало этапа: "},
        {"Compose stage end: ", "Завершён этап: "},
        {"Compose result: ", "Результат сборки: "},
        {"Resource tool preparation: ", "Подготовка инструментов: "},
        {"Resource compose total: ", "Компоновка ресурсов: "},
        {"AAPT2 values scan: ", "AAPT2: проверка values: "},
        {"AAPT2 synthetic name scan: ", "AAPT2: поиск имён ресурсов: "},
        {"AAPT2 temporary resource copy: ", "AAPT2: копирование ресурсов: "},
        {"AAPT2 synthetic name rewrite: ", "AAPT2: обработка имён: "},
        {"AAPT2 temporary synthetic drawable aliases: ", "AAPT2: временных имён drawable: "},
        {"AAPT2 resource scan and staging: ", "AAPT2: подготовка ресурсов: "},
        {"AAPT2 synthetic drawable names restored in resource table: ", "AAPT2: имена ресурсов восстановлены: "},
        {"AAPT2 compile: ", "AAPT2: компиляция: "},
        {"AAPT2 link: ", "AAPT2: компоновка: "},
        {"DEX change check: ", "Проверка изменений DEX: "},
        {"Smali assembler: ", "Компилятор smali: "},
        {"Merge file collection and mapping: ", "Подготовка файлов для объединения: "},
        {"Merge ZIP engine: ", "Объединение ZIP: "},
        {"Signing key preference: ", "Ключ подписи: "},
        {"Source manifest: ", "Исходный манифест: "},
        {"Output manifest: ", "Итоговый манифест: "},
        {"Signing options: ", "Параметры подписи: "},
        {"Source: ", "Источник: "}, {"Target: ", "Результат: "},
        {"Stage: merge", "Этап: объединение"}, {"Stage: sign", "Этап: подпись"},
        {"Stage: verify", "Этап: проверка"}, {"Stage: publish", "Этап: сохранение"},
        {"merge ", "Добавление в APK: "}, {"skip ", "Пропуск: "},
        {"Signed APK structure verified", "Структура подписанного APK проверена"},
        {"build() succeeded", "Сборка успешно завершена"},
        {"build() failed: ", "Ошибка сборки: "},
        {"Finished: ", "Завершено: "}, {"Started: ", "Начало: "},
        {"Total elapsed: ", "Общее время: "}, {"Log file: ", "Файл журнала: "}
    };
    private BuildLogDisplay() {}
    public static String render(String line, String language) {
        if (line == null) return null;
        line = formatTimes(line, language);
        if (!"ru".equals(language)) return line;
        int end = line.startsWith("[") ? line.indexOf("] ") : -1;
        String prefix = end >= 0 ? line.substring(0, end + 2) : "";
        String body = end >= 0 ? line.substring(end + 2) : line;
        for (String[] pair : RU) if (body.startsWith(pair[0])) {
            String rest = body.substring(pair[0].length());
            if (pair[0].startsWith("Compose stage")) rest = stage(rest);
            else if (pair[0].equals("Compose result: ")) {
                if (rest.startsWith("success")) rest = "успех" + rest.substring(7);
                else if (rest.startsWith("failed")) rest = "ошибка" + rest.substring(6);
                else if (rest.startsWith("cancelled")) rest = "отменено" + rest.substring(9);
            }
            return prefix + pair[1] + rest;
        }
        return line;
    }
    private static final java.util.regex.Pattern TIMESTAMP = java.util.regex.Pattern.compile("^(\\[[^\\]]*? \\+)([0-9]+)ms(\\])");
    private static final java.util.regex.Pattern ELAPSED = java.util.regex.Pattern.compile("(?<![A-Za-z0-9_/.-])([0-9]+)\\s*ms\\b");

    static String formatDuration(long millis, String language) {
        boolean ru = "ru".equals(language);
        if (millis < 1000) return millis + (ru ? " мс" : " ms");
        long seconds = millis / 1000;
        long fraction = millis % 1000;
        String decimal = Long.toString(seconds % 60);
        if (fraction != 0) decimal += String.format(Locale.US, ".%03d", fraction).replaceFirst("0+$", "");
        if (millis < 60000) return decimal + (ru ? " с" : " s");
        return (seconds / 60) + (ru ? " мин " : " min ") + decimal + (ru ? " с" : " s");
    }

    private static String formatTimes(String line, String language) {
        java.util.regex.Matcher timestamp = TIMESTAMP.matcher(line);
        if (timestamp.find()) {
            try {
                String replacement = timestamp.group(1) + formatDuration(Long.parseLong(timestamp.group(2)), language) + timestamp.group(3);
                line = timestamp.replaceFirst(java.util.regex.Matcher.quoteReplacement(replacement));
            } catch (NumberFormatException ignored) { }
        }
        int separator = line.startsWith("[") ? line.indexOf("] ") : -1;
        String header = separator >= 0 ? line.substring(0, separator + 2) : "";
        String body = separator >= 0 ? line.substring(separator + 2) : line;
        // Only our own timing messages: arbitrary diagnostics and file names are
        // left untouched, even if they contain a number followed by "ms".
        if (!(body.startsWith("Compose stage end: ") || body.startsWith("Resource tool preparation: ")
                || body.startsWith("Resource compose total: ") || body.startsWith("AAPT2 values scan: ")
                || body.startsWith("AAPT2 synthetic name scan: ") || body.startsWith("AAPT2 temporary resource copy: ")
                || body.startsWith("AAPT2 synthetic name rewrite: ") || body.startsWith("AAPT2 resource scan and staging: ")
                || body.startsWith("AAPT2 synthetic drawable names restored in resource table: ")
                || body.startsWith("AAPT2 compile: ") || body.startsWith("AAPT2 link: ")
                || body.startsWith("Merge file collection and mapping: ") || body.startsWith("Merge ZIP engine: ")
                || body.startsWith("Total elapsed: "))) return line;
        java.util.regex.Matcher duration = ELAPSED.matcher(body);
        StringBuffer result = new StringBuffer();
        while (duration.find()) {
            try { duration.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(formatDuration(Long.parseLong(duration.group(1)), language))); }
            catch (NumberFormatException e) { duration.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(duration.group())); }
        }
        duration.appendTail(result);
        return header + result;
    }
    private static String stage(String name) {
        String[][] names = {{"resource change scan", "проверка изменений ресурсов"}, {"smali change scan", "проверка изменений smali"}, {"plan build", "планирование сборки"}, {"prepare and compile resources", "компоновка ресурсов"}, {"assemble ", "компиляция "}, {"merge APK", "объединение APK"}, {"sign and verify", "подпись и проверка"}, {"merge unsigned APK", "объединение неподписанного APK"}, {"normalize unsigned APK", "упаковка неподписанного APK"}, {"prepare original entries", "подготовка исходных файлов"}, {"cleanup", "очистка"}};
        for (String[] pair : names) if (name.startsWith(pair[0])) return pair[1] + name.substring(pair[0].length());
        return name;
    }
    public static String resolveLanguage(String preference, String systemLanguage) {
        String language = "system".equals(preference) ? systemLanguage : preference;
        return "ru".equals(language == null ? "" : language.toLowerCase(Locale.ROOT)) ? "ru" : "en";
    }
}
