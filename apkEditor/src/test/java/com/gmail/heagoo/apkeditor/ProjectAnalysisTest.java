package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Regression for project diagnostics reporting zero files on a decoded multi-DEX project. */
public class ProjectAnalysisTest {
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

    @Test(timeout = 15000)
    public void discoversManifestResourcesAndEverySmaliRoot() throws Exception {
        File root = temporary.getRoot();
        write("AndroidManifest.xml", "<manifest package=\"test.app\"/>\n");
        write("res/values/strings.xml", "<resources><string name=\"app_name\">Test</string></resources>\n");
        write("res/layout/main.xml", "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                + "android:text=\"@string/app_name\" android:id=\"@+id/title\"/>\n");
        write("smali/test/First.smali", validSmali("First"));
        write("smali_classes2/test/Second.smali", validSmali("Second"));
        write("classes.dex", "not text");
        write("classes2.dex", "not text either");

        ProjectAnalysis.Report report = ProjectAnalysis.diagnose(root);

        assertEquals(3, report.xmlFound);
        assertEquals(2, report.smaliFound);
        assertEquals(5, report.checked);
        assertEquals(3, report.xmlChecked);
        assertEquals(2, report.smaliChecked);
        assertEquals(0, report.skipped);
        assertFalse(report.truncated);
        assertFalse(report.resourceIndexIncomplete);
        assertTrue("valid project should have no findings: " + report.findings, report.findings.isEmpty());
    }

    @Test(timeout = 15000)
    public void reportsXmlSyntaxErrorsWithoutSkippingValidXml() throws Exception {
        write("res/values/valid.xml", "<resources><string name=\"ok\">Good</string></resources>\n");
        write("res/values/broken.xml", "<resources><string name=\"broken\">Bad</resources>\n");

        ProjectAnalysis.Report report = ProjectAnalysis.diagnose(temporary.getRoot());

        assertEquals(2, report.xmlFound);
        assertEquals(2, report.xmlChecked);
        assertEquals(0, report.skipped);
        assertTrue(hasFinding(report, "res/values/broken.xml", "XML:"));
        assertFalse(hasFinding(report, "res/values/valid.xml", "XML не проверен"));
    }

    @Test(timeout = 15000)
    public void skipsBinaryXmlAndOversizedTextInsteadOfCountingThemAsChecked() throws Exception {
        File binary = new File(temporary.getRoot(), "AndroidManifest.xml");
        Files.write(binary.toPath(), new byte[]{3, 0, 8, 0, 0, 0, 0, 0});
        File large = new File(temporary.getRoot(), "smali/test/Large.smali");
        assertTrue(large.getParentFile().mkdirs());
        byte[] bytes = new byte[ProjectAnalysis.MAX_TEXT_BYTES + 1];
        java.util.Arrays.fill(bytes, (byte) ' ');
        Files.write(large.toPath(), bytes);

        ProjectAnalysis.Report report = ProjectAnalysis.diagnose(temporary.getRoot());

        assertEquals(1, report.xmlFound);
        assertEquals(1, report.smaliFound);
        assertEquals(0, report.checked);
        assertEquals(2, report.skipped);
        assertTrue(hasFinding(report, "AndroidManifest.xml", "бинарная кодировка"));
        assertTrue(hasFinding(report, "smali/test/Large.smali", "размер больше 4 МБ"));
    }

    @Test(timeout = 15000)
    public void structuralSmaliCheckFindsUnclosedMethodWhenFullParserIsUnavailable() throws Exception {
        write("smali/test/Broken.smali", ".class public Ltest/Broken;\n"
                + ".super Ljava/lang/Object;\n"
                + ".method public static broken()V\n"
                + "    return-void\n");

        ProjectAnalysis.Report report = ProjectAnalysis.diagnose(temporary.getRoot());

        assertEquals(1, report.smaliFound);
        assertEquals(1, report.smaliChecked);
        assertTrue(report.smaliSyntaxUnavailable);
        assertTrue(hasFinding(report, "smali/test/Broken.smali", "Метод не закрыт .end method"));
    }

    @Test(timeout = 15000)
    public void resourceSearchCoversXmlSmaliJavaAndKotlin() throws Exception {
        write("res/layout/main.xml", "<TextView android:text=\"@string/app_name\"/>\n");
        write("smali/test/Use.smali", "sget v0, R$string;->app_name:I\n");
        write("src/Use.java", "int id = R.string.app_name;\n");
        write("src/Use.kt", "val id = R.string.app_name\n");
        write("src/Other.java", "int id = R.string.other;\n");

        ProjectAnalysis.Report report = ProjectAnalysis.resourceUses(temporary.getRoot(), "string/app_name");

        assertEquals(5, report.checked);
        assertEquals(4, report.findings.size());
        assertTrue(hasFinding(report, "res/layout/main.xml", "@string/app_name"));
        assertTrue(hasFinding(report, "smali/test/Use.smali", "R$string;->app_name"));
        assertTrue(hasFinding(report, "src/Use.java", "R.string.app_name"));
        assertTrue(hasFinding(report, "src/Use.kt", "R.string.app_name"));
    }

    @Test(timeout = 15000)
    public void cancellationStopsBeforeFilesAreChecked() throws Exception {
        write("AndroidManifest.xml", "<manifest/>\n");
        try {
            ProjectAnalysis.diagnose(temporary.getRoot(), null, () -> true);
            fail("expected cancellation");
        } catch (InterruptedIOException expected) {
            assertTrue(expected.getMessage().contains("отменена"));
        }
    }

    @Test(timeout = 15000)
    public void progressReachesTheSelectedFileTotal() throws Exception {
        write("AndroidManifest.xml", "<manifest/>\n");
        write("smali/test/One.smali", validSmali("One"));
        write("smali_classes2/test/Two.smali", validSmali("Two"));
        AtomicInteger done = new AtomicInteger(-1);
        AtomicInteger total = new AtomicInteger(-1);
        ProjectAnalysis.diagnose(temporary.getRoot(), (stage, current, count, path) -> {
            if ("Проверка XML и smali".equals(stage)) {
                done.set(current);
                total.set(count);
            }
        }, () -> false);
        assertEquals(3, total.get());
        assertEquals(3, done.get());
    }

    @Test(timeout = 15000)
    public void smaliApiUsesNearestMarkerAndFallsBackForInvalidValues() throws Exception {
        File root = temporary.getRoot();
        write("smali/.apkeditor-dex-api", "28\n");
        File first = write("smali/test/First.smali", validSmali("First"));
        File second = write("smali_classes2/test/Second.smali", validSmali("Second"));
        write("smali_classes2/.apkeditor-dex-api", "1001\n");
        File third = write("smali_classes3/test/Third.smali", validSmali("Third"));
        write("smali_classes3/.apkeditor-dex-api", "not-a-number\n");

        assertEquals(28, ProjectAnalysis.smaliApiFor(root, first));
        assertEquals(ProjectAnalysis.DEFAULT_SMALI_API, ProjectAnalysis.smaliApiFor(root, second));
        assertEquals(ProjectAnalysis.DEFAULT_SMALI_API, ProjectAnalysis.smaliApiFor(root, third));
    }

    @Test(timeout = 15000)
    public void missingProjectRootFailsInsteadOfReturningAnEmptySuccessReport() throws Exception {
        File missing = new File(temporary.getRoot(), "missing");
        try {
            ProjectAnalysis.diagnose(missing);
            fail("expected IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("недоступен"));
        }
    }

    @Test(timeout = 15000)
    public void resourceReferencesAreMatchedAgainstTypedItemsAndPlusIds() throws Exception {
        write("res/values/defs.xml", "<resources>\n"
                + "    <item type=\"string\" name=\"typed_name\">Typed</item>\n"
                + "    <public type=\"id\" name=\"public_id\" id=\"0x7f010000\"/>\n"
                + "</resources>\n");
        write("res/layout/main.xml", "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                + "android:text=\"@string/typed_name\" android:id=\"@id/public_id\"/>\n");

        ProjectAnalysis.Report report = ProjectAnalysis.diagnose(temporary.getRoot());

        assertEquals(2, report.xmlChecked);
        assertTrue(report.findings.toString(), report.findings.isEmpty());
    }
}
