package com.gmail.heagoo.apkeditor.util;

import com.android.apksig.ApkVerifier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ApkBuildPipelineTest {
    private static final byte[] MANIFEST = java.util.Base64.getDecoder().decode(
            "AwAIAD8CAAABABwAQAEAAAgAAAAAAAAAAAAAADwAAAAAAAAAAAAAABQAAAAmAAAAOAAAAJAAAACkAAAAwgAAAOYAAAAIAG0AYQBuAGkAZgBlAHMAdAAAAAcAcABhAGMAawBhAGcAZQAAAAcAYQBuAGQAcgBvAGkAZAAAACoAaAB0AHQAcAA6AC8ALwBzAGMAaABlAG0AYQBzAC4AYQBuAGQAcgBvAGkAZAAuAGMAbwBtAC8AYQBwAGsALwByAGUAcwAvAGEAbgBkAHIAbwBpAGQAAAAIAHUAcwBlAHMALQBzAGQAawAAAA0AbQBpAG4AUwBkAGsAVgBlAHIAcwBpAG8AbgAAABAAdABhAHIAZwBlAHQAUwBkAGsAVgBlAHIAcwBpAG8AbgAAAAwAYwBvAG0ALgB0AGUAcwB0AC4AYQBwAHAAAAAAAIABCAAQAAAADAIBAXACAQEAARAAGAAAAP//////////AgAAAAMAAAACARAAOAAAAP///////////////wAAAAAUABQAAQAAAAAAAAD/////AQAAAAcAAAAIAAAAAwcAAAACARAATAAAAP///////////////wQAAAAUABQAAgAAAAAAAAACAAAABQAAAP////8IAAAAEBoAAAACAAAABgAAAP////8IAAAAECMAAAADARAAGAAAAP///////////////wQAAAADARAAGAAAAP///////////////wAAAAABARAAGAAAAP//////////AgAAAAMAAAA=");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static File asset(String name) {
        File f = new File("apkEditorPro/src/main/assets/" + name);
        if (!f.isFile()) {
            f = new File("../apkEditorPro/src/main/assets/" + name);
        }
        assertTrue("Missing test asset " + name, f.isFile());
        return f;
    }

    private static ApkBuildPipeline.SigningOptions options() throws Exception {
        byte[] keyBytes = Files.readAllBytes(asset("testkey.pk8").toPath());
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        X509Certificate cert;
        try (InputStream in = new FileInputStream(asset("testkey.x509.pem"))) {
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
        return new ApkBuildPipeline.SigningOptions(key, cert, 26, 35, true, true, true, false, 6);
    }

    private File sourceApk() throws Exception {
        File apk = folder.newFile("source.apk");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(apk))) {
            put(out, "AndroidManifest.xml", MANIFEST, ZipEntry.DEFLATED);
            put(out, "classes.dex", "dex".getBytes(StandardCharsets.UTF_8), ZipEntry.DEFLATED);
            put(out, "resources.arsc", new byte[513], ZipEntry.STORED);
            put(out, "lib/arm64-v8a/libgame.so", new byte[70000], ZipEntry.STORED);
            put(out, "assets/big.bin", new byte[300000], ZipEntry.DEFLATED);
            put(out, "META-INF/CERT.RSA", new byte[10], ZipEntry.DEFLATED);
        }
        return apk;
    }

    private static void put(ZipOutputStream out, String name, byte[] data, int method) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(method);
        if (method == ZipEntry.STORED) {
            entry.setSize(data.length);
            entry.setCompressedSize(data.length);
            CRC32 crc = new CRC32();
            crc.update(data);
            entry.setCrc(crc.getValue());
        }
        out.putNextEntry(entry);
        out.write(data);
        out.closeEntry();
    }

    @Test
    public void signsVerifiesAndPublishesAtomically() throws Exception {
        File source = sourceApk();
        File target = new File(folder.getRoot(), "gen_signed.apk");
        File replacement = folder.newFile("big.bin");
        byte[] replacementBytes = "replacement-payload".getBytes(StandardCharsets.UTF_8);
        Files.write(replacement.toPath(), replacementBytes);
        Map<String, String> replaces = new HashMap<>();
        replaces.put("assets/big.bin", replacement.getAbsolutePath());
        ApkBuildPipeline.build(source, target, replaces, null, null, options(), null, null);
        assertTrue(target.isFile());
        try (ZipFile zip = new ZipFile(target)) {
            assertEquals(ZipEntry.STORED, zip.getEntry("resources.arsc").getMethod());
            assertEquals(ZipEntry.STORED, zip.getEntry("lib/arm64-v8a/libgame.so").getMethod());
            assertFalse("old signature must be stripped", zip.getEntry("META-INF/CERT.RSA") != null
                    && zip.getEntry("META-INF/CERT.RSA").getSize() == 10);
            try (InputStream in = zip.getInputStream(zip.getEntry("assets/big.bin"))) {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[128];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                assertArrayEquals(replacementBytes, bos.toByteArray());
            }
        }
        ApkVerifier.Result result = new ApkVerifier.Builder(target).build().verify();
        assertTrue(result.isVerified());
        assertTrue(result.isVerifiedUsingV2Scheme());
        assertTrue(result.isVerifiedUsingV3Scheme());
        File[] leftovers = folder.getRoot().listFiles((dir, name) -> name.startsWith("apkbuild-"));
        assertEquals(0, leftovers.length);
    }

    @Test
    public void missingReplacementFailsWithoutPublishing() throws Exception {
        File source = sourceApk();
        File target = new File(folder.getRoot(), "gen_signed.apk");
        Map<String, String> replaces = Collections.singletonMap("classes.dex",
                new File(folder.getRoot(), "missing.dex").getAbsolutePath());
        try {
            ApkBuildPipeline.build(source, target, replaces, null, null, options(), null, null);
            fail("Expected failure");
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("missing"));
        }
        assertFalse(target.exists());
    }

    @Test
    public void cancellationDoesNotPublish() throws Exception {
        File source = sourceApk();
        File target = new File(folder.getRoot(), "gen_signed.apk");
        try {
            ApkBuildPipeline.build(source, target, null, null, null, options(), null, () -> true);
            fail("Expected cancellation");
        } catch (java.io.InterruptedIOException expected) {
            // expected
        }
        assertFalse(target.exists());
    }

    @Test
    public void targetSdk30RejectsV1Only() throws Exception {
        File source = sourceApk();
        File target = new File(folder.getRoot(), "gen_signed.apk");
        ApkBuildPipeline.SigningOptions base = options();
        ApkBuildPipeline.SigningOptions v1Only = new ApkBuildPipeline.SigningOptions(
                base.privateKey, base.certificate, 26, 35, true, false, false, false, 6);
        try {
            ApkBuildPipeline.build(source, target, null, null, null, v1Only, null, null);
            fail("Expected v2/v3 requirement");
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("v2 or v3"));
        }
        assertFalse(target.exists());
    }
}
