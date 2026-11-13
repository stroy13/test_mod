package com.gmail.heagoo.apkeditor.util;
import java.util.Locale;
public final class ApkFileName {
    private ApkFileName() {}
    public static boolean isApk(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".apk");
    }
}
