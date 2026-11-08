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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ApkBuildPipelineTest {
    private static final byte[] MANIFEST = java.util.Base64.getDecoder().decode(
            "AwAIADwCAAABABwAQAEAAAgAAAAAAAAAAAAAADwAAAAAAAAAAAAAABQAAAAmAAAAOAAAAJAAAACkAAAAwgAAAOYAAAAIAG0AYQBuAGkAZgBlAHMAdAAAAAcAcABhAGMAawBhAGcAZQAAAAcAYQBuAGQAcgBvAGkAZAAAACoAaAB0AHQAcAA6AC8ALwBzAGMAaABlAG0AYQBzAC4AYQBuAGQAcgBvAGkAZAAuAGMAbwBtAC8AYQBwAGsALwByAGUAcwAvAGEAbgBkAHIAbwBpAGQAAAAIAHUAcwBlAHMALQBzAGQAawAAAA0AbQBpAG4AUwBkAGsAVgBlAHIAcwBpAG8AbgAAABAAdABhAHIAZwBlAHQAUwBkAGsAVgBlAHIAcwBpAG8AbgAAAAwAYwBvAG0ALgB0AGUAcwB0AC4AYQBwAHAAAAAAAIABCAAQAAAADAIBAXACAQEAARAAGAAAAP//////////AgAAAAMAAAACARAAOAAAAP///////////////wAAAAAUABQAAQAAAAAAAAD/////AQAAAAcAAAAIAAADBwAAAAIBEABMAAAA////////////////BAAAABQAFAACAAAAAAAAAAIAAAAFAAAA/////wgAABAaAAAAAgAAAAYAAAD/////CAAAECMAAAADARAAGAAAAP///////////////wQAAAADARAAGAAAAP///////////////wAAAAABARAAGAAAAP//////////AgAAAAMAAAA=");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static File asset(String name) {
        File f = new File("apkEditorPlus/src/main/assets/" + name);
        if (!f.isFile()) {
            f = new File("../apkEditorPlus/src/main/assets/" + name);
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
            // STORED entries first so the alignment test can inspect their local headers
            // without walking deflated data descriptors.
            put(out, "resources.arsc", minimalArsc(), ZipEntry.STORED);
            put(out, "lib/arm64-v8a/libgame.so", new byte[70000], ZipEntry.STORED);
            put(out, "AndroidManifest.xml", MANIFEST, ZipEntry.DEFLATED);
            put(out, "classes.dex", minimalDex(), ZipEntry.DEFLATED);
            put(out, "lib/armeabi-v7a/libcompressed.so", compressibleLibrary(), ZipEntry.DEFLATED);
            put(out, "assets/big.bin", new byte[300000], ZipEntry.DEFLATED);
            put(out, "META-INF/CERT.RSA", new byte[10], ZipEntry.DEFLATED);
        }
        return apk;
    }

    private static byte[] compressibleLibrary() {
        byte[] data = new byte[200000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 16);
        return data;
    }

    private static byte[] minimalArsc() {
        // ResTable with one package and an empty global string pool.
        byte[] pool = new byte[28];
        ByteBuffer p = ByteBuffer.wrap(pool).order(ByteOrder.LITTLE_ENDIAN);
        p.putShort((short) 0x0001); p.putShort((short) 28); p.putInt(28);
        p.putInt(0); p.putInt(0); p.putInt(0); p.putInt(28); p.putInt(0);
        byte[] pkg = new byte[288];
        ByteBuffer k = ByteBuffer.wrap(pkg).order(ByteOrder.LITTLE_ENDIAN);
        k.putShort((short) 0x0200); k.putShort((short) 288); k.putInt(288); k.putInt(0x7f);
        byte[] name = "com.test.app".getBytes(StandardCharsets.UTF_16LE);
        k.position(16); k.put(name);
        int size = 12 + pool.length + pkg.length;
        byte[] table = new byte[size];
        ByteBuffer t = ByteBuffer.wrap(table).order(ByteOrder.LITTLE_ENDIAN);
        t.putShort((short) 0x0002); t.putShort((short) 12); t.putInt(size); t.putInt(1);
        t.put(pool); t.put(pkg);
        return table;
    }

    private static byte[] minimalDex() throws Exception {
        byte[] dex = new byte[112];
        ByteBuffer b = ByteBuffer.wrap(dex).order(ByteOrder.LITTLE_ENDIAN);
        b.put("dex\n035\0".getBytes(StandardCharsets.US_ASCII));
        b.position(32); b.putInt(112); b.putInt(0x70); b.putInt(0x12345678);
        // link_size, link_off, map_off, string_ids_size, string_ids_off, type_ids_size,
        // type_ids_off, proto_ids_size, proto_ids_off, field_ids_size, field_ids_off,
        // method_ids_size, method_ids_off, class_defs_size, class_defs_off, data_size, data_off
        for (int i = 0; i < 17; i++) b.putInt(0);
        java.security.MessageDigest sha1 = java.security.MessageDigest.getInstance("SHA-1");
        sha1.update(dex, 32, dex.length - 32);
        byte[] digest = sha1.digest();
        System.arraycopy(digest, 0, dex, 12, 20);
        java.util.zip.Adler32 adler = new java.util.zip.Adler32();
        adler.update(dex, 12, dex.length - 12);
        b.position(8); b.putInt((int) adler.getValue());
        return dex;
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
        ApkVerifier.Result result = new ApkVerifier.Builder(target)
                .setMinCheckedPlatformVersion(26)
                .setMaxCheckedPlatformVersion(37)
                .build().verify();
        assertTrue("APK verification errors: " + result.getErrors(), result.isVerified());
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

    @Test
    public void preservesDeflatedNativeLibraryAndDoesNotInflateSize() throws Exception {
        File source = sourceApk();
        File target = new File(folder.getRoot(), "gen_signed.apk");
        ApkBuildPipeline.build(source, target, null, null, null, options(), null, null);
        try (ZipFile src = new ZipFile(source); ZipFile out = new ZipFile(target)) {
            ZipEntry lib = out.getEntry("lib/armeabi-v7a/libcompressed.so");
            assertEquals(ZipEntry.DEFLATED, lib.getMethod());
            assertEquals(src.getEntry("lib/armeabi-v7a/libcompressed.so").getSize(), lib.getSize());
            assertTrue("deflated library must stay compressed", lib.getCompressedSize() < lib.getSize() / 2);
            assertEquals(ZipEntry.STORED, out.getEntry("lib/arm64-v8a/libgame.so").getMethod());
        }
        // Output may differ by signature blocks, but must not multiply the payload size.
        assertTrue("output inflated: " + target.length() + " vs " + source.length(),
                target.length() < source.length() + 200000);
    }

    @Test
    public void storedEntriesAreAlignedAtRealDataOffset() throws Exception {
        File source = sourceApk();
        // Check alignment on the merged archive before apksig inserts its signing block
        // and rewrites the central directory; then confirm the signed APK still exposes
        // the same entries/compression through ZipFile.
        File unsigned = new File(folder.getRoot(), "unsigned.apk");
        ApkBuildPipeline.merge(source, unsigned, null, null, null, options(), null, null);
        Map<String, Long> compressedSizes = new HashMap<>();
        try (ZipFile zf = new ZipFile(unsigned)) {
            java.util.Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry ze = en.nextElement();
                compressedSizes.put(ze.getName(), ze.getCompressedSize());
            }
        }
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(unsigned, "r")) {
            byte[] all = new byte[(int) raf.length()];
            raf.readFully(all);
            int pos = 0;
            boolean checkedArsc = false;
            boolean checkedLib = false;
            while (pos + 30 <= all.length
                    && ByteBuffer.wrap(all, pos, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() == 0x04034b50) {
                ByteBuffer l = ByteBuffer.wrap(all, pos, 30).order(ByteOrder.LITTLE_ENDIAN);
                int flags = l.getShort(6) & 0xffff;
                int method = l.getShort(8) & 0xffff;
                int nameLen = l.getShort(26) & 0xffff;
                int extraLen = l.getShort(28) & 0xffff;
                String name = new String(all, pos + 30, nameLen, StandardCharsets.UTF_8);
                long dataOffset = pos + 30L + nameLen + extraLen;
                if (method == ZipEntry.STORED && name.equals("resources.arsc")) {
                    assertEquals("resources.arsc must be 4-byte aligned", 0, dataOffset % 4);
                    checkedArsc = true;
                }
                if (method == ZipEntry.STORED && name.equals("lib/arm64-v8a/libgame.so")) {
                    assertEquals("native library must be 16KB aligned", 0, dataOffset % 16384);
                    checkedLib = true;
                }
                if (checkedArsc && checkedLib) {
                    break;
                }
                Long compSize = compressedSizes.get(name);
                assertNotNull("central directory missing '" + name + "' at pos=" + pos
                        + ", dataOffset=" + dataOffset + ", extraLen=" + extraLen
                        + ", known=" + compressedSizes.keySet(), compSize);
                pos = (int) (dataOffset + compSize);
                if (method == ZipEntry.DEFLATED && (flags & 0x08) != 0) {
                    // Data descriptor is 12 bytes (crc/csize/size); the 0x08074b50 signature
                    // prefix is optional and java.util.zip.ZipOutputStream omits it.
                    if (pos + 4 <= all.length
                            && ByteBuffer.wrap(all, pos, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() == 0x08074b50) {
                        pos += 16;
                    } else {
                        pos += 12;
                    }
                }
            }
            assertTrue("resources.arsc not found", checkedArsc);
            assertTrue("stored library not found", checkedLib);
        }
        File target = new File(folder.getRoot(), "gen_signed.apk");
        ApkBuildPipeline.build(source, target, null, null, null, options(), null, null);
        try (ZipFile zip = new ZipFile(target)) {
            assertEquals(ZipEntry.STORED, zip.getEntry("resources.arsc").getMethod());
            assertEquals(ZipEntry.STORED, zip.getEntry("lib/arm64-v8a/libgame.so").getMethod());
            assertEquals(ZipEntry.DEFLATED, zip.getEntry("lib/armeabi-v7a/libcompressed.so").getMethod());
        }
    }

    @Test
    public void invalidEditedManifestIsRejectedBeforePublish() throws Exception {
        File source = sourceApk();
        File target = new File(folder.getRoot(), "gen_signed.apk");
        File bad = folder.newFile("AndroidManifest.xml");
        Files.write(bad.toPath(), new byte[]{1, 2, 3});
        Map<String, String> replaces = Collections.singletonMap("AndroidManifest.xml", bad.getAbsolutePath());
        try {
            ApkBuildPipeline.build(source, target, replaces, null, null, options(), null, null);
            fail("Expected structural validation failure");
        } catch (Exception expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("structurally invalid"));
        }
        assertFalse(target.exists());
    }
}
