package com.gmail.heagoo.applistutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class AppInfoComparatorTest {
    private static AppInfo app(String name, String pkg, long install, boolean system) {
        AppInfo info = new AppInfo();
        info.appName = name;
        info.packagePath = pkg;
        info.firstInstallTime = install;
        info.isSysApp = system;
        return info;
    }

    @Test
    public void userAppsStayBeforeSystemApps() {
        AppInfo system = app("A", "sys", 1, true);
        AppInfo user = app("Z", "user", 2, false);
        assertTrue(new AppInfoComparator("ByAlpha").compare(user, system) < 0);
    }

    @Test
    public void namesUseNaturalOrder() {
        List<AppInfo> apps = new ArrayList<>(Arrays.asList(
                app("App10", "c", 1, false), app("App2", "b", 2, false), app("App1", "a", 3, false)));
        apps.sort(new AppInfoComparator("ByAlpha"));
        assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(
                apps.get(0).packagePath, apps.get(1).packagePath, apps.get(2).packagePath));
    }

    @Test
    public void installTimeIsNewestFirstAndEqualTimesAreStable() {
        List<AppInfo> apps = new ArrayList<>(Arrays.asList(
                app("Old", "old", 10, false), app("New", "new", 20, false), app("Same", "same", 20, false)));
        apps.sort(new AppInfoComparator("ByInstallTime"));
        assertEquals("new", apps.get(0).packagePath);
        assertEquals("same", apps.get(1).packagePath);
        assertEquals("old", apps.get(2).packagePath);
        assertEquals(0, new AppInfoComparator("ByInstallTime").compare(apps.get(0), apps.get(0)));
    }
}
