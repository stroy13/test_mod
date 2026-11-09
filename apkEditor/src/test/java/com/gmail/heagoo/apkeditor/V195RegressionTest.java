package com.gmail.heagoo.apkeditor;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Source-level guards for the five v195 regressions. The affected classes need
 * an Android runtime, so these checks pin the wiring that unit tests cannot
 * exercise directly; the pure-Java pieces have their own tests.
 */
public class V195RegressionTest {

    private static Path root() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir);
        return dir;
    }

    private static String read(String relative) throws Exception {
        return new String(Files.readAllBytes(root().resolve(relative)), StandardCharsets.UTF_8);
    }

    @Test
    public void executeDexStagesPrivatelyAndLoadsOnlyAfterChmod() throws Exception {
        String src = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/patch/PatchRule_ExecDex.java");
        assertTrue(src.contains("activity.getDir(\"patch_dex\", Context.MODE_PRIVATE)"));
        assertTrue(src.contains("PatchDexFile.extract(dexDirectory, patchZip.getInputStream(ze))"));
        assertFalse("shared storage produced the writable-dex SecurityException",
                src.contains("SDCard.makeWorkingDir"));
        int extract = src.indexOf("PatchDexFile.extract");
        int loader = src.indexOf("new DexClassLoader");
        assertTrue("class loader must be constructed after read-only staging", extract > 0 && loader > extract);
        String helper = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/patch/PatchDexFile.java");
        assertTrue(helper.contains("file.setReadOnly()"));
        assertTrue(helper.contains("file.canWrite()"));
    }

    @Test
    public void assignmentsAndStartLineAreNotPlainWhite() throws Exception {
        String assign = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/patch/PatchRule_MatchAssign.java");
        assertTrue(assign.contains("patchCtx.assignment(name, assignedVal);"));
        assertFalse(assign.contains("patchCtx.info(\"%s=\\\"%s\\\"\", false"));
        String dialog = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/PatchDialog.java");
        assertTrue(dialog.contains("PatchLogPalette.ASSIGN"));
        assertTrue(dialog.contains("appendStyledText(getContext().getString(R.string.patch_started_log, this.patchPath) + Document.LINE_SEPARATOR_UNIX, PatchLogPalette.PHASE);"));
        assertTrue("plain text must use the palette, not the theme's white", dialog.contains("this.logTv.setTextColor(logPalette().text);"));
        String buffer = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/PatchLogBuffer.java");
        assertTrue(buffer.contains("public static final int MAX_STYLE = ASSIGN;"));
    }

    @Test
    public void buildWarningIsPersistentAndReplayed() throws Exception {
        String activity = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ApkComposeActivity.java");
        assertTrue(activity.contains("this.msgHandler.warning(message);"));
        assertFalse("a toast truncates and disappears", activity.contains("this.msgHandler.toast(message);"));
        assertTrue(activity.contains("R.id.layout_build_warning"));
        assertTrue(activity.contains("R.id.btn_copy_build_warning"));
        String service = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ApkComposeService.java");
        assertTrue(service.contains("composeResult.warnings.add(message)"));
        assertTrue(service.contains("_observer.taskWarning(warning);"));
        for (String layout : new String[]{
                "apkEditor/src/main/res/layout/activity_apkcompose.xml",
                "apkEditorPlus/src/main/res/layout/activity_apkcompose.xml"}) {
            String xml = read(layout);
            assertTrue(layout, xml.contains("tv_build_warning"));
            assertTrue(layout, xml.contains("btn_dismiss_build_warning"));
            assertFalse(layout + " must not ellipsize the warning", xml.contains("android:ellipsize"));
        }
    }

    @Test
    public void customKeyIsSelectedByValueNotByAHardCodedFourthRow() throws Exception {
        String pref = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/KeyListPreference.java");
        assertTrue(pref.contains("static final String CUSTOM_VALUE = \"customkey\";"));
        assertTrue(pref.contains("CUSTOM_VALUE.equals(value)"));
        assertTrue(pref.contains("new KeySelectDlgHelper(KeyListPreference.this).showDialog"));
        assertFalse("the old fourth-row assumption closed the dialog silently", pref.contains("which != 3"));
        assertFalse(pref.contains("getEntryValues()[3]"));
        String arrays = read("apkEditor/src/main/res/values/arrays.xml");
        int values = arrays.indexOf("<string-array name=\"signer_value_dont_translate\">");
        int end = arrays.indexOf("</string-array>", values);
        String block = arrays.substring(values, end);
        assertTrue(block.contains("<item>customkey</item>"));
        assertFalse("there are only three signer choices", block.contains("<item>legacykey</item>"));
    }

    @Test
    public void paletteContrastIsBodyTextGrade() throws Exception {
        String palette = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/patch/PatchLogPalette.java");
        assertTrue(palette.contains("contrast(color, surface) < 4.5d"));
        assertTrue(palette.contains("public static final int ASSIGN = 6;"));
    }
}
