package com.gmail.heagoo.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;

public class AppListOrderTest {
    private static final class App {
        final String name;
        final String packageName;
        final long firstInstall;
        final long lastUpdate;

        App(String name, String packageName, long firstInstall, long lastUpdate) {
            this.name = name;
            this.packageName = packageName;
            this.firstInstall = firstInstall;
            this.lastUpdate = lastUpdate;
        }
    }

    @Test
    public void preferenceValuesAreStableAndLegacyLabelsMigrate() {
        String[] ruLabels = {"по названию приложения", "по времени установки"};
        assertEquals(AppListOrder.BY_NAME, AppListOrder.fromPreference("ByAlpha", ruLabels));
        assertEquals(AppListOrder.BY_INSTALL_TIME, AppListOrder.fromPreference("ByInstallTime", ruLabels));
        assertEquals(AppListOrder.BY_NAME, AppListOrder.fromPreference("By App Name", null));
        assertEquals(AppListOrder.BY_INSTALL_TIME, AppListOrder.fromPreference("By Install Time", null));
        assertEquals(AppListOrder.BY_INSTALL_TIME, AppListOrder.fromPreference("По времени установки", ruLabels));
        assertEquals(AppListOrder.BY_INSTALL_TIME, AppListOrder.fromPreference("  по времени установки  ", ruLabels));
        assertEquals(AppListOrder.BY_NAME, AppListOrder.fromPreference("unknown", ruLabels));
        assertEquals(AppListOrder.BY_NAME, AppListOrder.fromPreference(null, ruLabels));
        assertEquals(AppListOrder.BY_INSTALL_TIME, AppListOrder.fromIndex(1));
        assertEquals(AppListOrder.BY_NAME, AppListOrder.fromIndex(99));
    }

    @Test
    public void nameOrderIsNaturalAndDeterministic() {
        List<App> apps = new ArrayList<>(Arrays.asList(
                new App("App10", "c", 1, 100),
                new App("App2", "b", 2, 200),
                new App("App2", "a", 3, 300)));
        apps.sort(AppListOrder.BY_NAME.comparator(a -> a.name, a -> a.firstInstall, a -> a.packageName));
        assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(
                apps.get(0).packageName, apps.get(1).packageName, apps.get(2).packageName));
    }

    @Test
    public void installTimeUsesFirstInstallNotLastUpdate() {
        List<App> apps = new ArrayList<>(Arrays.asList(
                new App("Old", "old", 10, 999),
                new App("New", "new", 20, 1)));
        apps.sort(AppListOrder.BY_INSTALL_TIME.comparator(a -> a.name, a -> a.firstInstall, a -> a.packageName));
        assertEquals("new", apps.get(0).packageName);
        assertEquals("old", apps.get(1).packageName);
    }

    @Test
    public void equalInstallTimesFallBackToNameAndPackage() {
        App a = new App("App2", "a", 10, 1);
        App b = new App("App10", "b", 10, 2);
        Comparator<App> comparator = AppListOrder.BY_INSTALL_TIME.comparator(
                x -> x.name, x -> x.firstInstall, x -> x.packageName);
        assertTrue(comparator.compare(a, b) < 0);
        assertEquals(0, comparator.compare(a, a));
    }
}
