package com.gmail.heagoo.applistutil;

import com.gmail.heagoo.common.AppListOrder;
import java.util.Comparator;

public class AppInfoComparator implements Comparator<AppInfo> {
    private final Comparator<AppInfo> delegate;

    public AppInfoComparator() {
        this(null);
    }

    public AppInfoComparator(String appOrder) {
        delegate = AppListOrder.fromPreference(appOrder, null).comparator(
                info -> info.appName, info -> info.firstInstallTime, info -> info.packagePath);
    }

    @Override
    public int compare(AppInfo a, AppInfo b) {
        if (a.isSysApp != b.isSysApp) return a.isSysApp ? 1 : -1;
        return delegate.compare(a, b);
    }
}
