package com.gmail.heagoo.apkeditor.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.Assume;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.Assert.*;

public class Aapt2DummyResourcesTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private static final String INPUT = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
            + "  <item type=\"anim\" name=\"dummy_ae_0\">false</item>\n"
            + "  <item type=\"animator\" name=\"dummy_ae_1a\">false</item>\n</resources>\n";
    @Test public void repairsGeneratedItemsInMixedFilesOnlyInCopy() throws Exception {
        Path res = folder.newFolder("res").toPath();
        Path values = Files.createDirectory(res.resolve("values"));
        Path file = values.resolve("anims.xml");
        Files.write(file, INPUT.getBytes(StandardCharsets.UTF_8));
        Path other = values.resolve("attrs.xml");
        String mixed = "<resources>\n<attr name=\"real\" format=\"string\"/>\n"
                + "<item type=\"attr\" name=\"dummy_ae_a\">false</item>\n"
                + "<item type=\"array\" name=\"dummy_ae_b\">false</item>\n</resources>";
        Files.write(other, mixed.getBytes(StandardCharsets.UTF_8));
        Map<Path, byte[]> repairs = Aapt2DummyResources.findRepairs(res);
        assertEquals(2, repairs.size());
        Path stage = folder.newFolder("stage").toPath();
        Aapt2DummyResources.stage(res, stage, repairs);
        assertEquals(INPUT, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        assertEquals(mixed, new String(Files.readAllBytes(other), StandardCharsets.UTF_8));
        assertEquals(INPUT.replace(">false</item>", ">@null</item>"),
                new String(Files.readAllBytes(stage.resolve("values/anims.xml")), StandardCharsets.UTF_8));
        assertEquals(mixed.replace(">false</item>", ">@null</item>"),
                new String(Files.readAllBytes(stage.resolve("values/attrs.xml")), StandardCharsets.UTF_8));
    }
    @Test public void ignoresCommentsCdataNestedItemsAndUserEntries() {
        String dummy = "<item type=\"attr\" name=\"dummy_ae_0\">false</item>";
        String xml = "<resources><!-- " + dummy + " --><string name=\"s\"><![CDATA[" + dummy
                + "]]></string><array name=\"real\">" + dummy + "</array>"
                + "<item type=\"attr\" name=\"user_entry\">false</item>" + dummy + "</resources>";
        String expected = xml.substring(0, xml.lastIndexOf(dummy))
                + dummy.replace(">false<", ">@null<") + "</resources>";
        assertEquals(expected, Aapt2DummyResources.normalize(xml));
        String dtd = "<!DOCTYPE resources [<!ENTITY x 'foo'>]>" + xml;
        assertEquals(dtd, Aapt2DummyResources.normalize(dtd));
    }

    @Test public void realAapt2CompilesAndLinksRepairedMixedResources() throws Exception {
        String sdk = System.getenv("ANDROID_SDK_ROOT");
        if (sdk == null) sdk = System.getenv("ANDROID_HOME");
        if (System.getenv("CI") != null) assertNotNull("Android SDK required in CI", sdk);
        Assume.assumeNotNull(sdk);
        File aapt = new File(sdk, "build-tools/37.0.0/aapt2");
        File framework = new File(sdk, "platforms/android-37.0/android.jar");
        if (System.getenv("CI") != null) assertTrue("AAPT2 and framework required in CI", aapt.isFile() && framework.isFile());
        Assume.assumeTrue(aapt.isFile() && framework.isFile());
        Path root = folder.newFolder("integration").toPath();
        Path res = Files.createDirectory(root.resolve("res"));
        Path values = Files.createDirectory(res.resolve("values"));
        String attrs = "<resources><attr name=\"real_attr\" format=\"string\"/>\n"
                + "<item type=\"attr\" name=\"dummy_ae_2\">false</item></resources>";
        String arrays = "<resources><string-array name=\"real_array\"><item>value</item></string-array>\n"
                + "<item type=\"array\" name=\"dummy_ae_3\">false</item></resources>";
        Path attrsFile = values.resolve("attrs.xml"), arraysFile = values.resolve("arrays.xml");
        Files.write(attrsFile, attrs.getBytes(StandardCharsets.UTF_8));
        Files.write(arraysFile, arrays.getBytes(StandardCharsets.UTF_8));
        StringBuilder misc = new StringBuilder("<resources>\n");
        String[] types = {"anim", "animator", "color", "dimen", "drawable", "font", "fraction", "integer", "interpolator", "layout", "menu", "mipmap", "navigation", "plurals", "raw", "style", "transition", "xml"};
        for (String type : types) misc.append("<item type=\"").append(type)
                .append("\" name=\"dummy_ae_0\">false</item>\n");
        misc.append("</resources>");
        Files.write(values.resolve("other.xml"), misc.toString().getBytes(StandardCharsets.UTF_8));
        String publicXml = "<resources><public type=\"attr\" name=\"dummy_ae_2\" id=\"0x7f010002\"/>"
                + "<public type=\"array\" name=\"dummy_ae_3\" id=\"0x7f020003\"/></resources>";
        Files.write(values.resolve("public.xml"), publicXml.getBytes(StandardCharsets.UTF_8));
        Path manifest = root.resolve("AndroidManifest.xml");
        Files.write(manifest, "<manifest package=\"com.example.dummy\"><application/></manifest>".getBytes(StandardCharsets.UTF_8));
        Process broken = new ProcessBuilder(aapt.toString(), "compile", "--legacy", "--no-crunch", "--dir", res.toString(), "-o", root.resolve("broken.zip").toString()).redirectErrorStream(true).start();
        byte[] errors = readAll(broken.getInputStream());
        assertNotEquals("fixture should reproduce the original AAPT2 error", 0, broken.waitFor());
        assertTrue(new String(errors, StandardCharsets.UTF_8).contains("Expected a reference"));
        File apk = root.resolve("out.apk").toFile();
        Aapt2Pipeline.build(aapt.toString(), framework.toString(), res.toString(), manifest.toString(), apk.getAbsolutePath());
        assertTrue(apk.isFile() && apk.length() > 0);
        assertEquals(attrs, new String(Files.readAllBytes(attrsFile), StandardCharsets.UTF_8));
        assertEquals(arrays, new String(Files.readAllBytes(arraysFile), StandardCharsets.UTF_8));
    }

    private static byte[] readAll(java.io.InputStream input) throws Exception {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = input.read(buf)) != -1) output.write(buf, 0, n);
        return output.toByteArray();
    }
}
