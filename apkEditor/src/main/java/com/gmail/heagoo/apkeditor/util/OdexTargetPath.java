package com.gmail.heagoo.apkeditor.util;

import java.io.File;

/* loaded from: /tmp/apkx/classes9.dex */
public final class OdexTargetPath {
    private OdexTargetPath() {
    }

    public static String resolve(String ownSource, String ownOdex, String targetSource) {
        if (ownSource == null || ownOdex == null || targetSource == null) {
            return null;
        }
        File ownDir = new File(ownSource).getParentFile();
        File targetApk = new File(targetSource);
        File targetDir = targetApk.getParentFile();
        if (ownDir == null || targetDir == null) {
            return null;
        }
        String prefix = ownDir.getAbsolutePath() + "/oat/";
        if (!ownOdex.startsWith(prefix)) {
            return null;
        }
        String[] parts = ownOdex.substring(prefix.length()).split("/");
        if (parts.length != 2 || ((!parts[0].equals("arm") && !parts[0].equals("arm64")) || !parts[1].endsWith(".odex"))) {
            return null;
        }
        String name = targetApk.getName();
        if (!name.endsWith(".apk")) {
            return null;
        }
        return new File(new File(new File(targetDir, "oat"), parts[0]), name.substring(0, name.length() - 4) + ".odex").getAbsolutePath();
    }
}
