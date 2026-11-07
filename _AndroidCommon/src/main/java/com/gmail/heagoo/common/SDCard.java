package com.gmail.heagoo.common;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import com.gmail.heagoo.common.StorageUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/* loaded from: /tmp/apkx/classes7.dex */
public class SDCard {
    public static boolean exist() {
        if (Environment.getExternalStorageState().equals("mounted")) {
            return true;
        }
        return false;
    }

    public static String getRootDirectory() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public static synchronized String getEditorDirectory() {
        String path;
        synchronized (SDCard.class) {
            path = new File(getRootDirectory(), "Apk Editor Plus").getPath();
        }
        return path;
    }

    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[4096];
        while (true) {
            int count = is.read(buffer);
            if (count > 0) {
                os.write(buffer, 0, count);
            } else {
                return;
            }
        }
    }

    public static String makeWorkingDir(Context ctx) throws Exception {
        return makeDir(ctx, "tmp");
    }

    public static String makeBackupDir(Context ctx) throws Exception {
        return makeDir(ctx, "backup");
    }

    public static String makeImageDir(Context ctx) throws Exception {
        return makeDir(ctx, "image");
    }

    public static String makeDir(Context ctx, String dirName) throws Exception {
        if (!exist()) {
            throw new Exception("Can not find sd card.");
        }
        String subDir = "";
        String packagePath = ctx.getPackageName();
        if (packagePath.startsWith("com.gmail.heagoo.apkpermremover")) {
            subDir = "/.ApkPermRemover/" + dirName + "/";
        } else if (packagePath.startsWith("com.gmail.heagoo.pmaster")) {
            subDir = "/PermMaster/" + dirName + "/";
        } else if (packagePath.equals("com.gmail.heagoo.permissionmanager")) {
            subDir = "/PermMaster/" + dirName + "/";
        } else {
            if (packagePath.equals("ru.apkeditor.plus")) {
                String target = getEditorDirectory() + "/" + dirName + "/";
                File dir = new File(target);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                return target;
            }
            if (packagePath.startsWith("com.gmail.heagoo.apkeditor")) {
                subDir = "/ApkEditor/" + dirName + "/";
            } else if (packagePath.startsWith("com.gmail.heagoo.appdm")) {
                subDir = "/HackAppData/" + dirName + "/";
            }
        }
        String rootDir = getRootDirectory();
        String targetDir = rootDir + subDir;
        File f = new File(targetDir);
        if (!f.exists()) {
            f.mkdirs();
        }
        return targetDir;
    }

    public static String getExternalStoragePath(Context ctx) {
        int i = Build.VERSION.SDK_INT;
        String internalPath = Environment.getExternalStorageDirectory().getPath();
        File[] files = ctx.getExternalFilesDirs(null);
        if (files != null) {
            int appendedLen = 0;
            int length = files.length;
            int i2 = 0;
            while (true) {
                if (i2 >= length) {
                    break;
                }
                File f = files[i2];
                String path = f.getPath();
                if (!path.startsWith(internalPath)) {
                    i2++;
                } else {
                    appendedLen = path.length() - internalPath.length();
                    break;
                }
            }
            for (File f2 : files) {
                String path2 = f2.getPath();
                if (!path2.startsWith(internalPath)) {
                    return path2.substring(0, path2.length() - appendedLen);
                }
            }
        }
        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList();
        if (storageList == null) {
            return null;
        }
        for (StorageUtils.StorageInfo si : storageList) {
            if (!si.internal && !si.readonly) {
                return si.path;
            }
        }
        return null;
    }
}
