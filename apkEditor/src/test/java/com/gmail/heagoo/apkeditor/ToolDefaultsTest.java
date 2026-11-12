package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ToolDefaultsTest {
    @Test public void newestCompatibleAaptForEachSupportedAndroid() {
        assertEquals("legacy", ToolDefaults.aaptForSdk(26));
        assertEquals("legacy", ToolDefaults.aaptForSdk(27));
        assertEquals("aapt2", ToolDefaults.aaptForSdk(28));
        assertEquals("aapt2", ToolDefaults.aaptForSdk(29));
        assertEquals("aapt2-37", ToolDefaults.aaptForSdk(30));
        assertEquals("aapt2-37", ToolDefaults.aaptForSdk(37));
    }
}
