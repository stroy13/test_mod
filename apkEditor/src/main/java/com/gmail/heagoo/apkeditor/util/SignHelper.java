package com.gmail.heagoo.apkeditor.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.gmail.heagoo.apkeditor.SettingActivity;
import com.gmail.heagoo.apkeditor.ce.ManifestInfo;
import com.gmail.heagoo.apkeditor.ce.ManifestParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Modern, fail-fast APK signing/assembly shared by quick edit and full compose. */
public final class SignHelper {

    private SignHelper() {
    }

    public static void sign(Context ctx, String srcApkPath, String targetApkPath,
                            Map<String, String> replacedFiles,
                            Map<String, String> addedFiles,
                            Set<String> deletedFiles) throws Exception {
        sign(ctx, srcApkPath, targetApkPath, replacedFiles, addedFiles, deletedFiles, null, null);
    }

    public static void sign(Context ctx, String srcApkPath, String targetApkPath,
                            Map<String, String> replacedFiles,
                            Map<String, String> addedFiles,
                            Set<String> deletedFiles,
                            ApkBuildPipeline.Progress progress,
                            ApkBuildPipeline.Cancellation cancellation) throws Exception {
        sign(ctx, srcApkPath, targetApkPath, replacedFiles, addedFiles, deletedFiles, progress, cancellation, null);
    }

    public static void sign(Context ctx, String srcApkPath, String targetApkPath,
                            Map<String, String> replacedFiles,
                            Map<String, String> addedFiles,
                            Set<String> deletedFiles,
                            ApkBuildPipeline.Progress progress,
                            ApkBuildPipeline.Cancellation cancellation,
                            ApkBuildPipeline.Logger logger) throws Exception {
        // When the caller does not provide a log, still write a visible build-log.txt
        // next to the target APK so every signing/assembly path is diagnosable.
        BuildLog ownLog = null;
        ApkBuildPipeline.Logger out = logger;
        if (out == null) {
            try {
                File target = new File(targetApkPath);
                File parent = target.getAbsoluteFile().getParentFile();
                if (parent != null) {
                    ownLog = new BuildLog(new File(parent, "build-log.txt"));
                    out = ownLog;
                }
            } catch (IOException ignored) {
                out = null;
            }
        }
        try {
            signInternal(ctx, srcApkPath, targetApkPath, replacedFiles, addedFiles, deletedFiles,
                    progress, cancellation, out);
            if (out != null) {
                out.log("SignHelper finished successfully");
            }
        } catch (Exception e) {
            if (out != null) {
                out.log("SignHelper failed: " + e);
            }
            throw e;
        } finally {
            if (ownLog != null) {
                ownLog.close();
            }
        }
    }

    private static void signInternal(Context ctx, String srcApkPath, String targetApkPath,
                                     Map<String, String> replacedFiles,
                                     Map<String, String> addedFiles,
                                     Set<String> deletedFiles,
                                     ApkBuildPipeline.Progress progress,
                                     ApkBuildPipeline.Cancellation cancellation,
                                     ApkBuildPipeline.Logger out) throws Exception {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String keyName = prefs.getString("SignApkWith", "testkey");
        if (out != null) {
            out.log("SignHelper: source=" + srcApkPath + ", target=" + targetApkPath);
            out.log("Signing key preference: " + keyName);
        }
        PrivateKey privateKey;
        X509Certificate certificate;
        if ("customkey".equals(keyName)) {
            String privatePath = prefs.getString(SettingActivity.STR_PRIVATEKEYPATH, "");
            String publicPath = prefs.getString(SettingActivity.STR_PUBLICKEYPATH, "");
            if (privatePath.isEmpty() || publicPath.isEmpty()) {
                throw new IOException("Custom signing key is not configured");
            }
            try (InputStream keyInput = new BufferedInputStream(new FileInputStream(privatePath));
                 InputStream certInput = new BufferedInputStream(new FileInputStream(publicPath))) {
                privateKey = readPrivateKey(keyInput);
                certificate = readCertificate(certInput);
            }
        } else {
            if (!"editorkey".equals(keyName)) {
                keyName = "testkey";
            }
            try (InputStream keyInput = ctx.getAssets().open(keyName + ".pk8");
                 InputStream certInput = ctx.getAssets().open(keyName + ".x509.pem")) {
                privateKey = readPrivateKey(keyInput);
                certificate = readCertificate(certInput);
            }
        }

        ManifestInfo manifest = readManifest(new File(srcApkPath));
        if (out != null) {
            out.log("Source manifest: " + ManifestParser.describe(manifest));
        }
        String schemes = prefs.getString("SignSchemes", "v123");
        boolean v1 = schemes.contains("1");
        boolean v2 = schemes.contains("2");
        boolean v3 = schemes.contains("3");
        boolean v4 = schemes.contains("4");
        if (!v1 && !v2 && !v3) {
            v1 = true;
            v2 = true;
            v3 = true;
        }
        int minSdk = manifest.minSdkVersion > 0 ? manifest.minSdkVersion : 26;
        int targetSdk = manifest.targetSdkVersion > 0 ? manifest.targetSdkVersion : minSdk;
        int compressionLevel = SettingActivity.getCompressionLevel(ctx);
        if (out != null) {
            out.log("Signature schemes preference: " + schemes + " -> v1=" + v1 + ", v2=" + v2
                    + ", v3=" + v3 + ", v4=" + v4 + "; compressionLevel=" + compressionLevel);
        }
        ApkBuildPipeline.SigningOptions options = new ApkBuildPipeline.SigningOptions(
                privateKey, certificate, minSdk, targetSdk, v1, v2, v3, v4, compressionLevel);
        ApkBuildPipeline.build(new File(srcApkPath), new File(targetApkPath),
                replacedFiles, addedFiles, deletedFiles, options, progress, cancellation, out);
        try {
            validateOutputWithAndroid(ctx, new File(targetApkPath), out);
        } catch (Exception e) {
            if (out != null) {
                out.log("Output rejected by Android package parser; removing unusable APK: " + e);
            }
            // The pipeline publishes atomically before this final framework check.
            // Do not leave an APK that the installer cannot parse.
            //noinspection ResultOfMethodCallIgnored
            new File(targetApkPath).delete();
            throw e;
        }
    }

    /**
     * Final gate: ask Android's own package parser to read the generated APK.
     * apksig + chunk validation do not catch every semantic manifest problem that
     * PackageInstaller rejects with "cannot parse package".
     */
    private static void validateOutputWithAndroid(Context ctx, File apk, ApkBuildPipeline.Logger out)
            throws Exception {
        PackageManager pm = ctx.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_META_DATA);
        if (info == null || info.applicationInfo == null || info.packageName == null) {
            throw new IOException("Android package parser cannot read generated APK: " + apk);
        }
        if (out != null) {
            long versionCode = android.os.Build.VERSION.SDK_INT >= 28
                    ? info.getLongVersionCode() : info.versionCode;
            out.log("Android package parser OK: package=" + info.packageName
                    + ", versionCode=" + versionCode
                    + ", versionName=" + info.versionName);
        }
        try (ZipFile zip = new ZipFile(apk)) {
            ZipEntry entry = zip.getEntry("AndroidManifest.xml");
            if (entry == null) {
                throw new IOException("Generated APK has no AndroidManifest.xml");
            }
            try (InputStream input = zip.getInputStream(entry)) {
                ManifestParser parser = new ManifestParser(input);
                parser.parse();
                ManifestInfo parsed = parser.getManifestInfo();
                if (parsed == null || parsed.packageName == null) {
                    throw new IOException("Generated manifest has no package name");
                }
                if (!parsed.packageName.equals(info.packageName)) {
                    throw new IOException("Manifest package mismatch: binary=" + parsed.packageName
                            + ", framework=" + info.packageName);
                }
                if (out != null) {
                    out.log("Output manifest: " + ManifestParser.describe(parsed));
                }
            }
        }
    }

    private static ManifestInfo readManifest(File apk) throws Exception {
        try (ZipFile zip = new ZipFile(apk)) {
            ZipEntry entry = zip.getEntry("AndroidManifest.xml");
            if (entry == null) {
                throw new IOException("APK has no AndroidManifest.xml: " + apk);
            }
            try (InputStream input = zip.getInputStream(entry)) {
                ManifestParser parser = new ManifestParser(input);
                parser.parse();
                ManifestInfo info = parser.getManifestInfo();
                if (info == null || info.packageName == null) {
                    throw new IOException("Cannot parse AndroidManifest.xml in " + apk);
                }
                return info;
            }
        }
    }

    static PrivateKey readPrivateKey(InputStream input) throws Exception {
        byte[] encoded = readAll(input);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoded);
        String[] algorithms = {"RSA", "EC", "DSA"};
        Exception last = null;
        for (String algorithm : algorithms) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(spec);
            } catch (Exception e) {
                last = e;
            }
        }
        throw new IOException("Unsupported or invalid PKCS#8 private key", last);
    }

    static X509Certificate readCertificate(InputStream input) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(input);
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read > 0) {
                output.write(buffer, 0, read);
            }
        }
        return output.toByteArray();
    }
}
