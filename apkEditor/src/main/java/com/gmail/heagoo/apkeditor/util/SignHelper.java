package com.gmail.heagoo.apkeditor.util;

import android.content.Context;
import android.content.SharedPreferences;
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String keyName = prefs.getString("SignApkWith", "testkey");
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
        ApkBuildPipeline.SigningOptions options = new ApkBuildPipeline.SigningOptions(
                privateKey, certificate, minSdk, targetSdk, v1, v2, v3, v4, compressionLevel);
        ApkBuildPipeline.build(new File(srcApkPath), new File(targetApkPath),
                replacedFiles, addedFiles, deletedFiles, options, progress, cancellation);
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
