package com.gmail.heagoo.apkeditor.pro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.gmail.heagoo.common.ThemeRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Guards against screens or palettes drifting back to hard-coded light colours. */
public class ThemeCoverageTest {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final Set<String> THEMED_BASES = new HashSet<>();
    static {
        THEMED_BASES.add("com.gmail.heagoo.common.ThemedActivity");
        THEMED_BASES.add("com.gmail.heagoo.common.ThemedAppCompatActivity");
        THEMED_BASES.add("com.gmail.heagoo.common.ThemedPreferenceActivity");
        THEMED_BASES.add("com.gmail.heagoo.common.CustomizedLangActivity");
    }

    private static Path root() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        assertNotNull("repository root", dir);
        return dir;
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test
    public void everyPaletteHasCompleteActivityAndDialogStyles() throws Exception {
        Path stylesFile = root().resolve("apkEditor/src/main/res/values/styles.xml");
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(stylesFile.toFile()).getDocumentElement();
        Map<String, Element> styles = new HashMap<>();
        NodeList nodes = root.getElementsByTagName("style");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element style = (Element) nodes.item(i);
            styles.put(style.getAttribute("name"), style);
        }
        assertEquals(12, ThemeRegistry.count());
        String[] required = {"labText", "labSurface", "labSurfaceAlt", "labAccent", "labMuted",
                "labBorder", "labPressed", "labHeader", "labError", "android:windowBackground",
                "android:textColorPrimary", "android:forceDarkAllowed"};
        for (int id = 0; id < ThemeRegistry.count(); id++) {
            String name = ThemeRegistry.styleName(id);
            assertTrue(name, styles.containsKey(name));
            Map<String, String> items = resolve(styles, name);
            for (String key : required) {
                assertTrue(name + " missing " + key, items.containsKey(key));
            }
            assertEquals(name, "false", items.get("android:forceDarkAllowed"));
            assertEquals(name, Boolean.toString(!ThemeRegistry.isDark(id)),
                    items.get("android:windowLightStatusBar"));
            assertEquals(name, "@style/" + ThemeRegistry.dialogStyleName(id), items.get("android:dialogTheme"));
            assertEquals(name, "@style/" + ThemeRegistry.dialogStyleName(id), items.get("android:alertDialogTheme"));
            assertEquals(name, "@style/" + ThemeRegistry.dialogStyleName(id), items.get("alertDialogTheme"));

            String dialogName = ThemeRegistry.dialogStyleName(id);
            assertTrue(dialogName, styles.containsKey(dialogName));
            Map<String, String> dialog = resolve(styles, dialogName);
            for (String key : new String[]{"labText", "labSurface", "labAccent", "labMuted",
                    "android:windowBackground", "android:textColorPrimary", "colorSurface"}) {
                assertTrue(dialogName + " missing " + key, dialog.containsKey(key));
            }
            assertEquals(dialogName, "@drawable/lab_dialog_surface", dialog.get("android:windowBackground"));
        }
    }

    private static Map<String, String> resolve(Map<String, Element> styles, String name) {
        Map<String, String> result = new HashMap<>();
        Set<String> seen = new HashSet<>();
        String current = name;
        while (current != null && seen.add(current)) {
            Element style = styles.get(current);
            if (style == null) {
                break;
            }
            NodeList items = style.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                result.putIfAbsent(item.getAttribute("name"), item.getTextContent().trim());
            }
            String parent = style.getAttribute("parent");
            if (parent.isEmpty()) {
                int dot = current.lastIndexOf('.');
                parent = dot > 0 ? current.substring(0, dot) : null;
            }
            current = parent == null ? null : (parent.startsWith("@style/") ? parent.substring("@style/".length()) : parent);
        }
        return result;
    }

    @Test
    public void everyManifestActivityUsesAThemedBaseBeforeSuperOnCreate() throws Exception {
        Path repo = root();
        Map<String, String> bases = new HashMap<>();
        Map<String, Map<String, String>> imports = new HashMap<>();
        Pattern declaration = Pattern.compile(
                "public\\s+(?:final\\s+|abstract\\s+)?class\\s+(\\w+)\\s+extends\\s+([\\w.]+)");
        try (Stream<Path> files = Files.walk(repo)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java")
                    && p.toString().contains("/src/main/java/"))::iterator) {
                String source = read(file);
                Matcher pkg = Pattern.compile("package\\s+([\\w.]+);").matcher(source);
                Matcher cls = declaration.matcher(source);
                if (!pkg.find() || !cls.find()) {
                    continue;
                }
                String fqcn = pkg.group(1) + "." + cls.group(1);
                bases.put(fqcn, cls.group(2));
                Map<String, String> imp = new HashMap<>();
                Matcher im = Pattern.compile("import\\s+([\\w.]+)\\.(\\w+);").matcher(source);
                while (im.find()) {
                    imp.put(im.group(2), im.group(1) + "." + im.group(2));
                }
                imports.put(fqcn, imp);
            }
        }
        Element manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(repo.resolve("apkEditorPro/src/main/AndroidManifest.xml").toFile())
                .getDocumentElement();
        NodeList activities = manifest.getElementsByTagName("activity");
        assertTrue(activities.getLength() >= 30);
        for (int i = 0; i < activities.getLength(); i++) {
            String name = ((Element) activities.item(i)).getAttribute("android:name");
            String current = name;
            Set<String> seen = new HashSet<>();
            while (bases.containsKey(current) && seen.add(current)) {
                if (THEMED_BASES.contains(current)) {
                    break;
                }
                String base = bases.get(current);
                if (base.indexOf('.') >= 0) {
                    current = base;
                } else if (imports.getOrDefault(current, new HashMap<>()).containsKey(base)) {
                    current = imports.get(current).get(base);
                } else {
                    String samePackage = current.substring(0, current.lastIndexOf('.') + 1) + base;
                    current = bases.containsKey(samePackage) ? samePackage : base;
                }
            }
            assertTrue("Activity without themed base: " + name + " -> " + current,
                    THEMED_BASES.contains(current));
        }
        for (String base : new String[]{"ThemedActivity", "ThemedAppCompatActivity", "ThemedPreferenceActivity"}) {
            String source = read(repo.resolve("_AndroidCommon/src/main/java/com/gmail/heagoo/common/" + base + ".java"));
            int apply = source.indexOf("EditorTheme.apply(this)");
            int superCall = source.indexOf("super.onCreate(savedInstanceState)");
            assertTrue(base + " must apply theme", apply >= 0);
            assertTrue(base + " must apply before super.onCreate", apply < superCall);
        }
    }

    @Test
    public void layoutsDoNotReintroduceHardCodedSurfaceOrTextColours() throws Exception {
        Path repo = root();
        Pattern hardCoded = Pattern.compile(
                "android:(?:background|textColor|textColorHint|divider)=\"(#[0-9a-fA-F]+|@color/(?!lab_text_selector)[^\"]+|@android:color/(?:white|black))\"");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(repo)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().contains("/src/main/res/layout")
                    && p.toString().endsWith(".xml"))::iterator) {
                String name = file.getFileName().toString();
                if (name.startsWith("notification_")) {
                    continue; // RemoteViews are rendered by the system, not by the editor theme.
                }
                String source = read(file);
                Matcher matcher = hardCoded.matcher(source);
                while (matcher.find()) {
                    String value = matcher.group(1);
                    boolean pngSwatch = (name.equals("pngeditor_bgcolor.xml") || name.equals("pngeditor_activity.xml"))
                            && (value.equals("@color/white") || value.equals("@color/black"));
                    if (!pngSwatch) {
                        violations.add(repo.relativize(file) + ": " + matcher.group());
                    }
                }
            }
        }
        assertTrue("Hard-coded UI colours:\n" + String.join("\n", violations), violations.isEmpty());
    }

    @Test
    public void customDialogsDoNotBypassTheSelectedDialogTheme() throws Exception {
        Path repo = root();
        try (Stream<Path> files = Files.walk(repo)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java")
                    && p.toString().contains("/src/main/java/"))::iterator) {
                String source = read(file);
                assertFalse(repo.relativize(file) + " uses a non-palette dialog style",
                        source.contains("R.style.Dialog_No_Border_2"));
            }
        }
    }

    @Test
    public void everyPaletteKeepsTextMutedAndAccentReadable() throws Exception {
        Path stylesFile = root().resolve("apkEditor/src/main/res/values/styles.xml");
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(stylesFile.toFile()).getDocumentElement();
        Map<String, Element> styles = new HashMap<>();
        NodeList nodes = root.getElementsByTagName("style");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element style = (Element) nodes.item(i);
            styles.put(style.getAttribute("name"), style);
        }
        for (int id = 0; id < ThemeRegistry.count(); id++) {
            Map<String, String> theme = resolve(styles, ThemeRegistry.styleName(id));
            int surface = parseColor(theme.get("labSurface"));
            assertContrast(ThemeRegistry.styleName(id), parseColor(theme.get("labText")), surface);
            assertContrast(ThemeRegistry.styleName(id), parseColor(theme.get("labMuted")), surface);
            assertContrast(ThemeRegistry.styleName(id), parseColor(theme.get("labAccent")), surface);
            Map<String, String> dialog = resolve(styles, ThemeRegistry.dialogStyleName(id));
            int dialogSurface = parseColor(dialog.get("labSurface"));
            assertContrast(ThemeRegistry.dialogStyleName(id), parseColor(dialog.get("labText")), dialogSurface);
            assertContrast(ThemeRegistry.dialogStyleName(id), parseColor(dialog.get("labMuted")), dialogSurface);
        }
    }

    private static int parseColor(String value) {
        assertNotNull(value);
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() == 8) {
            hex = hex.substring(2);
        }
        return Integer.parseInt(hex, 16);
    }

    private static void assertContrast(String palette, int foreground, int background) {
        double a = luminance(foreground);
        double b = luminance(background);
        double contrast = (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
        assertTrue(palette + " contrast too low: " + contrast, contrast >= 4.5);
    }

    private static double luminance(int color) {
        double[] channels = {((color >> 16) & 255) / 255.0, ((color >> 8) & 255) / 255.0, (color & 255) / 255.0};
        for (int i = 0; i < channels.length; i++) {
            double c = channels[i];
            channels[i] = c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
    }

}
