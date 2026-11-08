package com.gmail.heagoo.apkeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns raw diagnostics into a short, human-readable report. Raw messages stay available separately. */
public final class AnalysisReportFormatter {
    public static final class Summary {
        public final String headline;
        public final String body;
        public final boolean positive;

        Summary(String headline, String body, boolean positive) {
            this.headline = headline;
            this.body = body;
            this.positive = positive;
        }
    }

    public static final class Entry {
        public final ProjectAnalysis.Finding finding;
        public final String label;
        public final String title;
        public final String explanation;
        public final String action;
        public final String location;

        Entry(ProjectAnalysis.Finding finding, String label, String title, String explanation,
              String action, String location) {
            this.finding = finding;
            this.label = label;
            this.title = title;
            this.explanation = explanation;
            this.action = action;
            this.location = location;
        }
    }

    private AnalysisReportFormatter() {
    }

    public static Summary summarize(ProjectAnalysis.Report report, boolean resourceSearch) {
        String checked = "Проверено файлов: " + count(report.checked)
                + " (XML: " + count(report.xmlChecked) + ", smali: " + count(report.smaliChecked) + ").";
        if (resourceSearch) {
            String headline = report.checked == 0 ? "Нечего проверять"
                    : report.matches == 0 ? "Совпадений не найдено"
                    : "Найдено совпадений: " + count(report.matches);
            StringBuilder body = new StringBuilder(checked);
            if (report.matches > 0) {
                body.append("\nНиже показаны файлы и строки, где встречается ресурс.");
            } else if (report.checked > 0) {
                body.append("\nВ проверенных файлах этот ресурс не встречается.");
            }
            appendLimits(body, report);
            body.append("\nПоиск идёт по сохранённым файлам на диске; динамические ссылки и числовые ID могут не найтись.");
            return new Summary(headline, body.toString(), report.checked > 0 && report.matches == 0 && !limited(report));
        }
        if (report.xmlFound + report.smaliFound == 0) {
            return new Summary("Нечего проверять",
                    "В проекте не найдено XML- или smali-файлов. Откройте распакованный проект и повторите проверку.", false);
        }
        String headline;
        boolean positive = false;
        if (report.errors > 0) {
            headline = "Найдены ошибки: " + count(report.errors);
        } else if (limited(report)) {
            headline = "Проверка завершена не полностью";
        } else if (report.warnings > 0) {
            headline = "Явных ошибок нет, есть предупреждения";
        } else {
            headline = "Ошибок не найдено";
            positive = true;
        }
        StringBuilder body = new StringBuilder(checked);
        body.append("\nОшибки: ").append(count(report.errors))
                .append(" · Предупреждения: ").append(count(report.warnings))
                .append(" · Не проверено: ").append(count(report.notChecked));
        if (report.errors > 0) {
            body.append("\nСначала исправьте ошибки, затем повторите проверку.");
        } else if (report.warnings > 0) {
            body.append("\nПредупреждения — возможные проблемы, а не подтверждённые ошибки приложения.");
        } else if (positive) {
            body.append("\nПроверенные XML-файлы и smali не содержат найденных проблем.");
        }
        appendLimits(body, report);
        body.append("\nПроверяются сохранённые файлы; несохранённые изменения форм могут не учитываться.");
        body.append("\nЭто предварительная проверка. Окончательно результат подтверждает сборка APK.");
        return new Summary(headline, body.toString(), positive);
    }

    public static List<Entry> entries(ProjectAnalysis.Report report, boolean resourceSearch) {
        List<Entry> entries = new ArrayList<>();
        for (ProjectAnalysis.Finding finding : report.findings) {
            entries.add(entry(finding, resourceSearch));
        }
        return entries;
    }

    public static Entry entry(ProjectAnalysis.Finding finding, boolean resourceSearch) {
        String message = finding.message == null ? "" : finding.message;
        String location = finding.path + (finding.line > 0 ? " · строка " + finding.line : "");
        if (resourceSearch || finding.kind == ProjectAnalysis.FindingKind.MATCH) {
            return new Entry(finding, "СОВПАДЕНИЕ", "Ресурс встречается здесь",
                    "В этой строке найдено упоминание искомого ресурса.",
                    "Откройте файл, чтобы посмотреть строку в контексте.", location);
        }
        if (message.startsWith("Возможно отсутствует ресурс")) {
            String resource = message.substring("Возможно отсутствует ресурс ".length());
            int suffix = resource.indexOf(" (");
            if (suffix >= 0) {
                resource = resource.substring(0, suffix);
            }
            return new Entry(finding, "ПРЕДУПРЕЖДЕНИЕ", "Возможно, нет ресурса " + resource,
                    "XML ссылается на ресурс, который не найден среди проверенных определений. "
                            + "Это приблизительная проверка: ресурс может появляться при сборке или задаваться иначе.",
                    "Если приложение работает, не меняйте файл только из-за этого предупреждения. "
                            + "При проблеме проверьте, существует ли ресурс в проекте.", location);
        }
        if (message.startsWith("XML:")) {
            return new Entry(finding, "ОШИБКА", "Ошибка в XML-файле",
                    "Файл содержит некорректную XML-разметку: например, незакрытый тег или недопустимый символ.",
                    "Откройте указанную строку, исправьте разметку и повторите проверку.", location);
        }
        if (message.startsWith("smali:")) {
            return new Entry(finding, "ОШИБКА", "Ошибка в smali-коде",
                    "Проверка нашла некорректную инструкцию или запись в коде приложения.",
                    "Откройте указанную строку и исправьте инструкцию. После правки повторите проверку.", location);
        }
        if (message.contains(".end method") || message.contains(".method") || message.contains(".class")) {
            return new Entry(finding, "ОШИБКА", "Нарушена структура smali-файла",
                    "В файле пропущена обязательная директива класса/метода или нарушен их порядок.",
                    "Откройте файл и проверьте парность .method / .end method и наличие .class.", location);
        }
        if (finding.kind == ProjectAnalysis.FindingKind.NOT_CHECKED) {
            String explanation;
            String action;
            if (message.contains("размер больше")) {
                explanation = "Файл слишком большой для встроенной проверки (лимит 4 МБ).";
                action = "Проверьте файл внешним инструментом или разделите его, если это возможно.";
            } else if (message.contains("бинарная кодировка")) {
                explanation = "Это бинарный XML, а проверка читает только текстовый XML.";
                action = "Сначала раскодируйте ресурсы проекта, затем повторите проверку.";
            } else if (message.contains("DTD")) {
                explanation = "XML использует DTD-определения, которые встроенная проверка не поддерживает.";
                action = "Проверьте такой файл внешним XML-инструментом.";
            } else {
                explanation = "Файл не удалось прочитать или разобрать, поэтому проблема в нём не подтверждена и не исключена.";
                action = "Проверьте доступность файла и повторите диагностику.";
            }
            return new Entry(finding, "НЕ ПРОВЕРЕНО", "Файл пропущен проверкой", explanation, action, location);
        }
        return new Entry(finding, "ОШИБКА", "Найдена проблема в файле",
                "Проверка сообщила о проблеме в этом файле.",
                "Откройте файл, исправьте проблему и повторите проверку.", location);
    }

    public static String technicalDetails(ProjectAnalysis.Finding finding) {
        return finding.toString();
    }

    private static boolean limited(ProjectAnalysis.Report report) {
        return report.skipped > 0 || report.smaliNotParsed > 0 || report.smaliSyntaxUnavailable
                || report.fileLimitReached || report.findingsTruncated || report.resourceIndexIncomplete;
    }

    private static void appendLimits(StringBuilder body, ProjectAnalysis.Report report) {
        if (report.smaliSyntaxUnavailable || report.smaliNotParsed > 0) {
            body.append("\nЧасть smali проверена только по структуре: полный разбор синтаксиса был недоступен для ")
                    .append(count(report.smaliNotParsed)).append(" файлов.");
        }
        if (report.skipped > 0) {
            body.append("\nПропущено файлов или папок: ").append(count(report.skipped)).append('.');
        }
        if (report.fileLimitReached) {
            body.append("\nДостигнут лимит числа файлов; проверена только часть проекта.");
        }
        if (report.findingsTruncated) {
            body.append("\nПоказаны не все замечания: список ограничен ").append(count(ProjectAnalysis.MAX_FINDINGS))
                    .append(" строками. Всего замечаний: ").append(count(report.totalFindings)).append('.');
        }
        if (report.resourceIndexIncomplete && !report.fileLimitReached) {
            body.append("\nИндекс ресурсов неполный, поэтому предупреждения о ссылках могут быть неточными.");
        }
    }

    private static String count(int value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }
}
