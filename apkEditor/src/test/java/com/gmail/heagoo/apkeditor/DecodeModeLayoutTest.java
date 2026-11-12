package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class DecodeModeLayoutTest {
    @Test public void rowsMatchEditModeCardsInBothLayouts() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle"))) root = root.getParent();
        assertNotNull(root);
        for (String module : new String[]{"apkEditor", "apkEditorPlus"}) {
            String xml = new String(Files.readAllBytes(root.resolve(module + "/src/main/res/layout/dlg_decodemode.xml")), StandardCharsets.UTF_8);
            assertTrue(xml.contains("android:background=\"@drawable/lab_dialog_surface\""));
            assertTrue(xml.contains("android:padding=\"8dp\""));
            assertEquals(3, xml.split("\\?labCardBackground", -1).length - 1);
            assertEquals(3, xml.split("android:layout_marginTop=\"3dp\"", -1).length - 1);
            assertEquals(3, xml.split("android:layout_marginBottom=\"3dp\"", -1).length - 1);
            assertFalse(xml.contains("android:clipToOutline"));
            assertTrue(xml.contains("@+id/decode_all_files"));
            assertTrue(xml.contains("@+id/decode_partial_files"));
            assertTrue(xml.contains("@+id/cancel"));
        }
    }
}
