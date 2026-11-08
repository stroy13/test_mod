package com.gmail.heagoo.apkeditor.util;

import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Fail-fast APK assembly used by quick/common editing.
 *
 * <p>The previous JAR signer swallowed every exception and could leave a truncated
 * {@code gen_signed.apk} that the UI reported as success. This pipeline writes only
 * temporary files, signs with apksig (v1/v2/v3), verifies the result and publishes it
 * with an atomic rename. Original compression methods are preserved (native libraries
 * are NOT forcibly uncompressed, which previously multiplied APK size). Only
 * resources.arsc is forced to STORED for targetSdk 30+, and STORED native libraries /
 * resources.arsc are aligned for modern Android loaders.
 */
public final class ApkBuildPipeline {

    public interface Progress {
        void onStage(String stage, int percent);
    }

    public interface Cancellation {
        boolean isCancelled();
    }

    public static final class SigningOptions {
        public final PrivateKey privateKey;
        public final X509Certificate certificate;
        public final int minSdkVersion;
        public final int targetSdkVersion;
        public final boolean v1;
        public final boolean v2;
        public final boolean v3;
        public final boolean v4;
        public final int compressionLevel;

        public SigningOptions(PrivateKey privateKey, X509Certificate certificate,
                              int minSdkVersion, int targetSdkVersion,
                              boolean v1, boolean v2, boolean v3, boolean v4,
                              int compressionLevel) {
            this.privateKey = privateKey;
            this.certificate = certificate;
            this.minSdkVersion = Math.max(1, minSdkVersion);
            this.targetSdkVersion = targetSdkVersion;
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
            this.compressionLevel = Math.max(0, Math.min(9, compressionLevel));
        }
    }

    private static final int BUFFER_SIZE = 256 * 1024;
    private static final Progress NO_PROGRESS = (stage, percent) -> { };
    private static final Cancellation NEVER_CANCELLED = () -> false;

    private ApkBuildPipeline() {
    }

    public static void build(File source, File target,
                             Map<String, String> replacedFiles,
                             Map<String, String> addedFiles,
                             Set<String> deletedFiles,
                             SigningOptions options,
                             Progress progress,
                             Cancellation cancellation) throws Exception {
        if (source == null || !source.isFile()) {
            throw new IOException("Source APK does not exist: " + source);
        }
        if (target == null) {
            throw new IOException("Target APK path is null");
        }
        if (source.getCanonicalFile().equals(target.getCanonicalFile())) {
            throw new IOException("Source and target APK must be different files");
        }
        if (options == null || options.privateKey == null || options.certificate == null) {
            throw new IOException("Signing key/certificate is missing");
        }
        Map<String, String> replaces = safeMap(replacedFiles);
        Map<String, String> added = safeMap(addedFiles);
        Set<String> deleted = deletedFiles == null ? Collections.emptySet() : new HashSet<>(deletedFiles);
        validateReplacements(replaces, added);
        Progress report = progress == null ? NO_PROGRESS : progress;
        Cancellation cancel = cancellation == null ? NEVER_CANCELLED : cancellation;
        if (options.targetSdkVersion >= 30 && !options.v2 && !options.v3) {
            throw new IOException("APK with targetSdk " + options.targetSdkVersion
                    + " requires signature v2 or v3 on Android 11+");
        }

        File parent = target.getAbsoluteFile().getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new IOException("Cannot create output directory: " + parent);
        }
        File unsigned = File.createTempFile("apkbuild-", ".unsigned", parent);
        File signed = File.createTempFile("apkbuild-", ".signed", parent);
        File idsig = new File(signed.getAbsolutePath() + ".idsig");
        try {
            checkCancelled(cancel);
            report.onStage("merge", 0);
            merge(source, unsigned, replaces, added, deleted, options, report, cancel);
            checkCancelled(cancel);
            report.onStage("sign", -1);
            sign(unsigned, signed, options);
            checkCancelled(cancel);
            report.onStage("verify", -1);
            verify(signed, options);
            checkCancelled(cancel);
            report.onStage("publish", -1);
            atomicReplace(signed, target);
            if (options.v4) {
                File targetIdsig = new File(target.getAbsolutePath() + ".idsig");
                if (idsig.isFile()) {
                    atomicReplace(idsig, targetIdsig);
                }
            } else {
                File staleIdsig = new File(target.getAbsolutePath() + ".idsig");
                if (staleIdsig.exists() && !staleIdsig.delete()) {
                    throw new IOException("Cannot delete stale " + staleIdsig);
                }
            }
            report.onStage("done", 100);
        } finally {
            deleteQuietly(unsigned);
            deleteQuietly(signed);
            deleteQuietly(idsig);
        }
    }

    private static Map<String, String> safeMap(Map<String, String> input) {
        return input == null ? Collections.emptyMap() : new LinkedHashMap<>(input);
    }

    private static void validateReplacements(Map<String, String> replaces, Map<String, String> added)
            throws IOException {
        for (Map.Entry<String, String> entry : replaces.entrySet()) {
            validateEntryName(entry.getKey());
            requireReadable(entry.getValue());
        }
        for (Map.Entry<String, String> entry : added.entrySet()) {
            validateEntryName(entry.getKey());
            requireReadable(entry.getValue());
            if (replaces.containsKey(entry.getKey())) {
                throw new IOException("Entry is both replaced and added: " + entry.getKey());
            }
        }
    }

    private static void validateEntryName(String name) throws IOException {
        if (name == null || name.isEmpty() || name.startsWith("/") || name.contains("\\")
                || name.contains("../") || name.contains("/./") || name.endsWith("/.")) {
            throw new IOException("Unsafe ZIP entry name: " + name);
        }
    }

    private static void requireReadable(String path) throws IOException {
        File file = path == null ? null : new File(path);
        if (file == null || !file.isFile() || !file.canRead()) {
            throw new IOException("Replacement file is missing or unreadable: " + path);
        }
    }

    private static void merge(File source, File output,
                              Map<String, String> replaces, Map<String, String> added,
                              Set<String> deleted, SigningOptions options,
                              Progress report, Cancellation cancel) throws IOException {
        try (ZipFile zip = new ZipFile(source)) {
            List<ZipEntry> entries = new ArrayList<>();
            long totalBytes = 0;
            Enumeration<? extends ZipEntry> enumeration = zip.entries();
            Set<String> names = new HashSet<>();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                String name = entry.getName();
                if (!names.add(name)) {
                    throw new IOException("Duplicate ZIP entry: " + name);
                }
                if (deleted.contains(name) || isOldSignature(name)) {
                    continue;
                }
                entries.add(entry);
                totalBytes += Math.max(0, entry.getSize());
            }
            for (String name : added.keySet()) {
                if (!names.add(name)) {
                    throw new IOException("Duplicate ZIP entry: " + name);
                }
                totalBytes += new File(added.get(name)).length();
            }
            CountingOutputStream counting = new CountingOutputStream(
                    new BufferedOutputStream(new FileOutputStream(output), BUFFER_SIZE));
            try (ZipOutputStream out = new ZipOutputStream(counting)) {
                out.setLevel(options.compressionLevel);
                long doneBytes = 0;
                int index = 0;
                int total = entries.size() + added.size();
                byte[] buffer = new byte[BUFFER_SIZE];
                for (ZipEntry entry : entries) {
                    checkCancelled(cancel);
                    String name = entry.getName();
                    String replacement = replaces.get(name);
                    File replacementFile = replacement == null ? null : new File(replacement);
                    long size = replacementFile != null ? replacementFile.length() : entry.getSize();
                    if (replacementFile != null) {
                        validateReplacement(name, replacementFile);
                    }
                    try (InputStream input = replacementFile != null
                            ? new BufferedInputStream(new FileInputStream(replacementFile), BUFFER_SIZE)
                            : zip.getInputStream(entry)) {
                        writeEntry(out, counting, name, input, size, entry, replacementFile, buffer,
                                options.targetSdkVersion);
                    }
                    doneBytes += Math.max(0, size);
                    index++;
                    reportMerge(report, index, total, doneBytes, totalBytes, name);
                }
                for (Map.Entry<String, String> entry : added.entrySet()) {
                    checkCancelled(cancel);
                    File file = new File(entry.getValue());
                    validateReplacement(entry.getKey(), file);
                    try (InputStream input = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE)) {
                        writeEntry(out, counting, entry.getKey(), input, file.length(), null, file, buffer,
                                options.targetSdkVersion);
                    }
                    doneBytes += file.length();
                    index++;
                    reportMerge(report, index, total, doneBytes, totalBytes, entry.getKey());
                }
                out.finish();
            }
        }
    }

    private static void reportMerge(Progress report, int index, int total,
                                    long doneBytes, long totalBytes, String name) {
        int percent = totalBytes > 0
                ? (int) Math.min(99, doneBytes * 100 / totalBytes)
                : (total == 0 ? 100 : index * 100 / total);
        report.onStage("merge:" + name, percent);
    }

    private static void writeEntry(ZipOutputStream out, CountingOutputStream counting,
                                   String name, InputStream input, long size,
                                   ZipEntry sourceEntry, File replacementFile,
                                   byte[] buffer, int targetSdkVersion) throws IOException {
        // Preserve the source compression method. Forcing every .so to STORED inflated
        // game APKs several times over. resources.arsc must be STORED on targetSdk 30+.
        boolean stored = (sourceEntry != null && sourceEntry.getMethod() == ZipEntry.STORED)
                || mustStore(name, targetSdkVersion);
        ZipEntry entry = new ZipEntry(name);
        if (stored) {
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(size);
            entry.setCompressedSize(size);
            if (replacementFile == null && sourceEntry != null && sourceEntry.getCrc() != -1) {
                entry.setCrc(sourceEntry.getCrc());
            } else {
                entry.setCrc(computeCrc(replacementFile, size, buffer));
            }
            alignEntry(counting, entry);
            out.putNextEntry(entry);
            copy(input, out, buffer, size, name);
        } else {
            entry.setMethod(ZipEntry.DEFLATED);
            out.putNextEntry(entry);
            copy(input, out, buffer, -1, name);
        }
        out.closeEntry();
    }

    private static void validateReplacement(String name, File file) throws IOException {
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.equals("androidmanifest.xml") && !lower.equals("resources.arsc")
                && !lower.matches("classes[0-9]*\\.dex")) {
            return;
        }
        byte[] data;
        try (InputStream in = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            data = bos.toByteArray();
        }
        try {
            if (lower.equals("androidmanifest.xml")) {
                ApkStructureValidator.validateManifest(data);
            } else if (lower.equals("resources.arsc")) {
                ApkStructureValidator.validateResources(data);
            } else {
                ApkStructureValidator.validateDex(data);
            }
        } catch (IOException e) {
            throw new IOException("Edited " + name + " is structurally invalid: " + e.getMessage(), e);
        }
    }

    private static long computeCrc(File file, long expectedSize, byte[] buffer) throws IOException {
        CRC32 crc = new CRC32();
        long total = 0;
        try (InputStream input = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    crc.update(buffer, 0, read);
                    total += read;
                }
            }
        }
        if (total != expectedSize) {
            throw new IOException("Replacement changed size while reading: " + file);
        }
        return crc.getValue();
    }

    private static void copy(InputStream input, OutputStream out, byte[] buffer,
                             long expected, String name) throws IOException {
        long total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read > 0) {
                out.write(buffer, 0, read);
                total += read;
            }
        }
        if (expected >= 0 && total != expected) {
            throw new IOException("Truncated entry " + name + ": expected " + expected
                    + " bytes, got " + total);
        }
    }

    private static boolean mustStore(String name, int targetSdkVersion) {
        return targetSdkVersion >= 30 && name.toLowerCase(Locale.ROOT).equals("resources.arsc");
    }

    private static boolean isOldSignature(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) {
            return false;
        }
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA")
                || upper.endsWith(".EC") || upper.equals("META-INF/MANIFEST.MF");
    }

    /** Align STORED data using the standard 0xd935 padding extra field. */
    private static void alignEntry(CountingOutputStream counting, ZipEntry entry) throws IOException {
        int alignment = entry.getName().toLowerCase(Locale.ROOT).startsWith("lib/")
                && entry.getName().endsWith(".so") ? 16384 : 4;
        int nameLength = entry.getName().getBytes("UTF-8").length;
        long dataStart = counting.written + 30 + nameLength;
        int padding = (int) ((alignment - (dataStart % alignment)) % alignment);
        if (padding != 0) {
            if (padding < 4) {
                padding += alignment;
            }
            byte[] extra = new byte[padding];
            extra[0] = (byte) 0x35;
            extra[1] = (byte) 0xd9;
            int size = padding - 4;
            extra[2] = (byte) (size & 0xff);
            extra[3] = (byte) ((size >> 8) & 0xff);
            entry.setExtra(extra);
        }
    }

    private static void sign(File unsigned, File signed, SigningOptions options) throws Exception {
        ApkSigner.SignerConfig config = new ApkSigner.SignerConfig.Builder(
                "apkeditor", options.privateKey, Collections.singletonList(options.certificate)).build();
        new ApkSigner.Builder(Collections.singletonList(config))
                .setInputApk(unsigned)
                .setOutputApk(signed)
                .setMinSdkVersion(options.minSdkVersion)
                .setV1SigningEnabled(options.v1)
                .setV2SigningEnabled(options.v2)
                .setV3SigningEnabled(options.v3)
                .setV4SigningEnabled(options.v4)
                .build().sign();
    }

    private static void verify(File signed, SigningOptions options) throws Exception {
        ApkVerifier.Result result = new ApkVerifier.Builder(signed)
                .setMinCheckedPlatformVersion(options.minSdkVersion)
                .setMaxCheckedPlatformVersion(Math.max(37, options.targetSdkVersion))
                .build().verify();
        if (!result.isVerified() || !result.getErrors().isEmpty()) {
            throw new IOException("Signed APK verification failed: " + result.getErrors());
        }
        if (options.v2 && !result.isVerifiedUsingV2Scheme()) {
            throw new IOException("Requested v2 signature is missing or invalid");
        }
        if (options.v3 && !result.isVerifiedUsingV3Scheme() && options.minSdkVersion >= 28) {
            throw new IOException("Requested v3 signature is missing or invalid");
        }
        try (ZipFile zip = new ZipFile(signed)) {
            if (zip.getEntry("AndroidManifest.xml") == null) {
                throw new IOException("Signed APK has no AndroidManifest.xml");
            }
            if (zip.getEntry("classes.dex") == null) {
                throw new IOException("Signed APK has no classes.dex");
            }
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String lower = entry.getName().toLowerCase(Locale.ROOT);
                if (!lower.equals("androidmanifest.xml") && !lower.equals("resources.arsc")
                        && !lower.matches("classes[0-9]*\\.dex")) {
                    continue;
                }
                byte[] data;
                try (InputStream in = zip.getInputStream(entry);
                     java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
                    byte[] buf = new byte[BUFFER_SIZE];
                    int n;
                    while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                    data = bos.toByteArray();
                }
                if (lower.equals("androidmanifest.xml")) {
                    ApkStructureValidator.validateManifest(data);
                } else if (lower.equals("resources.arsc")) {
                    ApkStructureValidator.validateResources(data);
                } else {
                    ApkStructureValidator.validateDex(data);
                }
            }
        }
    }

    private static void atomicReplace(File from, File to) throws IOException {
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void checkCancelled(Cancellation cancellation) throws InterruptedIOException {
        if (cancellation.isCancelled()) {
            throw new InterruptedIOException("APK build cancelled");
        }
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    private static final class CountingOutputStream extends FilterOutputStream {
        long written;

        CountingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        @Override
        public void write(byte[] buffer, int off, int len) throws IOException {
            out.write(buffer, off, len);
            written += len;
        }
    }
}
