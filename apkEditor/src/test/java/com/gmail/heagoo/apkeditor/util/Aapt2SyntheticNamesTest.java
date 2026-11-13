package com.gmail.heagoo.apkeditor.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

public class Aapt2SyntheticNamesTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    @Test public void aliasesAreTemporaryAndCollisionFree() throws Exception {
        Path res = folder.newFolder("res").toPath(); Path values = Files.createDirectory(res.resolve("values"));
        Path drawable = Files.createDirectory(res.resolve("drawable"));
        String ledger = "<resources><public type=\"drawable\" name=\"$avd_hide__0\" id=\"0x7f010001\"/></resources>";
        Files.write(values.resolve("public.xml"), ledger.getBytes(StandardCharsets.UTF_8));
        Files.write(drawable.resolve("$avd_hide__0.xml"), "<shape/>".getBytes(StandardCharsets.UTF_8));
        Files.write(values.resolve("strings.xml"), "<resources><string name=\"aavd_hide__0\">foo</string></resources>".getBytes(StandardCharsets.UTF_8));
        Map<String,String> map = Aapt2SyntheticNames.find(res); assertEquals("bavd_hide__0", map.get("$avd_hide__0"));
        Path stage = folder.newFolder("stage").toPath(); Aapt2DummyResources.stage(res, stage, Collections.emptyMap());
        Aapt2SyntheticNames.rewriteStage(stage, map);
        assertTrue(Files.exists(stage.resolve("drawable/bavd_hide__0.xml")));
        assertTrue(Files.exists(drawable.resolve("$avd_hide__0.xml")));
        assertEquals(ledger, new String(Files.readAllBytes(values.resolve("public.xml")), StandardCharsets.UTF_8));
        String text = "<!-- @drawable/$avd_hide__0 --><![CDATA[@drawable/$avd_hide__0]]><item name=\"$avd_hide__0\">@drawable/$avd_hide__0</item>";
        assertEquals("<!-- @drawable/$avd_hide__0 --><![CDATA[@drawable/$avd_hide__0]]><item name=\"bavd_hide__0\">@drawable/bavd_hide__0</item>", Aapt2SyntheticNames.rewriteXml(text,map));
    }
    @Test public void largeUnrelatedXmlIsNotRewritten() throws Exception {
        StringBuilder xml = new StringBuilder("<resources>");
        for (int i = 0; i < 100000; i++) xml.append("<string name=\"s").append(i).append("\">text</string>");
        xml.append("</resources>");
        String input = xml.toString();
        Map<String,String> aliases = Collections.singletonMap("$test", "atest");
        assertSame(input, Aapt2SyntheticNames.rewriteXml(input, aliases));
        String mixed = "<resources><string name=\"price\">$100</string><item name=\"$test\" type=\"drawable\">@drawable/$test</item></resources>";
        assertEquals("<resources><string name=\"price\">$100</string><item name=\"atest\" type=\"drawable\">@drawable/atest</item></resources>", Aapt2SyntheticNames.rewriteXml(mixed, aliases));
        try { Aapt2SyntheticNames.rewriteXml("<!DOCTYPE resources><resources/>", aliases); fail("DTD must be rejected"); }
        catch (IOException expected) { assertTrue(expected.getMessage().contains("DTD")); }
    }
    @Test public void actualAapt2RestoresNamesAndIds() throws Exception {
        String sdk=System.getenv("ANDROID_SDK_ROOT"); if (sdk==null) sdk=System.getenv("ANDROID_HOME");
        if (System.getenv("CI")!=null) assertNotNull(sdk); Assume.assumeNotNull(sdk);
        File aapt=new File(sdk,"build-tools/37.0.0/aapt2"), framework=new File(sdk,"platforms/android-37.0/android.jar");
        Assume.assumeTrue(aapt.isFile() && framework.isFile());
        Path root=folder.newFolder("integration").toPath(), res=Files.createDirectory(root.resolve("res")), values=Files.createDirectory(res.resolve("values")), drawable=Files.createDirectory(res.resolve("drawable"));
        String pub="<resources><public type=\"drawable\" name=\"$avd_hide__0\" id=\"0x7f010000\"/></resources>";
        Files.write(values.resolve("public.xml"),pub.getBytes(StandardCharsets.UTF_8));
        Files.write(drawable.resolve("$avd_hide__0.xml"),"<shape xmlns:android=\"http://schemas.android.com/apk/res/android\"><solid android:color=\"#ff0000\"/></shape>".getBytes(StandardCharsets.UTF_8));
        Files.write(drawable.resolve("reference.xml"),"<inset xmlns:android=\"http://schemas.android.com/apk/res/android\" android:drawable=\"@drawable/$avd_hide__0\"/>".getBytes(StandardCharsets.UTF_8));
        Path manifest=root.resolve("AndroidManifest.xml"); Files.write(manifest,"<manifest package=\"com.example.test\"><application/></manifest>".getBytes(StandardCharsets.UTF_8));
        File output=root.resolve("output.apk").toFile();
        Aapt2Pipeline.build(aapt.toString(),framework.toString(),res.toString(),manifest.toString(),output.getAbsolutePath());
        Process proc=new ProcessBuilder(aapt.toString(),"dump","resources",output.toString()).redirectErrorStream(true).start();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n; try(InputStream in=proc.getInputStream()){while((n=in.read(buf))!=-1)bytes.write(buf,0,n);} assertEquals(0,proc.waitFor());
        String dump=bytes.toString("UTF-8"); assertTrue(dump,dump.contains("$avd_hide__0")); assertTrue(dump,dump.contains("0x7f010000"));
        assertEquals(pub,new String(Files.readAllBytes(values.resolve("public.xml")),StandardCharsets.UTF_8));
    }
}
