package com.gmail.heagoo.apkeditor.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/* loaded from: /tmp/apkx/classes9.dex */
public final class BundledFrameworkFiles {
    public static final String ARSC_SHA = "5d5032e85b8d95a9163646649f0cda453e39cbb9d8282bacdba680f086b8270b";
    public static final String JAR_SHA = "cc1482f9ec4a69dd8d01b1491f155e3b1d16851a4fb4309c790c646faff55cbc";
    private static final String MANIFEST_SHA = "73c34365df81f46aa1af01582f85ea1fc108563d8bc519982be81692eb915335";

    public interface AssetSource {
        InputStream open() throws IOException;
    }

    private BundledFrameworkFiles() {
    }

    public static synchronized void ensure(File dir, AssetSource source) throws IOException {
        synchronized (BundledFrameworkFiles.class) {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new IOException("Cannot create framework directory");
            }
            File jar = new File(dir, "android.jar");
            File arsc = new File(dir, "resources.arsc");
            File manifest = new File(dir, "AndroidManifest.xml");
            if (matches(jar, JAR_SHA) && matches(arsc, ARSC_SHA) && matches(manifest, MANIFEST_SHA)) {
                return;
            }
            File tmpJar = new File(dir, "android.jar.prepare.tmp");
            File tmpArsc = new File(dir, "resources.arsc.prepare.tmp");
            File tmpManifest = new File(dir, "AndroidManifest.prepare.tmp");
            try {
                ZipInputStream zip = new ZipInputStream(source.open());
                boolean found = false;
                while (true) {
                    try {
                        ZipEntry entry = zip.getNextEntry();
                        if (entry != null) {
                            if (!entry.isDirectory() && entry.getName().equals("android.jar")) {
                                copy(zip, tmpJar, 8388608L);
                                found = true;
                                break;
                            }
                            zip.closeEntry();
                        } else {
                            break;
                        }
                    } finally {
                    }
                }
                if (!found) {
                    throw new IOException("Missing android.jar in bundled framework");
                }
                zip.close();
                if (!matches(tmpJar, JAR_SHA)) {
                    throw new IOException("Bundled framework archive hash mismatch");
                }
                ZipFile zip2 = new ZipFile(tmpJar);
                try {
                    extract(zip2, "resources.arsc", tmpArsc, 104857600L);
                    extract(zip2, "AndroidManifest.xml", tmpManifest, 4194304L);
                    zip2.close();
                    if (!matches(tmpArsc, ARSC_SHA) || !matches(tmpManifest, MANIFEST_SHA)) {
                        throw new IOException("Bundled framework data hash mismatch");
                    }
                    publish(tmpArsc, arsc);
                    publish(tmpManifest, manifest);
                    publish(tmpJar, jar);
                } finally {
                }
            } finally {
                tmpJar.delete();
                tmpArsc.delete();
                tmpManifest.delete();
            }
        }
    }

    private static void extract(ZipFile zip, String name, File out, long limit) throws IOException {
        ZipEntry e = zip.getEntry(name);
        if (e == null || e.isDirectory()) {
            throw new IOException("Missing " + name);
        }
        InputStream in = zip.getInputStream(e);
        try {
            copy(in, out, limit);
            if (in != null) {
                in.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private static void copy(InputStream in, File out, long limit) throws IOException {
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(out), 65536);
        try {
            byte[] buf = new byte[65536];
            long count = 0;
            while (true) {
                int n = in.read(buf);
                if (n == -1) {
                    stream.close();
                    return;
                }
                count += n;
                if (count > limit) {
                    throw new IOException("Framework file too large");
                }
                stream.write(buf, 0, n);
            }
        } catch (Throwable th) {
            try {
                stream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static boolean matches(File f, String expected) throws IOException {
        if (!f.isFile()) {
            return false;
        }
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            InputStream in = new BufferedInputStream(new FileInputStream(f), 65536);
            try {
                byte[] b = new byte[65536];
                while (true) {
                    int n = in.read(b);
                    if (n == -1) {
                        break;
                    }
                    d.update(b, 0, n);
                }
                in.close();
                StringBuilder s = new StringBuilder(64);
                for (byte b2 : d.digest()) {
                    int v = b2 & 255;
                    s.append(Character.forDigit(v >>> 4, 16));
                    s.append(Character.forDigit(v & 15, 16));
                }
                return expected.equals(s.toString());
            } finally {
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static void publish(File tmp, File dest) throws IOException {
        if (!tmp.renameTo(dest)) {
            throw new IOException("Cannot publish framework file: " + dest.getName());
        }
    }
}
