package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import com.gmail.heagoo.apkeditor.RecentHistory;
import java.util.List;

final class RecentFiles {
    private static final String KEY = "entries";
    private static final String PREFS = "recent_apk_files";

    private RecentFiles() {
    }

    static synchronized List<RecentHistory.Entry> list(Context c) {
        List<RecentHistory.Entry> parse;
        synchronized (RecentFiles.class) {
            parse = RecentHistory.parse(c.getSharedPreferences(PREFS, 0).getString(KEY, ""));
        }
        return parse;
    }

    static synchronized void record(Context c, String path, String pkg) {
        synchronized (RecentFiles.class) {
            SharedPreferences p = c.getSharedPreferences(PREFS, 0);
            p.edit().putString(KEY, RecentHistory.serialize(RecentHistory.add(RecentHistory.parse(p.getString(KEY, "")), path, pkg))).apply();
        }
    }

    static synchronized void remove(Context c, RecentHistory.Entry e) {
        synchronized (RecentFiles.class) {
            SharedPreferences p = c.getSharedPreferences(PREFS, 0);
            p.edit().putString(KEY, RecentHistory.serialize(RecentHistory.remove(RecentHistory.parse(p.getString(KEY, "")), e))).apply();
        }
    }
}
