package com.gmail.heagoo.apkeditor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class ManifestInlineEditorTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void savesExactSourceWithoutReformatting() throws Exception {
        File manifest = folder.newFile("AndroidManifest.xml");
        String original = "<?xml version=\"1.0\"?>\n<manifest  package=\"ru.test\">\n<!--  keep spaces  -->\n<application />\n</manifest>\n";
        AtomicProjectFile.writeText(manifest, original);
        String edited = original.replace("ru.test", "ru.example");
        ManifestDocumentFile.save(manifest, original, edited);
        assertEquals(edited, ManifestDocumentFile.read(manifest));
    }

    @Test public void externalChangeIsNotOverwritten() throws Exception {
        File manifest = folder.newFile("AndroidManifest.xml");
        AtomicProjectFile.writeText(manifest, "old");
        AtomicProjectFile.writeText(manifest, "external");
        try {
            ManifestDocumentFile.save(manifest, "old", "draft");
            fail("expected conflict");
        } catch (ManifestDocumentFile.Conflict expected) { }
        assertEquals("external", ManifestDocumentFile.read(manifest));
        ManifestDocumentFile.save(manifest, "old", "external");
        assertEquals("external", ManifestDocumentFile.read(manifest));
    }

    @Test public void editorIsInlineAndProjectButtonHiddenOnManifestTab() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle"))) root = root.getParent();
        assertNotNull(root);
        String src = new String(Files.readAllBytes(root.resolve("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ApkInfoActivity.java")), StandardCharsets.UTF_8);
        assertTrue(src.contains("new ManifestInlineEditor(this, headerColors)"));
        assertTrue(src.contains("this.curSelectedRadio == 2 ? View.GONE : View.VISIBLE"));
        assertTrue(src.contains("ManifestDocumentFile.save(file, this.manifestBaseline, text)"));
        assertTrue(src.contains("saveInlineManifest(true)"));
        String widget = new String(Files.readAllBytes(root.resolve("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ManifestInlineEditor.java")), StandardCharsets.UTF_8);
        assertTrue(widget.contains("extends AppCompatEditText"));
        assertTrue(widget.contains("layout.getLineBaseline(line)"));
    }
}
