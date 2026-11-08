package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The first screen must be understandable without reading parser diagnostics. */
public class AnalysisReportFormatterTest {
    private static ProjectAnalysis.Report report(int checked, int xml, int smali) {
        ProjectAnalysis.Report report = new ProjectAnalysis.Report();
        report.checked = checked;
        report.xmlChecked = xml;
        report.smaliChecked = smali;
        report.xmlFound = xml;
        report.smaliFound = smali;
        return report;
    }

    @Test
    public void cleanLargeProjectHasAShortPositiveSummary() {
        ProjectAnalysis.Report report = report(17497, 332, 17165);

        AnalysisReportFormatter.Summary summary = AnalysisReportFormatter.summarize(report, false);

        assertEquals("Ошибок не найдено", summary.headline);
        assertTrue(summary.positive);
        assertTrue(summary.body.contains("17 497"));
        assertTrue(summary.body.contains("XML: 332"));
        assertTrue(summary.body.contains("smali: 17 165"));
        assertFalse(summary.body.contains("res/raw"));
        assertFalse(summary.body.contains("Возможно отсутствует"));
    }

    @Test
    public void warningsAreNotPresentedAsConfirmedAppErrors() {
        ProjectAnalysis.Report report = report(1, 1, 0);
        report.add("res/layout/main.xml", 3, "Возможно отсутствует ресурс @string/missing (проверка приблизительная)");

        AnalysisReportFormatter.Summary summary = AnalysisReportFormatter.summarize(report, false);
        AnalysisReportFormatter.Entry entry = AnalysisReportFormatter.entries(report, false).get(0);

        assertEquals("Явных ошибок нет, есть предупреждения", summary.headline);
        assertFalse(summary.positive);
        assertEquals("ПРЕДУПРЕЖДЕНИЕ", entry.label);
        assertTrue(entry.title.contains("@string/missing"));
        assertTrue(entry.explanation.contains("приблизительная проверка"));
        assertTrue(entry.action.contains("не меняйте"));
        assertFalse(entry.explanation.contains("проверка приблизительная"));
        assertTrue(AnalysisReportFormatter.technicalDetails(entry.finding).contains("проверка приблизительная"));
    }

    @Test
    public void errorsAndSkippedFilesHaveDistinctActions() {
        ProjectAnalysis.Report report = report(1, 1, 1);
        report.add("res/values/broken.xml", 2, "XML: mismatched tag");
        report.add("AndroidManifest.xml", 0, "XML не проверен: бинарная кодировка");

        AnalysisReportFormatter.Summary summary = AnalysisReportFormatter.summarize(report, false);
        java.util.List<AnalysisReportFormatter.Entry> entries = AnalysisReportFormatter.entries(report, false);

        assertEquals("Найдены ошибки: 1", summary.headline);
        assertEquals("ОШИБКА", entries.get(0).label);
        assertEquals("НЕ ПРОВЕРЕНО", entries.get(1).label);
        assertTrue(entries.get(0).action.contains("исправьте"));
        assertTrue(entries.get(1).explanation.contains("бинарный XML"));
    }

    @Test
    public void partialSmaliCheckIsNotReportedAsFullSuccess() {
        ProjectAnalysis.Report report = report(2, 0, 2);
        report.smaliNotParsed = 2;
        report.smaliSyntaxUnavailable = true;

        AnalysisReportFormatter.Summary summary = AnalysisReportFormatter.summarize(report, false);

        assertEquals("Проверка завершена не полностью", summary.headline);
        assertFalse(summary.positive);
        assertTrue(summary.body.contains("только по структуре"));
    }

    @Test
    public void resourceSearchSummaryCountsMatchesNotErrors() {
        ProjectAnalysis.Report report = report(2, 1, 1);
        report.add("res/layout/main.xml", 1, "<TextView android:text=\"@string/app_name\"/>", ProjectAnalysis.FindingKind.MATCH);

        AnalysisReportFormatter.Summary summary = AnalysisReportFormatter.summarize(report, true);
        AnalysisReportFormatter.Entry entry = AnalysisReportFormatter.entries(report, true).get(0);

        assertEquals("Найдено совпадений: 1", summary.headline);
        assertEquals("СОВПАДЕНИЕ", entry.label);
        assertEquals(0, report.errors);
    }
}
