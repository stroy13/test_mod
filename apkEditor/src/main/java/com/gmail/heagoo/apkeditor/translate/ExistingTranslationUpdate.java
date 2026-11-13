package com.gmail.heagoo.apkeditor.translate;

import brut.androlib.res.xml.ResXmlEncoders;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Plan a conservative, text-preserving update of existing Android resource XML files. */
public final class ExistingTranslationUpdate {
    private ExistingTranslationUpdate() {}

    private static String encodedValue(String value) {
        if (value.startsWith("@string/") || value.startsWith("@android:string/")) return value;
        return ResXmlEncoders.encodeAsXmlValue(ResXmlEncoders.escapeXmlChars(value));
    }

    /** Returns changed XML files, retaining unrelated resources and adding missing strings. */
    public static Map<String, String> plan(Map<String, String> files, Map<String, String> updates, Set<String> expectedExisting) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        Set<String> found = new HashSet<>();
        for (Map.Entry<String, String> file : files.entrySet()) {
            String xml = file.getValue();
            if (xml.contains("<!DOCTYPE") || xml.contains("<!ENTITY")) throw new IOException("Unsupported XML declaration: " + file.getKey());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null || !"resources".equals(root.getTagName())) throw new IOException("Invalid resources XML: " + file.getKey());
            String changed = xml;
            for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (!(node instanceof Element)) continue;
                Element element = (Element) node;
                boolean isString = "string".equals(element.getTagName()) || ("item".equals(element.getTagName()) && "string".equals(element.getAttribute("type")));
                String name = element.getAttribute("name");
                if (!isString || !updates.containsKey(name)) continue;
                if (!found.add(name)) throw new IOException("Duplicate string: " + name);
                if (!"string".equals(element.getTagName())) throw new IOException("Unsupported string item: " + name);
                for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() != Node.TEXT_NODE) throw new IOException("Formatted or complex string: " + name);
                }
                Pattern pattern = Pattern.compile("(?s)(<string\\b(?=[^>]*\\bname\\s*=\\s*[\"']" + Pattern.quote(name) + "[\"'])[^>]*>)([^<]*)(</string\\s*>)");
                Matcher matcher = pattern.matcher(changed);
                if (!matcher.find()) throw new IOException("Cannot locate string: " + name);
                int begin = matcher.start(2), end = matcher.end(2);
                String opening = matcher.group(1);
                if (matcher.find()) throw new IOException("Ambiguous string: " + name);
                String value = updates.get(name);
                if (ResXmlEncoders.hasMultipleNonPositionalSubstitutions(value) && !opening.matches("(?s).*\\bformatted\\s*=\\s*['\"]false['\"].*"))
                    throw new IOException("String requires formatted=false: " + name);
                changed = changed.substring(0, begin) + encodedValue(value) + changed.substring(end);
            }
            if (!changed.equals(xml)) result.put(file.getKey(), changed);
        }
        if (!found.containsAll(expectedExisting)) throw new IOException("Existing strings are not uniquely available in project XML");
        StringBuilder added = new StringBuilder();
        for (Map.Entry<String, String> update : updates.entrySet()) {
            if (!update.getKey().matches("[a-z][a-z0-9_]*") || update.getValue() == null) throw new IOException("Invalid resource name or value");
            if (!found.contains(update.getKey())) {
                added.append("<string name=\"").append(update.getKey()).append('\"');
                if (ResXmlEncoders.hasMultipleNonPositionalSubstitutions(update.getValue())) added.append(" formatted=\"false\"");
                added.append('>').append(encodedValue(update.getValue())).append("</string>\n");
            }
        }
        if (added.length() != 0) {
            if (files.isEmpty()) throw new IOException("No existing resource XML available");
            String name = files.containsKey("strings.xml") ? "strings.xml" : files.keySet().iterator().next();
            String xml = result.containsKey(name) ? result.get(name) : files.get(name);
            Matcher closing = Pattern.compile("</resources\\s*>").matcher(xml);
            if (!closing.find()) throw new IOException("Missing resources closing tag");
            int position = closing.start();
            if (closing.find()) throw new IOException("Ambiguous resources closing tag");
            result.put(name, xml.substring(0, position) + "\n" + added + xml.substring(position));
        }
        return result;
    }
}
