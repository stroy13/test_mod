package com.gmail.heagoo.apkeditor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Guards the ru.apkeditor.plus regression where a literal "pro" suffix disabled package editing. */
public class CommonEditPackageFieldTest {
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
    public void packageEditingUsesBuildCapabilityNotApplicationIdSuffix() throws Exception {
        String activity = read(root().resolve("apkEditor/src/main/java/com/gmail/heagoo/apkeditor/CommonEditActivity.java"));
        String gradle = read(root().resolve("apkEditorPlus/build.gradle"));
        assertTrue(gradle.contains("applicationId \"ru.apkeditor.plus\""));
        assertTrue(activity.contains("import com.gmail.heagoo.apkeditor.base.BuildConfig;"));
        assertTrue(activity.contains("boolean canRenamePackage = BuildConfig.IS_PRO;"));
        assertTrue(activity.contains("this.pkgNameEt.setEnabled(canRenamePackage);"));
        assertFalse(activity.contains("getPackageName().endsWith(\"pro\")"));
        assertTrue(activity.contains("this.pkgNameEt.addTextChangedListener(this);"));
    }

    @Test
    public void bothCommonEditLayoutsKeepThePackageFieldInteractive() throws Exception {
        for (String module : new String[]{"apkEditor", "apkEditorPlus"}) {
            Path layout = root().resolve(module + "/src/main/res/layout/activity_commonedit.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Element root = factory.newDocumentBuilder().parse(layout.toFile()).getDocumentElement();
            NodeList edits = root.getElementsByTagName("EditText");
            Element field = null;
            for (int i = 0; i < edits.getLength(); i++) {
                Element edit = (Element) edits.item(i);
                String id = edit.getAttributeNS("http://schemas.android.com/apk/res/android", "id");
                if (id.equals("@+id/et_pkgname") || id.equals("@id/et_pkgname")) {
                    field = edit;
                    break;
                }
            }
            assertNotNull(module, field);
            String android = "http://schemas.android.com/apk/res/android";
            assertFalse("false".equals(field.getAttributeNS(android, "enabled")));
            assertFalse("false".equals(field.getAttributeNS(android, "focusable")));
            assertTrue(field.getAttributeNS(android, "inputType").contains("textNoSuggestions"));
            assertTrue(field.getAttributeNS(android, "hint").contains("lab_package_hint"));
        }
    }
}
