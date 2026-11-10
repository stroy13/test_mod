package com.gmail.heagoo.apkeditor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class ProjectSaveRegressionTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void failedWriteLeavesOriginalProjectOrManifestIntact() throws Exception {
        File file = folder.newFile("AndroidManifest.xml");
        AtomicProjectFile.writeText(file, "<manifest package=\"old\"/>");
        try {
            AtomicProjectFile.replace(file, temp -> {
                Files.write(temp.toPath(), "partial".getBytes(StandardCharsets.UTF_8));
                throw new IOException("test failure");
            });
            fail("expected write failure");
        } catch (IOException expected) { }
        assertEquals("<manifest package=\"old\"/>", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        AtomicProjectFile.writeText(file, "<manifest package=\"новый\"/>");
        assertEquals("<manifest package=\"новый\"/>", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        assertEquals(1, folder.getRoot().listFiles().length);
    }

    @Test public void copyPreservesTimeUsedForBuildCacheDecisions() throws Exception {
        File input = folder.newFile("source");
        Files.write(input.toPath(), new byte[]{1, 2, 3});
        assertTrue(input.setLastModified(1600000000000L));
        File dest = new File(folder.getRoot(), "target");
        CheckpointFiles.copy(input, dest);
        assertEquals(input.lastModified(), dest.lastModified());
        assertArrayEquals(Files.readAllBytes(input.toPath()), Files.readAllBytes(dest.toPath()));
    }

    @Test public void projectButtonExistsInAllOrientationsAndExitUsesSave() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle"))) root = root.getParent();
        assertNotNull(root);
        for (String module : new String[]{"apkEditor", "apkEditorPlus"}) {
            for (String layout : new String[]{"layout", "layout-land"}) {
                String xml = new String(Files.readAllBytes(root.resolve(module + "/src/main/res/" + layout + "/activity_apkinfo.xml")), StandardCharsets.UTF_8);
                assertTrue(xml.contains("/project_save_button\""));
                assertTrue(xml.contains("@string/lab_save_project"));
            }
        }
        String activity = new String(Files.readAllBytes(root.resolve("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ApkInfoActivity.java")), StandardCharsets.UTF_8);
        assertTrue(activity.contains("saveProject(true)"));
        assertTrue(activity.contains("saveProject(false)"));
        assertTrue(activity.contains("AtomicProjectFile.writeObject(new File(infoPath), prjInfo)"));
    }

    @Test public void selfClosedRootTagIsNotTruncated() {
        LineRecord root = new LineRecord(0, "<application/>", "\t");
        root.sectionStart = 0; root.sectionEnd = 0;
        assertEquals("application", root.getSectionTag());
    }
}
