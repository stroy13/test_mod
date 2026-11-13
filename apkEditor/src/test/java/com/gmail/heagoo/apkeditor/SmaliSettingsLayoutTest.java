package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class SmaliSettingsLayoutTest {
    @Test public void toolSelectorsAreAdjacentInBothResources() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle"))) root = root.getParent();
        assertNotNull(root);
        for (String module : new String[]{"apkEditor", "apkEditorPlus"}) {
            String xml = new String(Files.readAllBytes(root.resolve(module + "/src/main/res/xml/settings.xml")), StandardCharsets.UTF_8);
            int decoder = xml.indexOf("android:key=\"SmaliDecoderVersion\"");
            int encoder = xml.indexOf("android:key=\"SmaliEncoderVersion\"");
            assertTrue(decoder >= 0 && encoder > decoder);
            String between = xml.substring(xml.indexOf("/>", decoder) + 2, xml.lastIndexOf("<", encoder)).trim();
            assertTrue(between.isEmpty());
            assertFalse(xml.substring(decoder, encoder).contains("</PreferenceCategory>"));
        }
    }
    @Test public void appOverrideKeepsSettingsInSyncWithLibrary() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle"))) root = root.getParent();
        assertNotNull(root);
        String library = new String(Files.readAllBytes(root.resolve("apkEditor/src/main/res/xml/settings.xml")), StandardCharsets.UTF_8);
        String app = new String(Files.readAllBytes(root.resolve("apkEditorPlus/src/main/res/xml/settings.xml")), StandardCharsets.UTF_8);
        assertEquals("App resource overrides the library settings; keep both in sync", library, app);
        assertTrue(app.contains("android:key=\"NotificationSettings\""));
        assertTrue(app.contains("android:key=\"BuildLogLanguage\""));
        assertFalse(app.contains("android:key=\"LocalAiModels\""));
    }
}
