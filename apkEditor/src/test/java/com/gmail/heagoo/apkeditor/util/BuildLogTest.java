package com.gmail.heagoo.apkeditor.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class BuildLogTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void writesVisibleLogWithSectionsAndStackTrace() throws Exception {
        File file = new File(folder.getRoot(), "build-log.txt");
        BuildLog log = new BuildLog(file);
        log.section("Environment");
        log.key("Source APK", "/tmp/source.apk");
        log.map("Replaces", Collections.singletonMap("AndroidManifest.xml", "/tmp/manifest.xml"));
        log.log("merge AndroidManifest.xml");
        log.error(new IllegalStateException("boom", new RuntimeException("cause")));
        log.close();
        assertTrue(file.isFile());
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("APK Editor Plus build log"));
        assertTrue(text.contains("--- Environment ---"));
        assertTrue(text.contains("Source APK: /tmp/source.apk"));
        assertTrue(text.contains("AndroidManifest.xml <- /tmp/manifest.xml"));
        assertTrue(text.contains("merge AndroidManifest.xml"));
        assertTrue(text.contains("ERROR: java.lang.IllegalStateException: boom"));
        assertTrue(text.contains("Caused by: java.lang.RuntimeException: cause"));
        assertTrue(text.contains("Total elapsed:"));
    }
}
