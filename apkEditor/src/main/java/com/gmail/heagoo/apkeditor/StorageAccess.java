package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

final class StorageAccess {
    private StorageAccess() {
    }

    static boolean has(Activity a) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        int i = Build.VERSION.SDK_INT;
        return ContextCompat.checkSelfPermission(a, "android.permission.WRITE_EXTERNAL_STORAGE") == 0;
    }

    static void request(final Activity a, int code) {
        if (has(a)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            new AlertDialog.Builder(a).setTitle("Доступ к файлам").setMessage("Для выбора, разборки и сохранения APK этому тестовому редактору нужен доступ ко всем файлам. На следующем экране его можно разрешить или отклонить. Доступ к Android/data и Android/obb может оставаться ограниченным.").setNegativeButton("Отмена", (DialogInterface.OnClickListener) null).setPositiveButton("Открыть настройки", new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    StorageAccess.lambda$request$0(a, dialogInterface, i);
                }
            }).show();
        } else {
            int i = Build.VERSION.SDK_INT;
            ActivityCompat.requestPermissions(a, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, code);
        }
    }

    static void lambda$request$0(Activity a, DialogInterface d, int w) {
        try {
            a.startActivity(new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION", Uri.parse("package:" + a.getPackageName())));
        } catch (ActivityNotFoundException e) {
            try {
                a.startActivity(new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"));
            } catch (Exception e2) {
                Toast.makeText(a, "Откройте настройки доступа ко всем файлам вручную", 1).show();
            }
        }
    }
}
