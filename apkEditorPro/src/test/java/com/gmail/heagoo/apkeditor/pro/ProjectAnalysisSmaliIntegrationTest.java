package com.gmail.heagoo.apkeditor.pro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.gmail.heagoo.apkeditor.ProjectAnalysis;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Runs project diagnostics against the real Pro smali parser, not only the structural fallback. */
public class ProjectAnalysisSmaliIntegrationTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    private File write(String relative, String text) throws IOException {
        File file = new File(temporary.getRoot(), relative);
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String validSmali(String name) {
        return ".class public Ltest/" + name + ";\n"
                + ".super Ljava/lang/Object;\n"
                + ".method public constructor <init>()V\n"
                + "    .registers 1\n"
                + "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n"
                + "    return-void\n"
                + ".end method\n";
    }

    private static boolean hasFinding(ProjectAnalysis.Report report, String path, String fragment) {
        for (ProjectAnalysis.Finding finding : report.findings) {
            if (finding.path.equals(path) && finding.message.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    @Test(timeout = 30000)
    public void fullParserChecksEverySmaliRootAndValidXml() throws Exception {
        write("AndroidManifest.xml", "<manifest package=\"test.app\"/>\n");
        write("smali/test/First.smali", validSmali("First"));
        write("smali_classes2/test/Second.smali", validSmali("Second"));
        write("smali/.apkeditor-dex-api", "28\n");
        write("smali_classes2/.apkeditor-dex-api", "28\n");

        ProjectAnalysis.Report report = ProjectAnalysis.diagnose(temporary.getRoot());

        assertEquals(1, report.xmlFound);
        assertEquals(2, report.smaliFound);
        assertEquals(3, report.checked);
        assertEquals(1, report.xmlChecked);
        assertEquals(2, report.smaliChecked);
        assertEquals(0, report.skipped);
        assertEquals(0, report.smaliNotParsed);
        assertFalse(report.smaliSyntaxUnavailable);
        assertTrue(report.findings.toString(), report.findings.isEmpty());
    }

    @Test(timeout = 30000)
    public void fullParserReportsSmaliSyntaxErrorsWithFileAndLine() throws Exception {
        write("smali_classes2/test/Broken.smali", ".class public Ltest/Broken;\n"
                + ".super Ljava/lang/Object;\n"
                + ".method public static broken()V\n"
                + "    .registers 1\n"
                + "    const/4 v0,\n"
                + ".end method\n");

        ProjectAnalysis.Report report = ProjectAnalysis.diagnose(temporary.getRoot());

        assertEquals(1, report.smaliFound);
        assertEquals(1, report.smaliChecked);
        assertEquals(0, report.smaliNotParsed);
        assertFalse(report.smaliSyntaxUnavailable);
        assertTrue(report.findings.toString(), hasFinding(report, "smali_classes2/test/Broken.smali", "smali:"));
        boolean lined = false;
        for (ProjectAnalysis.Finding finding : report.findings) {
            if (finding.path.equals("smali_classes2/test/Broken.smali") && finding.line > 0) {
                lined = true;
            }
        }
        assertTrue("expected a line number for the smali error", lined);
    }


    @Test(timeout = 60000)
    public void fullParserHandlesLargeProjectWithMonotonicProgressAndNoStructuralFallback() throws Exception {
        for (int i = 0; i < 120; i++) {
            String name = String.format(java.util.Locale.ROOT, "Parallel%03d", i);
            write("smali_classes2/test/" + name + ".smali", validSmali(name));
        }
        write("smali_classes2/test/Broken.smali", ".class public Ltest/Broken;\n"
                + ".super Ljava/lang/Object;\n"
                + ".method public static broken()V\n"
                + "    .registers 1\n"
                + "    const/4 v0,\n"
                + ".end method\n");
        write("AndroidManifest.xml", "<manifest package=\"test.app\"/>\n");
        AtomicInteger lastDone = new AtomicInteger();
        AtomicInteger total = new AtomicInteger();

        ProjectAnalysis.Report report = ProjectAnalysis.diagnose(temporary.getRoot(),
                (stage, done, count, path) -> {
                    if ("Проверка XML и smali".equals(stage)) {
                        assertTrue(done >= lastDone.get());
                        lastDone.set(done);
                        total.set(count);
                    }
                }, () -> false);

        assertEquals(122, report.checked);
        assertEquals(121, report.smaliChecked);
        assertEquals(1, report.xmlChecked);
        assertEquals(0, report.smaliNotParsed);
        assertFalse(report.smaliSyntaxUnavailable);
        assertEquals(122, total.get());
        assertEquals(122, lastDone.get());
        assertTrue(hasFinding(report, "smali_classes2/test/Broken.smali", "smali:"));
    }

    @Test(timeout = 15000)
    public void fullParserStopsWhenSupplierIsCancelled() throws Exception {
        File file = write("smali/test/Cancelled.smali", validSmali("Cancelled"));
        try {
            SmaliCheck.check(file.getAbsolutePath(), 26, () -> true);
            fail("expected cancellation");
        } catch (InterruptedIOException expected) {
            assertTrue(expected.getMessage().contains("cancelled"));
        }
    }

    @Test(timeout = 30000)
    public void projectCancellationStopsParallelFullParser() throws Exception {
        for (int i = 0; i < 40; i++) {
            write("smali/test/Cancel" + i + ".smali", validSmali("Cancel" + i));
        }
        try {
            ProjectAnalysis.diagnose(temporary.getRoot(), null, () -> true);
            fail("expected cancellation");
        } catch (InterruptedIOException expected) {
            assertTrue(expected.getMessage().contains("отменена"));
        }
    }

    @Test(timeout = 30000)
    public void validSmaliStillPassesWhenDexApiMarkerIsMissing() throws Exception {
        write("smali/test/NoMarker.smali", validSmali("NoMarker"));

        ProjectAnalysis.Report report = ProjectAnalysis.diagnose(temporary.getRoot());

        assertEquals(1, report.smaliChecked);
        assertEquals(0, report.smaliNotParsed);
        assertFalse(report.smaliSyntaxUnavailable);
        assertTrue(report.findings.toString(), report.findings.isEmpty());
    }
}
