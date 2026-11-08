package com.gmail.heagoo.apkeditor;

import common.types.StringItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure-JVM model behind the translation comparison dialog. */
public final class TranslationComparison {
    public enum Status {
        MISSING,
        PLACEHOLDERS,
        EXTRA,
        OK
    }

    public static final class Row {
        public final String name;
        public final Status status;
        public final StringItem source;
        public final StringItem target;
        public final String sourceTokens;
        public final String targetTokens;

        Row(String name, Status status, StringItem source, StringItem target,
            String sourceTokens, String targetTokens) {
            this.name = name;
            this.status = status;
            this.source = source;
            this.target = target;
            this.sourceTokens = sourceTokens;
            this.targetTokens = targetTokens;
        }
    }

    public static final class Result {
        public final List<Row> rows;
        public final int sourceTotal;
        public final int targetTotal;
        public final int ok;
        public final int missing;
        public final int placeholderMismatches;
        public final int extra;
        public final boolean showAll;

        Result(List<Row> rows, int sourceTotal, int targetTotal, int ok, int missing,
               int placeholderMismatches, int extra, boolean showAll) {
            this.rows = Collections.unmodifiableList(rows);
            this.sourceTotal = sourceTotal;
            this.targetTotal = targetTotal;
            this.ok = ok;
            this.missing = missing;
            this.placeholderMismatches = placeholderMismatches;
            this.extra = extra;
            this.showAll = showAll;
        }

        public int present() {
            return sourceTotal - missing;
        }

        public int issues() {
            return missing + placeholderMismatches + extra;
        }
    }

    private TranslationComparison() {
    }

    public static Result compare(List<StringItem> source, List<StringItem> target, boolean showAll) {
        Map<String, StringItem> sourceMap = indexed(source);
        Map<String, StringItem> targetMap = indexed(target);
        List<Row> rows = new ArrayList<>();
        int ok = 0;
        int missing = 0;
        int mismatches = 0;
        int extra = 0;
        for (Map.Entry<String, StringItem> entry : sourceMap.entrySet()) {
            String name = entry.getKey();
            StringItem from = entry.getValue();
            StringItem to = targetMap.get(name);
            String fromTokens = LocalizationChecks.formatTokens(LocalizationChecks.tokens(from.value));
            if (to == null) {
                missing++;
                rows.add(new Row(name, Status.MISSING, from, null, fromTokens, "нет"));
            } else {
                String toTokens = LocalizationChecks.formatTokens(LocalizationChecks.tokens(to.value));
                if (!fromTokens.equals(toTokens)) {
                    mismatches++;
                    rows.add(new Row(name, Status.PLACEHOLDERS, from, to, fromTokens, toTokens));
                } else {
                    ok++;
                    if (showAll) {
                        rows.add(new Row(name, Status.OK, from, to, fromTokens, toTokens));
                    }
                }
            }
        }
        for (Map.Entry<String, StringItem> entry : targetMap.entrySet()) {
            if (!sourceMap.containsKey(entry.getKey())) {
                extra++;
                StringItem to = entry.getValue();
                rows.add(new Row(entry.getKey(), Status.EXTRA, null, to, "нет",
                        LocalizationChecks.formatTokens(LocalizationChecks.tokens(to.value))));
            }
        }
        rows.sort(Comparator.comparingInt((Row row) -> row.status.ordinal())
                .thenComparing(row -> row.name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(row -> row.name));
        return new Result(rows, sourceMap.size(), targetMap.size(), ok, missing, mismatches, extra, showAll);
    }

    private static Map<String, StringItem> indexed(List<StringItem> items) {
        Map<String, StringItem> map = new LinkedHashMap<>();
        if (items != null) {
            for (StringItem item : items) {
                if (item != null && item.name != null) {
                    map.putIfAbsent(item.name, item);
                }
            }
        }
        return map;
    }

    public static String summary(Result result, String sourceQualifier, String targetQualifier) {
        Objects.requireNonNull(result, "result");
        StringBuilder text = new StringBuilder();
        text.append(ResourceLocales.label(sourceQualifier)).append(" → ")
                .append(ResourceLocales.label(targetQualifier)).append('\n');
        text.append(ResourceLocales.folder(sourceQualifier)).append(" → ")
                .append(ResourceLocales.folder(targetQualifier)).append("\n\n");
        text.append("Строк в оригинале: ").append(result.sourceTotal)
                .append("; в переводе: ").append(result.targetTotal).append('\n');
        text.append("Есть перевод: ").append(result.present()).append(" из ").append(result.sourceTotal);
        if (result.sourceTotal > 0) {
            text.append(" (").append(result.present() * 100 / result.sourceTotal).append("%)");
        }
        text.append('\n');
        text.append("Без замечаний: ").append(result.ok).append('\n');
        text.append("Нет перевода: ").append(result.missing).append('\n');
        text.append("Отличаются плейсхолдеры: ").append(result.placeholderMismatches).append('\n');
        text.append("Только в переводе: ").append(result.extra).append("\n\n");
        text.append(result.showAll ? "Показаны все строки." : "Показаны только замечания.");
        if (result.rows.isEmpty()) {
            text.append(" Замечаний нет.");
        } else {
            text.append(" Нажмите на строку, чтобы добавить или изменить перевод.");
        }
        text.append("\n\nПроверяются строковые ресурсы и простые плейсхолдеры (%s, %d, %1$s…). ")
                .append("Plurals, массивы и нестандартные форматы не проверяются.");
        return text.toString();
    }
}
