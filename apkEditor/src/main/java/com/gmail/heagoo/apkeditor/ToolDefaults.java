package com.gmail.heagoo.apkeditor;

import android.content.SharedPreferences;

/** Defaults are installed only for keys the user has not already selected. */
final class ToolDefaults {
    static String aaptForSdk(int sdk) {
        return sdk >= 30 ? "aapt2-37" : sdk >= 28 ? "aapt2" : "legacy";
    }

    static void initialize(SharedPreferences preferences, int sdk) {
        SharedPreferences.Editor editor = preferences.edit();
        if (!preferences.contains("AaptVersion")) editor.putString("AaptVersion", aaptForSdk(sdk));
        if (!preferences.contains("LabZipEngine")) editor.putString("LabZipEngine", "libzip-1.12.1");
        if (!preferences.contains("JadxVersion")) editor.putString("JadxVersion", "modern");
        editor.apply();
    }
}
