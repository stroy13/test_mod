package com.gmail.heagoo.apkeditor.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class Aapt2ManifestCompatibilityTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    @Test public void retriesOnlyKnownValidationErrors() {
        String known = "AAPT2 link: /tmp/AndroidManifest.xml:37: error: unexpected element <queries> found in <manifest>.\n"
                + "/tmp/AndroidManifest.xml:70: error: unexpected element <profileable> found in <manifest><application>.\n"
                + "/tmp/AndroidManifest.xml:390: error: unexpected element <property> found in <manifest><application><service>.";
        assertTrue(Aapt2Pipeline.isKnownOldManifestValidationFailure(known));
        assertFalse(Aapt2Pipeline.isKnownOldManifestValidationFailure(known + "\nx:1: error: resource not found"));
        assertFalse(Aapt2Pipeline.isKnownOldManifestValidationFailure("error: unexpected element <other> found in <manifest>."));
        assertFalse(Aapt2Pipeline.safeRetryWarnings("x: warning: unexpected element <bad> found in <manifest>."));
        assertTrue(Aapt2Pipeline.safeRetryWarnings("x: warning: unexpected element <queries> found in <manifest>."));
    }
    @Test public void retryKeepsNewElementsInCompiledManifest() throws Exception {
        String sdk=System.getenv("ANDROID_SDK_ROOT"); if (sdk==null) sdk=System.getenv("ANDROID_HOME");
        if (System.getenv("CI")!=null) assertNotNull(sdk); Assume.assumeNotNull(sdk);
        File aapt=new File(sdk,"build-tools/37.0.0/aapt2"), framework=new File(sdk,"platforms/android-37.0/android.jar");
        Assume.assumeTrue(aapt.isFile() && framework.isFile());
        Path root=folder.newFolder("manifest-integration").toPath(), res=Files.createDirectory(root.resolve("res")), values=Files.createDirectory(res.resolve("values"));
        Files.write(values.resolve("strings.xml"),"<resources><string name=\"app_name\">Test</string></resources>".getBytes(StandardCharsets.UTF_8));
        String manifest="<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"com.example.test\">"
                + "<queries><package android:name=\"com.example.other\"/></queries><application android:label=\"@string/app_name\">"
                + "<profileable android:shell=\"true\"/><service android:name=\".TestService\"><property android:name=\"test.property\" android:value=\"hello\"/></service>"
                + "</application></manifest>";
        Path manifestPath=root.resolve("AndroidManifest.xml"); Files.write(manifestPath,manifest.getBytes(StandardCharsets.UTF_8));
        Path script=root.resolve("old-aapt2");
        String wrapper="#!/bin/sh\nif [ \"$1\" = link ]; then\n  case \" $* \" in *' --warn-manifest-validation '*) ;; *) echo 'AndroidManifest.xml:1: error: unexpected element <queries> found in <manifest>.' >&2; exit 1;; esac\nfi\nexec '"+aapt.getAbsolutePath()+"' \"$@\"\n";
        Files.write(script,wrapper.getBytes(StandardCharsets.UTF_8)); assertTrue(script.toFile().setExecutable(true));
        File output=root.resolve("out.apk").toFile(); List<String> logs=new ArrayList<>();
        Aapt2Pipeline.build(script.toString(),framework.toString(),res.toString(),manifestPath.toString(),output.getAbsolutePath(),logs::add);
        assertTrue(logs.toString(),logs.stream().anyMatch(s->s.contains("retrying with validation warnings")));
        Process p=new ProcessBuilder(aapt.toString(),"dump","xmltree",output.toString(),"--file","AndroidManifest.xml").redirectErrorStream(true).start();
        ByteArrayOutputStream b=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; try(InputStream in=p.getInputStream()){int n; while((n=in.read(buf))!=-1)b.write(buf,0,n);}
        String dump=b.toString("UTF-8"); assertEquals(dump,0,p.waitFor());
        assertTrue(dump,dump.contains("queries")); assertTrue(dump,dump.contains("profileable")); assertTrue(dump,dump.contains("property")); assertTrue(dump,dump.contains("test.property"));
        assertEquals(manifest,new String(Files.readAllBytes(manifestPath),StandardCharsets.UTF_8));
    }
}
