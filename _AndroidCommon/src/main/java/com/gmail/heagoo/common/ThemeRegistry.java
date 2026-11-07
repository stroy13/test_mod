package com.gmail.heagoo.common;

/* loaded from: /tmp/apkx/classes7.dex */
public final class ThemeRegistry {
    private static final String[] STYLES = {"EditorThemeLight", "EditorThemeDark", "EditorThemeDarkAlt", "EditorThemeAmoled", "EditorThemeViolet", "EditorThemeOcean", "EditorThemeWine", "EditorThemeGraphite", "EditorThemeRose", "EditorThemeIce", "EditorThemeMint", "EditorThemeSand"};
    private static final int[] ACCENTS = {-13214523, -6571777, -7940673, -2039584, -3033857, -8333067, -20803, -13953, -19504, -13013308, -14386072, -5939662};

    private ThemeRegistry() {
    }

    public static int count() {
        return STYLES.length;
    }

    public static boolean valid(int id) {
        return id >= 0 && id < count();
    }

    public static String styleName(int id) {
        return STYLES[valid(id) ? id : 0];
    }

    public static String dialogStyleName(int id) {
        String style = styleName(id);
        return "EditorDialog" + style.substring("EditorTheme".length());
    }

    public static boolean isDark(int id) {
        return valid(id) && id > 0 && id < 9;
    }

    public static int accent(int id) {
        return ACCENTS[valid(id) ? id : 0];
    }

    public static int swatch(int id) {
        if (id == 3) {
            return -16777216;
        }
        return accent(id);
    }
}
