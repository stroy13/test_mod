package com.gmail.heagoo.apkeditor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OutputPathAndUiDefaultsTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static Path root() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir);
        return dir;
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test
    public void outputPathNeverReusesExistingResult() throws Exception {
        File dir = folder.newFolder("out");
        String input = new File(dir, "source.apk").getAbsolutePath();
        String first = ApkInfoActivity.createOutputPath(input, dir.getAbsolutePath(), "gen_signed");
        assertEquals(new File(dir, "gen_signed.apk").getAbsolutePath(), first);
        assertTrue(new File(first).createNewFile());
        String second = ApkInfoActivity.createOutputPath(input, dir.getAbsolutePath(), "gen_signed");
        assertEquals(new File(dir, "gen_signed2.apk").getAbsolutePath(), second);
        assertTrue(new File(second).createNewFile());
        String third = ApkInfoActivity.createOutputPath(input, dir.getAbsolutePath(), "gen_signed");
        assertEquals(new File(dir, "gen_signed3.apk").getAbsolutePath(), third);
    }

    @Test
    public void defaultThemeIsDark1Everywhere() throws Exception {
        String registry = read(root().resolve("_AndroidCommon/src/main/java/com/gmail/heagoo/common/ThemeRegistry.java"));
        assertTrue(registry.contains("public static final int DEFAULT_THEME = 1;"));
        assertTrue(registry.contains("\"EditorThemeDark\""));
        // Index 1 must stay "Dark Theme 1" in both label arrays.
        String labels = read(root().resolve("apkEditor/src/main/res/values/arrays.xml"));
        assertTrue(labels.contains("<item>Dark Theme 1</item>"));
        String labelsRu = read(root().resolve("apkEditor/src/main/res/values-ru/arrays.xml"));
        assertTrue(labelsRu.contains("<item>\u0422\u0451\u043c\u043d\u0430\u044f \u0442\u0435\u043c\u0430 1</item>"));

        String editorTheme = read(root().resolve("_AndroidCommon/src/main/java/com/gmail/heagoo/common/EditorTheme.java"));
        assertTrue(editorTheme.contains("getString(\"AppTheme\", \"1\")"));
        String config = read(root().resolve("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/GlobalConfig.java"));
        assertTrue(config.contains("getString(\"AppTheme\", String.valueOf(ThemeRegistry.DEFAULT_THEME))"));
        String colorTheme = read(root().resolve("_AndroidEditor/src/main/java/com/gmail/heagoo/neweditor/ColorTheme.java"));
        assertTrue(colorTheme.contains("getString(\"AppTheme\", String.valueOf(ThemeRegistry.DEFAULT_THEME))"));
        String styles = read(root().resolve("apkEditor/src/main/res/values/styles.xml"));
        assertTrue(styles.contains("<style name=\"AppTheme\" parent=\"@style/EditorThemeDark\" />"));
        for (String module : new String[]{"apkEditor", "apkEditorPlus"}) {
            String settings = read(root().resolve(module + "/src/main/res/xml/settings.xml"));
            assertTrue(settings.contains("android:key=\"AppTheme\" android:defaultValue=\"1\""));
        }
    }

    @Test
    public void drawerHasNoAboutEntryButHomeKeepsInfo() throws Exception {
        String adapter = read(root().resolve("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/MenuListAdapter.java"));
        assertFalse(adapter.contains("R.string.about"));
        assertTrue(adapter.contains("int[] itemIds = {0, 4}"));
        String main = read(root().resolve("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/MainActivity.java"));
        assertTrue(main.contains("home_info_button"));
    }

    @Test
    public void visibleBrandingIsPlusAndModuleRenamed() throws Exception {
        String settingsGradle = read(root().resolve("settings.gradle"));
        assertTrue(settingsGradle.contains("include ':apkEditorPlus'"));
        assertFalse(settingsGradle.contains("apkEditorPro"));
        String appName = read(root().resolve("apkEditorPlus/src/main/res/values/strings.xml"));
        assertTrue(appName.contains("<string name=\"app_name\">APK Editor Plus</string>"));
        String gradle = read(root().resolve("apkEditorPlus/build.gradle"));
        assertTrue(gradle.contains("namespace 'com.gmail.heagoo.apkeditor.plus'"));
        assertTrue(gradle.contains("applicationId \"ru.apkeditor.plus\""));
        String proguard = read(root().resolve("apkEditorPlus/proguard-project.txt"));
        assertTrue(proguard.contains("-keep class com.gmail.heagoo.apkeditor.plus.**"));
        assertFalse(proguard.contains("com.gmail.heagoo.apkeditor.pro"));
    }
}
