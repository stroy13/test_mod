package com.gmail.heagoo.apkeditor;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import java.io.File;
import java.util.Date;

public class AppInfo {
    public String apkPath;
    public String appName;
    public ApplicationInfo applicationInfo;
    public boolean autoRunEnabled;
    public Drawable icon;
    public Date installTime;
    public boolean isSysApp;
    public long lastUpdateTime;
    public String otherInfo;
    public String packagePath;
    public long size;

    public static AppInfo create(PackageManager pm, ApplicationInfo applicationInfo) {
        AppInfo appInfo = new AppInfo();
        appInfo.applicationInfo = applicationInfo;
        appInfo.appName = (String) applicationInfo.loadLabel(pm);
        appInfo.packagePath = applicationInfo.packageName;
        try {
            PackageInfo packageInfo = pm.getPackageInfo(applicationInfo.packageName, 0);
            appInfo.lastUpdateTime = packageInfo.lastUpdateTime;
            File f = new File(applicationInfo.sourceDir);
            appInfo.size = f.length();
        } catch (Throwable th) {
            appInfo.lastUpdateTime = 0L;
        }
        return appInfo;
    }
}
