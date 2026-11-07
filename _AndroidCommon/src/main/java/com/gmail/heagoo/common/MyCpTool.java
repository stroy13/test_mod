package com.gmail.heagoo.common;

import android.content.Context;
import android.preference.PreferenceManager;
import java.io.File;

/* loaded from: /tmp/apkx/classes7.dex */
public final class MyCpTool {
    private MyCpTool() {
    }

    public static String command(Context ctx) {
        String mode = PreferenceManager.getDefaultSharedPreferences(ctx).getString("MyCpVersion", "legacy");
        if ("system".equals(mode)) {
            return "/system/bin/cp";
        }
        if ("2022".equals(mode)) {
            File file = new File(ctx.getApplicationInfo().nativeLibraryDir, "libmycp2022_exec.so");
            return file.getAbsolutePath();
        }
        File old = new File(ctx.getFilesDir(), "mycp");
        return old.exists() ? old.getAbsolutePath() : "cp";
    }
}
