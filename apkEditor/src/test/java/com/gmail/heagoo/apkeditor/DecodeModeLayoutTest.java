package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class DecodeModeLayoutTest {
    @Test public void rowsShareOneRoundedSurfaceInBothLayouts() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle"))) root = root.getParent();
        assertNotNull(root);
        for (String module : new String[]{"apkEditor", "apkEditorPlus"}) {
            String xml = new String(Files.readAllBytes(root.resolve(module + "/src/main/res/layout/dlg_decodemode.xml")), StandardCharsets.UTF_8);
            assertTrue(xml.contains("android:background=\"@drawable/lab_dialog_surface\""));
            assertTrue(xml.contains("android:clipToOutline=\"true\""));
            assertFalse(xml.contains("?labCardBackground"));
            assertTrue(xml.contains("?android:attr/selectableItemBackground"));
            assertTrue(xml.contains("@+id/decode_all_files"));
            assertTrue(xml.contains("@+id/decode_partial_files"));
            assertTrue(xml.contains("@+id/cancel"));
        }
    }
}
