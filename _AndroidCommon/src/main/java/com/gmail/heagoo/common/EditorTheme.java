package com.gmail.heagoo.common;

import android.app.Activity;
import android.preference.PreferenceManager;
import java.util.WeakHashMap;

/* loaded from: /tmp/apkx/classes7.dex */
public final class EditorTheme {
    private static final WeakHashMap<Activity, Integer> APPLIED = new WeakHashMap<>();

    private EditorTheme() {
    }

    public static int selected(Activity activity) {
        try {
            int id = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("AppTheme", "0"));
            if (ThemeRegistry.valid(id)) {
                return id;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void apply(Activity activity) {
        int id = selected(activity);
        String name = ThemeRegistry.styleName(id);
        int res = activity.getResources().getIdentifier(name, "style", activity.getPackageName());
        if (res != 0) {
            activity.setTheme(res);
        }
        APPLIED.put(activity, Integer.valueOf(id));
    }

    public static boolean isDark(int id) {
        return ThemeRegistry.isDark(id);
    }

    public static boolean needsRecreate(Activity activity) {
        Integer old = APPLIED.get(activity);
        return (old == null || old.intValue() == selected(activity)) ? false : true;
    }
}
