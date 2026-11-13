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
        if (!"ru".equals(language) || line == null) return line;
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
