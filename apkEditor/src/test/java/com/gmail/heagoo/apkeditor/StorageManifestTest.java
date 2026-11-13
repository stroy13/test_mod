package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class StorageManifestTest {
    @Test public void android10LegacyStorageIsRequested() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle"))) root = root.getParent();
        assertNotNull(root);
        String manifest = new String(Files.readAllBytes(root.resolve("apkEditorPlus/src/main/AndroidManifest.xml")), StandardCharsets.UTF_8);
        assertTrue(manifest.contains("android:requestLegacyExternalStorage=\"true\""));
        assertTrue(manifest.contains("android.permission.WRITE_EXTERNAL_STORAGE"));
        assertTrue(manifest.contains("android.permission.READ_EXTERNAL_STORAGE"));
        for (String module : new String[]{"apkEditor", "apkEditorPlus"}) {
            String layout = new String(Files.readAllBytes(root.resolve(module + "/src/main/res/layout/activity_listfile.xml")), StandardCharsets.UTF_8);
            assertTrue(layout.contains("file_list_status"));
        }
    }
}
