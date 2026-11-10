package com.gmail.heagoo.apkeditor;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Guards the two v191 UI fixes at source level.
 *
 * <p>1. The manifest code view is styled after MT Manager: a line-number gutter,
 * monospace text, no row dividers. Both the library layout and the Plus override
 * must agree, because the Plus copy is the one that actually wins the resource
 * merge -- editing only the library layout would change nothing in the app.
 *
 * <p>2. The "XML and smali diagnostics" dialog must not resize while it runs. The
 * regression came from a single wrapping TextView: a long path added a line and
 * the centred dialog jumped. Both progress rows must stay pinned to one line.
 */
public class ManifestEditorViewTest {

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
    public void inlineEditorKeepsNumbersWithoutIndentStripes() throws Exception {
        String editor = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ManifestInlineEditor.java");
        assertFalse("indent guides must not be drawn over the code", editor.contains("guideX"));
        assertTrue("the number gutter must remain", editor.contains("Integer.toString(physical)"));
        assertTrue("wrapped-line marker must remain", editor.contains("canvas.drawText(\"↪\""));
    }

    @Test
    public void highlightingIsNotDebouncedUntilScrollingStops() throws Exception {
        String editor = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ManifestInlineEditor.java");
        int start = editor.indexOf("@Override protected void onScrollChanged");
        int end = editor.indexOf("@Override protected void onAttachedToWindow", start);
        assertTrue(start >= 0 && end > start);
        String scrolling = editor.substring(start, end);
        assertTrue(scrolling.contains("queueHighlight(16)"));
        assertFalse(scrolling.contains("invalidateHighlight()"));
        assertFalse(scrolling.contains("cancel(true)"));
        assertTrue(editor.contains("refreshAfterHighlight"));
        assertTrue(editor.contains("getLineForVertical(top) - 45"));
    }

    @Test
    public void bothManifestRowLayoutsCarryTheGutter() throws Exception {
        String[] layouts = {
                "apkEditor/src/main/res/layout/item_manifestline.xml",
                "apkEditorPlus/src/main/res/layout/item_manifestline.xml"
        };
        for (String layout : layouts) {
            String xml = read(layout);
            assertTrue(layout + ": missing line-number gutter", xml.contains("/line_number"));
            assertTrue(layout + ": missing collapse strip", xml.contains("/collapse_icon"));
            assertTrue(layout + ": missing code column", xml.contains("/line_data"));
            assertTrue(layout + ": code must be monospace", xml.contains("monospace"));
            assertTrue(layout + ": code must wrap, not scroll", xml.contains("android:singleLine=\"false\""));
            assertTrue(layout + ": numbers must align to the first line", xml.contains("android:gravity=\"top\""));
        }
    }

    @Test
    public void manifestListDropsRowDividers() throws Exception {
        String src = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ApkInfoActivity.java");
        assertTrue("code rows must not be separated by the theme divider",
                src.contains("this.manifestList.setDivider(null);"));
        assertTrue(src.contains("this.manifestList.setDividerHeight(0);"));
        assertTrue("old list must be hidden when the editable document is shown",
                src.contains("this.manifestList.setVisibility(View.GONE);"));
        String editor = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ManifestInlineEditor.java");
        assertTrue("editable document must use the editor surface", editor.contains("setBackgroundColor(colors.surface);"));
    }

    @Test
    public void analysisDialogPinsBothProgressRowsToOneLine() throws Exception {
        String src = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ApkInfoActivity.java");
        assertTrue(src.contains("TextView progressDetail;"));
        assertTrue("status row must be pinned", src.contains("job.progressText.setLines(1);"));
        assertTrue("path row must be pinned", src.contains("job.progressDetail.setLines(1);"));
        assertTrue(src.contains("job.progressText.setEllipsize(android.text.TextUtils.TruncateAt.END);"));
        assertTrue(src.contains("job.progressDetail.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);"));
        // A bare setText(message) with the embedded newline is what wrapped and
        // resized the dialog; the message must be split across the two rows.
        assertFalse("progress message must not be assigned to one wrapping view",
                src.contains("job.progressText.setText(message);"));
        assertTrue(src.contains("job.progressText.setText(status);"));
        assertTrue(src.contains("job.progressDetail.setText(detail.length() == 0 ? \" \" : detail);"));
    }

    @Test
    public void wrappedLinesCarryTheContinuationMark() {
        String line = "<uses-permission android:name=\"android.permission.REALLY_LONG_PERMISSION_NAME_HERE\" />";
        List<ManifestSyntax.Span> spans = ManifestSyntax.tokenize(line, new ManifestSyntax.State());
        ManifestDisplayFormatter.Plan plan = ManifestDisplayFormatter.plan(line, spans, 200f,
                new ManifestDisplayFormatter.Width() {
                    @Override
                    public float measure(String str, int i, int i2) {
                        return (i2 - i) * 7f;
                    }
                });
        boolean marked = false;
        for (ManifestDisplayFormatter.Insertion ins : plan.insertions) {
            assertTrue("insertion offset outside the line", ins.offset >= 0 && ins.offset <= line.length());
            if (ins.text.indexOf('\n') >= 0) {
                assertTrue("wrapped continuation must start with the MT arrow: " + ins.text,
                        ins.text.startsWith("\n\u21B3"));
                marked = true;
            }
        }
        assertTrue("expected at least one wrapped continuation", marked);
    }

    @Test
    public void namespacePrefixRuleMatchesMtManager() throws Exception {
        String src = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ManifestListAdapter.java");
        // "android:name" -> violet prefix; "xmlns:android" -> violet local part.
        assertTrue(src.contains("boolean declaration = rec.lineData.startsWith(\"xmlns\", span.start);"));
        assertTrue(src.contains("int prefixColor = declaration ? colors.attribute : colors.namespace;"));
        assertTrue(src.contains("int localColor = declaration ? colors.namespace : colors.attribute;"));
        assertEquals("comments are italic in MT Manager", 1,
                countOccurrences(src, "new StyleSpan(Typeface.ITALIC)"));
    }

    @Test
    public void manifestIsSplitIntoNumberedAttributeLines() throws Exception {
        String src = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ManifestListAdapter.java");
        assertTrue("the adapter must run the pretty printer",
                src.contains("ManifestPrettyPrinter.format(source.toString())"));
        assertTrue(src.contains("rec.owner = line.owner;"));
        assertTrue(src.contains("rec.nonElement = line.nonElement;"));
        assertTrue("saving must restore indentation", src.contains("private String buildContent()"));
        assertTrue("long-press on an attribute must act on the whole element",
                src.contains("lineRec = this.allXmlLines.get(lineRec.owner);"));
        assertTrue("comments must not open a section", src.contains("if (lr.nonElement) {"));
    }

    @Test
    public void headerBarExistsInEveryApkInfoLayout() throws Exception {
        String[] layouts = {
                "apkEditor/src/main/res/layout/activity_apkinfo.xml",
                "apkEditor/src/main/res/layout-land/activity_apkinfo.xml",
                "apkEditorPlus/src/main/res/layout/activity_apkinfo.xml",
                "apkEditorPlus/src/main/res/layout-land/activity_apkinfo.xml"
        };
        for (String layout : layouts) {
            String xml = read(layout);
            assertTrue(layout + ": missing header", xml.contains("/manifest_header\""));
            assertTrue(layout + ": missing file name", xml.contains("/manifest_header_name\""));
            assertTrue(layout + ": missing type badge", xml.contains("/manifest_header_type\""));
        }
    }

    @Test
    public void gutterIsPaintedByTheRowBackground() throws Exception {
        String src = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ManifestRowBackground.java");
        assertTrue("the gutter must cover the whole row, not just the first line",
                src.contains("gutterPaint"));
        String adapter = read("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/ManifestListAdapter.java");
        assertFalse("a per-view gutter background stops after one line",
                adapter.contains("viewHolder.lineNumber.setBackgroundColor"));
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }
}
