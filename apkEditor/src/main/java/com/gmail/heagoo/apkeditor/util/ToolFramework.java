package com.gmail.heagoo.apkeditor.util;

import android.content.Context;
import android.preference.PreferenceManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/* loaded from: /tmp/apkx/classes9.dex */
public final class ToolFramework {
    public static final String ARCHIVE_SHA = "0b73dcd11ba3b2faa36ca88b70653f25ee4a6e7f924aeb5cc691496bbb58a159";
    public static final String FRAME_SHA = "880ab124adac6670551c55a93b94a75c1a1223289f16081b302c503c7796ab7e";
    public static final String URL_2022 = "https://raw.githubusercontent.com/modder-hub/ApkEditor_Patches_V2/main/ToolReplacement_frame33_aapt_aapt2_mycp_zipalign_aaptz_testkey(Dec_2022).zip";

    private ToolFramework() {
    }

    public static File installed(Context c) {
        return new File(c.getFilesDir(), "bin/framework-33-thirdparty.jar");
    }

    public static String selected(Context c, String defaultPath) throws IOException {
        String selected = PreferenceManager.getDefaultSharedPreferences(c).getString("ToolFramework", "37");
        if (!"33-thirdparty".equals(selected)) {
            return defaultPath;
        }
        File f = installed(c);
        if (!f.isFile() || !FRAME_SHA.equals(hash(read(new FileInputStream(f), 3000000)))) {
            throw new IOException("Framework 33 отсутствует или повреждён. Установите его заново в настройках инструментов.");
        }
        return f.getAbsolutePath();
    }

    public static synchronized void install(Context c) throws Exception {
        synchronized (ToolFramework.class) {
            HttpURLConnection conn = (HttpURLConnection) new URL(URL_2022).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "APK-Editor-Plus/163");
            try {
                if (conn.getResponseCode() != 200) {
                    throw new IOException("HTTP " + conn.getResponseCode());
                }
                byte[] outer = read(conn.getInputStream(), 4000000);
                conn.disconnect();
                if (!ARCHIVE_SHA.equals(hash(outer))) {
                    throw new IOException("SHA-256 архива не совпадает: источник изменился; установка остановлена.");
                }
                byte[] inner = entry(outer, "instruments.zip", 4000000);
                byte[] jar = entry(inner, "res/inst/android-framework.jar", 3000000);
                if (!FRAME_SHA.equals(hash(jar))) {
                    throw new IOException("SHA-256 framework не совпадает.");
                }
                File out = installed(c);
                File dir = out.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new IOException("Не удалось создать каталог инструментов");
                }
                File tmp = new File(dir, "framework-33-thirdparty.tmp");
                try {
                    FileOutputStream stream = new FileOutputStream(tmp);
                    try {
                        stream.write(jar);
                        stream.getFD().sync();
                        stream.close();
                        if (!tmp.renameTo(out)) {
                            throw new IOException("Не удалось сохранить framework");
                        }
                    } finally {
                    }
                } finally {
                    tmp.delete();
                }
            } catch (Throwable th) {
                conn.disconnect();
                throw th;
            }
        }
    }

    private static byte[] entry(byte[] archive, String name, int limit) throws IOException {
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive));
        while (true) {
            try {
                ZipEntry e = zip.getNextEntry();
                if (e != null) {
                    if (e.isDirectory() || !name.equals(e.getName())) {
                        zip.closeEntry();
                    } else {
                        byte[] readNoClose = readNoClose(zip, limit);
                        zip.close();
                        return readNoClose;
                    }
                } else {
                    zip.close();
                    throw new IOException("Не найдено в архиве: " + name);
                }
            } catch (Throwable th) {
                try {
                    zip.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    private static byte[] read(InputStream in, int max) throws IOException {
        try {
            byte[] readNoClose = readNoClose(in, max);
            if (in != null) {
                in.close();
            }
            return readNoClose;
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

    private static byte[] readNoClose(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int total = 0;
        while (true) {
            int n = in.read(b);
            if (n == -1) {
                return out.toByteArray();
            }
            total += n;
            if (total > max) {
                throw new IOException("Файл слишком большой");
            }
            out.write(b, 0, n);
        }
    }

    private static String hash(byte[] data) throws IOException {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder s = new StringBuilder();
            for (byte b : d) {
                s.append(String.format(Locale.US, "%02x", Integer.valueOf(b & 255)));
            }
            return s.toString();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
