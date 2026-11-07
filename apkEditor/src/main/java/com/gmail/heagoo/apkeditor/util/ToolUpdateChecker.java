package com.gmail.heagoo.apkeditor.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: /tmp/apkx/classes9.dex */
public final class ToolUpdateChecker {
    private static final long INTERVAL = 86400000;
    private static boolean running;
    private static final ArrayList<Runnable> waiters = new ArrayList<>();

    private ToolUpdateChecker() {
    }

    public static synchronized void check(Context context, boolean force, Runnable callback) {
        synchronized (ToolUpdateChecker.class) {
            Context app = context.getApplicationContext();
            final SharedPreferences p = app.getSharedPreferences("tool_update_status", 0);
            if (running) {
                if (callback != null) {
                    waiters.add(callback);
                }
            } else if (!force && System.currentTimeMillis() - p.getLong("checked", 0L) < INTERVAL) {
                if (callback != null) {
                    callback.run();
                }
            } else {
                if (callback != null) {
                    waiters.add(callback);
                }
                running = true;
                new Thread(new Runnable() { // from class: com.gmail.heagoo.apkeditor.util.ToolUpdateChecker$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ToolUpdateChecker.lambda$check$0(p);
                    }
                }, "Tool-update-check").start();
            }
        }
    }

    static /* synthetic */ void lambda$check$0(SharedPreferences p) {
        String sdk;
        String buildTools;
        String aapt;
        String aaptArm;
        String community;
        ArrayList<Runnable> callbacks;
        try {
            String xml = fetch("https://dl.google.com/android/repository/repository2-1.xml");
            Matcher m = Pattern.compile("path=\"platforms;android-(\\d+)(?:\\.\\d+)?\"").matcher(xml);
            int max = 0;
            while (m.find()) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
            sdk = max > 0 ? "API " + max : "?";
            Matcher m2 = Pattern.compile("path=\"build-tools;(\\d+)\\.(\\d+)\\.(\\d+)\"").matcher(xml);
            int bm = 0;
            int bn = 0;
            int bp = 0;
            while (m2.find()) {
                int a = Integer.parseInt(m2.group(1));
                int b = Integer.parseInt(m2.group(2));
                int c = Integer.parseInt(m2.group(3));
                if (a > bm || (a == bm && (b > bn || (b == bn && c > bp)))) {
                    bm = a;
                    bn = b;
                    bp = c;
                }
            }
            buildTools = bm > 0 ? bm + "." + bn + "." + bp : "?";
        } catch (Exception e) {
            sdk = "offline";
            buildTools = "offline";
        }
        try {
            Matcher m3 = Pattern.compile("<release>([^<]+)</release>").matcher(fetch("https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/maven-metadata.xml"));
            aapt = m3.find() ? m3.group(1) : "?";
        } catch (Exception e2) {
            aapt = "offline";
        }
        try {
            String json = fetch("https://api.github.com/repos/vhynlain/aapt2/releases/latest");
            Matcher m4 = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
            aaptArm = m4.find() ? m4.group(1) : "unknown";
        } catch (Exception e3) {
            aaptArm = "offline";
        }
        try {
            String json2 = fetch("https://api.github.com/repos/modder-hub/ApkEditor_Patches_V2/contents/ToolReplacement_frame33_aapt_aapt2_mycp_zipalign_aaptz_testkey(Dec_2022).zip");
            Matcher m5 = Pattern.compile("\"sha\"\\s*:\\s*\"([0-9a-f]{40})\"").matcher(json2);
            if (m5.find()) {
                community = "ef5b2015403efd450594f92d70f873b5c42e1bc9".equals(m5.group(1)) ? "same" : "changed";
            } else {
                community = "unknown";
            }
        } catch (Exception e4) {
            community = "offline";
        }
        p.edit().putString("sdk", sdk).putString("buildTools", buildTools).putString("aapt", aapt).putString("aaptArm", aaptArm).putString("community", community).putLong("checked", System.currentTimeMillis()).apply();
        synchronized (ToolUpdateChecker.class) {
            running = false;
            callbacks = new ArrayList<>(waiters);
            waiters.clear();
        }
        Handler main = new Handler(Looper.getMainLooper());
        Iterator<Runnable> it = callbacks.iterator();
        while (it.hasNext()) {
            Runnable done = it.next();
            main.post(done);
        }
    }

    private static String fetch(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent", "APK-Editor-Plus/163");
        try {
            if (c.getResponseCode() != 200) {
                throw new IOException("HTTP " + c.getResponseCode());
            }
            InputStream in = c.getInputStream();
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    byte[] b = new byte[8192];
                    int size = 0;
                    while (true) {
                        int n = in.read(b);
                        if (n == -1) {
                            String byteArrayOutputStream = out.toString("UTF-8");
                            out.close();
                            if (in != null) {
                                in.close();
                            }
                            return byteArrayOutputStream;
                        }
                        size += n;
                        if (size > 2000000) {
                            throw new IOException("metadata too large");
                        }
                        out.write(b, 0, n);
                    }
                } finally {
                }
            } finally {
            }
        } finally {
            c.disconnect();
        }
    }
}
