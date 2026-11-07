package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Guards the app-order preference against translated storage keys again. */
public class SortingResourcesTest {
    private static Path root() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull("repository root", dir);
        return dir;
    }

    private static Element parse(Path path) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(path.toFile()).getDocumentElement();
    }

    private static List<String> arrayItems(Element root, String name) {
        NodeList arrays = root.getElementsByTagName("string-array");
        for (int i = 0; i < arrays.getLength(); i++) {
            Element array = (Element) arrays.item(i);
            if (name.equals(array.getAttribute("name"))) {
                List<String> items = new ArrayList<>();
                NodeList children = array.getElementsByTagName("item");
                for (int j = 0; j < children.getLength(); j++) {
                    items.add(children.item(j).getTextContent().trim());
                }
                return items;
            }
        }
        return null;
    }

    @Test
    public void appOrderValuesAreCanonicalAndNotLocalized() throws Exception {
        Path res = root().resolve("apkEditor/src/main/res");
        Element defaults = parse(res.resolve("values/arrays.xml"));
        assertEquals(List.of("ByAlpha", "ByInstallTime"), arrayItems(defaults, "order_value"));
        assertEquals(2, arrayItems(defaults, "order_key").size());

        try (Stream<Path> paths = Files.list(res)) {
            List<Path> localizedDirs = paths.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("values-"))
                    .collect(Collectors.toList());
            for (Path dir : localizedDirs) {
                Path arrays = dir.resolve("arrays.xml");
                if (!Files.exists(arrays)) continue;
                Element localized = parse(arrays);
                assertFalse("translated order_value in " + dir,
                        arrayItems(localized, "order_value") != null);
                List<String> labels = arrayItems(localized, "order_key");
                if (labels != null) assertEquals(dir.toString(), 2, labels.size());
            }
        }

        Element utilDefaults = parse(root().resolve("appListUtil/src/main/res/values/arrays.xml"));
        assertEquals(List.of("ByAlpha", "ByInstallTime"), arrayItems(utilDefaults, "order_value"));
    }

    @Test
    public void settingsPreferenceUsesLabelsAndCanonicalValues() throws Exception {
        Path settings = root().resolve("apkEditor/src/main/res/xml/settings.xml");
        Element root = parse(settings);
        NodeList preferences = root.getElementsByTagName("*");
        boolean found = false;
        for (int i = 0; i < preferences.getLength(); i++) {
            Element preference = (Element) preferences.item(i);
            if ("AppListOrder".equals(preference.getAttribute("android:key"))) {
                found = true;
                assertEquals("@array/order_key", preference.getAttribute("android:entries"));
                assertEquals("@array/order_value", preference.getAttribute("android:entryValues"));
            }
        }
        assertTrue("AppListOrder preference", found);
    }
}
