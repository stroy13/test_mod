package com.gmail.heagoo.apkeditor.util;

import android.content.Context;
import android.preference.PreferenceManager;
import com.gmail.heagoo.apkeditor.MainActivity;
import java.io.IOException;

/* loaded from: /tmp/apkx/classes9.dex */
public final class ZipEngine {
    private ZipEngine() {
    }

    private static boolean modern(Context c) {
        return "libzip-1.12.1".equals(PreferenceManager.getDefaultSharedPreferences(c).getString("LabZipEngine", "legacy"));
    }

    public static void merge(Context c, String a, String b, String r, int rl, String m, int ml) throws IOException {
        if (!modern(c)) {
            MainActivity.mg(a, b, r, rl, m, ml);
            return;
        }
        try {
            ZipEngineNew.merge(a, b, r, rl, m, ml);
        } catch (LinkageError e) {
            throw new IOException("Cannot load libzip 1.12.1 for this device", e);
        }
    }

    public static void modify(Context c, String a, String b, String n, int nl, String d, int dl, String r, int rl) throws IOException {
        if (!modern(c)) {
            MainActivity.md(a, b, n, nl, d, dl, r, rl);
            return;
        }
        try {
            ZipEngineNew.modify(a, b, n, nl, d, dl, r, rl);
        } catch (LinkageError e) {
            throw new IOException("Cannot load libzip 1.12.1 for this device", e);
        }
    }
}
