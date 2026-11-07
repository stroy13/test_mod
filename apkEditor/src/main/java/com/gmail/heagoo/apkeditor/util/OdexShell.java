package com.gmail.heagoo.apkeditor.util;

/* loaded from: /tmp/apkx/classes9.dex */
public final class OdexShell {
    private OdexShell() {
    }

    public static String quote(String value) {
        if (value == null || value.indexOf(0) >= 0 || value.indexOf(10) >= 0 || value.indexOf(13) >= 0) {
            throw new IllegalArgumentException("Invalid shell argument");
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    public static boolean succeeded(boolean finished, String output) {
        if (!finished || output == null) {
            return false;
        }
        String s = output.trim();
        int pos = s.lastIndexOf(10);
        return s.substring(pos + 1).matches("rc:\\s*0");
    }
}
