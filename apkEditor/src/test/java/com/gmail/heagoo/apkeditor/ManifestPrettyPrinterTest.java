package com.gmail.heagoo.apkeditor;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * MT Manager numbers every attribute, which only works when each attribute is
 * its own line. Our AXML decoder emits a whole start tag on one physical line,
 * so the printer splits it for display. The split must stay reversible: joining
 * the lines back is what gets written to disk when a line is edited.
 */
public class ManifestPrettyPrinterTest {

    private static final String MANIFEST =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\""
            + " package=\"com.example.app\" android:versionCode=\"7\">\n"
            + "    <uses-sdk android:minSdkVersion=\"25\" android:targetSdkVersion=\"36\" />\n"
            + "    <!-- a comment\n         spanning lines -->\n"
            + "    <application android:label=\"App\" android:icon=\"@mipmap/ic\">\n"
            + "        <activity android:name=\".Main\" android:exported=\"true\">\n"
            + "            <intent-filter>\n"
            + "                <action android:name=\"android.intent.action.MAIN\" />\n"
            + "            </intent-filter>\n"
            + "        </activity>\n"
            + "    </application>\n"
            + "</manifest>\n";

    private static String render(List<ManifestPrettyPrinter.Line> lines) {
        StringBuilder sb = new StringBuilder();
        for (ManifestPrettyPrinter.Line line : lines) {
            for (int i = 0; i < line.indent; i++) {
                sb.append("    ");
            }
            sb.append(line.text).append('\n');
        }
        return sb.toString();
    }

    private static List<String> signature(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        List<String> out = new ArrayList<>();
        walk(doc.getDocumentElement(), out);
        return out;
    }

    private static void walk(Element element, List<String> out) {
        TreeMap<String, String> attributes = new TreeMap<>();
        NamedNodeMap map = element.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node a = map.item(i);
            attributes.put(a.getNodeName(), a.getNodeValue());
        }
        out.add(element.getTagName() + attributes);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                walk((Element) child, out);
            }
        }
    }

    @Test(timeout = 5000)
    public void eachAttributeGetsItsOwnNumberedLine() {
        List<ManifestPrettyPrinter.Line> lines = ManifestPrettyPrinter.format(MANIFEST);
        int tagIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).text.equals("<manifest")) {
                tagIndex = i;
                break;
            }
        }
        assertTrue("expected a bare <manifest line", tagIndex >= 0);
        assertEquals("xmlns:android=\"http://schemas.android.com/apk/res/android\"",
                lines.get(tagIndex + 1).text);
        assertEquals(tagIndex, lines.get(tagIndex + 1).owner);
        assertEquals("package=\"com.example.app\"", lines.get(tagIndex + 2).text);
        assertEquals(tagIndex, lines.get(tagIndex + 2).owner);
        // The closing bracket stays on the last attribute, as in MT Manager.
        assertEquals("android:versionCode=\"7\">", lines.get(tagIndex + 3).text);
        assertEquals(lines.get(tagIndex).indent + 1, lines.get(tagIndex + 1).indent);
    }

    @Test(timeout = 5000)
    public void splittingRoundTripsToTheSameXml() throws Exception {
        List<ManifestPrettyPrinter.Line> lines = ManifestPrettyPrinter.format(MANIFEST);
        assertEquals(signature(MANIFEST), signature(render(lines)));
    }

    @Test(timeout = 5000)
    public void wholeDocumentOnOneLineIsStillSplit() throws Exception {
        String source = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + " package=\"x.y\"><application android:label=\"A\" android:icon=\"@drawable/i\">"
                + "<activity android:name=\".M\"/></application></manifest>";
        List<ManifestPrettyPrinter.Line> lines = ManifestPrettyPrinter.format(source);
        assertTrue("expected many lines, got " + lines.size(), lines.size() >= 7);
        assertEquals(signature(source), signature(render(lines)));
    }

    @Test(timeout = 5000)
    public void singleAttributeTagsStayOnOneLine() {
        List<ManifestPrettyPrinter.Line> lines = ManifestPrettyPrinter.format(
                "<application><action android:name=\"android.intent.action.MAIN\" /></application>");
        boolean found = false;
        for (ManifestPrettyPrinter.Line line : lines) {
            if (line.text.startsWith("<action ")) {
                found = true;
                assertEquals(-1, line.owner);
                assertTrue(line.text.endsWith(" />"));
            }
        }
        assertTrue("the single-attribute action should stay inline", found);
    }

    @Test(timeout = 5000)
    public void commentsAreNotElements() {
        List<ManifestPrettyPrinter.Line> lines = ManifestPrettyPrinter.format(MANIFEST);
        int marked = 0;
        for (ManifestPrettyPrinter.Line line : lines) {
            if (line.text.startsWith("<!--") || line.text.endsWith("-->")) {
                assertTrue("comment line must be flagged: " + line.text, line.nonElement);
                marked++;
            }
            if (line.text.startsWith("<?xml")) {
                assertTrue("declaration must be flagged", line.nonElement);
            }
        }
        assertEquals("the two-line comment must stay two lines", 2, marked);
    }

    @Test(timeout = 5000)
    public void quotedValuesAreNeverSplitOrRewritten() {
        String source = "<application><meta-data android:name=\"k\" android:value=\"a>b=c\" /></application>";
        List<ManifestPrettyPrinter.Line> lines = ManifestPrettyPrinter.format(source);
        boolean found = false;
        for (ManifestPrettyPrinter.Line line : lines) {
            if (line.text.contains("android:value=")) {
                assertTrue(line.text.contains("\"a>b=c\""));
                found = true;
            }
        }
        assertTrue("the '>' inside a value must not end the tag", found);
    }

    @Test(timeout = 5000)
    public void malformedInputTerminatesAndIsNotLost() {
        String[] bad = {
                "<manifest android:name=\"unterminated",
                "<manifest <!-- unclosed comment",
                "<",
                "<manifest><application>",
                "plain text only",
        };
        for (String source : bad) {
            List<ManifestPrettyPrinter.Line> lines = ManifestPrettyPrinter.format(source);
            assertNotNull(lines);
            assertTrue("output must stay bounded", lines.size() < 1000);
        }
    }

    @Test(timeout = 5000)
    public void emptyInputProducesNoLines() {
        assertTrue(ManifestPrettyPrinter.format("").isEmpty());
        assertTrue(ManifestPrettyPrinter.format(null).isEmpty());
        assertFalse(ManifestPrettyPrinter.format("<manifest/>").isEmpty());
    }
}
