package com.gmail.heagoo.apkeditor.util;
import static org.junit.Assert.*;
import org.junit.Test;
public class ApkFileNameTest {
    @Test public void acceptsApkCaseInsensitively() {
        assertTrue(ApkFileName.isApk("/folder/test.apk"));
        assertTrue(ApkFileName.isApk("/folder/test.APK"));
        assertTrue(ApkFileName.isApk("test.ApK"));
        assertFalse(ApkFileName.isApk("test.apk.zip"));
        assertFalse(ApkFileName.isApk(null));
    }
}
