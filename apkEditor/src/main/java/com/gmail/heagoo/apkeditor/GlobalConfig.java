package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.gmail.heagoo.common.ThemeRegistry;

public class GlobalConfig {
    public static final int THEME_DARK_DEFAULT = 1;
    public static final int THEME_DARK_RUSSIAN = 2;
    public static final int THEME_LIGHT = 0;
    private static GlobalConfig config;
    private boolean isFullScreen;
    private int themeId;

    private GlobalConfig(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        try {
            String str = sp.getString("AppTheme", "2");
            int id = Integer.valueOf(str).intValue();
            if (ThemeRegistry.valid(id)) {
                this.themeId = id;
            } else {
                this.themeId = THEME_DARK_RUSSIAN;
            }
        } catch (Exception e) {
            this.themeId = THEME_DARK_RUSSIAN;
        }
        this.isFullScreen = sp.getBoolean("FullScreen", false);
    }

    public static GlobalConfig instance(Context ctx) {
        if (config == null) {
            config = new GlobalConfig(ctx);
        }
        return config;
    }

    public boolean isDarkTheme() {
        return ThemeRegistry.isDark(this.themeId);
    }

    public int getThemeId() {
        return this.themeId;
    }

    public boolean isFullScreen() {
        return this.isFullScreen;
    }

    public void updateThemeId(int themeId) {
        this.themeId = ThemeRegistry.valid(themeId) ? themeId : THEME_DARK_RUSSIAN;
    }

    public void setFullScreen(Context ctx, boolean isFullScreen) {
        if (isFullScreen != this.isFullScreen) {
            this.isFullScreen = isFullScreen;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("FullScreen", isFullScreen);
            editor.commit();
        }
    }
}
