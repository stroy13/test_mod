package com.gmail.heagoo.apkeditor.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class ComposeBuildTraceTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    @Test public void keepsFullTimingAndNestedSigningInOneFileOnFailure() throws Exception {
        File target = new File(folder.getRoot(), "out.apk");
        ComposeBuildTrace trace = new ComposeBuildTrace(target, "full");
        trace.begin("smali change scan");
        trace.begin("compile resources");
        trace.logger().log("AAPT2 compile: 10 ms");
        trace.begin("sign and verify");
        trace.logger().log("SignHelper: nested");
        trace.result(false, false, "compile failed");
        trace.close();
        String log = new String(Files.readAllBytes(new File(folder.getRoot(), "build-log.txt").toPath()), StandardCharsets.UTF_8);
        assertTrue(log.contains("Compose stage start: smali change scan"));
        assertTrue(log.contains("Compose stage end: compile resources"));
        assertTrue(log.contains("AAPT2 compile: 10 ms"));
        assertTrue(log.contains("SignHelper: nested"));
        assertTrue(log.contains("Compose result: failed; compile failed"));
        assertTrue(log.contains("Total elapsed:"));
    }
}
