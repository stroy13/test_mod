package com.gmail.heagoo.common;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.TypedValue;
import android.preference.PreferenceManager;
import java.util.Map;
import java.util.WeakHashMap;

/** Applies the palette selected in settings before an activity inflates any view. */
public final class EditorTheme {
    private static final Map<Activity, Integer> APPLIED = new WeakHashMap<>();

    private EditorTheme() {
    }

    public static int selected(Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        try {
            int id = Integer.parseInt(prefs.getString("AppTheme", "1"));
            return ThemeRegistry.valid(id) ? id : ThemeRegistry.DEFAULT_THEME;
        } catch (RuntimeException invalidValue) {
            return ThemeRegistry.DEFAULT_THEME;
        }
    }

    public static void apply(Activity activity) {
        int id = selected(activity);
        Resources resources = activity.getResources();
        int style = resources.getIdentifier(ThemeRegistry.styleName(id), "style", activity.getPackageName());
        if (style != 0) {
            activity.setTheme(style);
        }
        synchronized (APPLIED) {
            APPLIED.put(activity, id);
        }
    }

    public static boolean needsRecreate(Activity activity) {
        synchronized (APPLIED) {
            Integer applied = APPLIED.get(activity);
            return applied != null && applied.intValue() != selected(activity);
        }
    }

    /** Dialog theme matching the activity palette (android:dialogTheme). */
    public static int dialogStyle(Activity activity) {
        TypedValue value = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.dialogTheme, value, true);
        return value.resourceId;
    }

    public static boolean isDark(int id) {
        return ThemeRegistry.isDark(id);
    }

    public static boolean isDark(Activity activity) {
        return ThemeRegistry.isDark(selected(activity));
    }
}
