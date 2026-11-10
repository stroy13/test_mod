package com.gmail.heagoo.apkeditor.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.Assert.*;

public class Aapt2DummyResourcesTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private static final String INPUT = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
            + "  <item type=\"anim\" name=\"dummy_ae_0\">false</item>\n"
            + "  <item type=\"animator\" name=\"dummy_ae_1a\">false</item>\n</resources>\n";
    @Test public void repairsOnlyPureGeneratedFilesInCopy() throws Exception {
        Path res = folder.newFolder("res").toPath();
        Path values = Files.createDirectory(res.resolve("values"));
        Path file = values.resolve("anims.xml");
        Files.write(file, INPUT.getBytes(StandardCharsets.UTF_8));
        Path other = values.resolve("other.xml");
        String mixed = INPUT.replace("</resources>", "<string name=\"user\">hi</string></resources>");
        Files.write(other, mixed.getBytes(StandardCharsets.UTF_8));
        Map<Path, byte[]> repairs = Aapt2DummyResources.findRepairs(res);
        assertEquals(1, repairs.size());
        Path stage = folder.newFolder("stage").toPath();
        Aapt2DummyResources.stage(res, stage, repairs);
        assertEquals(INPUT, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        assertEquals(INPUT.replace(">false</item>", ">@null</item>"),
                new String(Files.readAllBytes(stage.resolve("values/anims.xml")), StandardCharsets.UTF_8));
        assertEquals(mixed, new String(Files.readAllBytes(stage.resolve("values/other.xml")), StandardCharsets.UTF_8));
    }
    @Test public void leavesCommentsAndUserEntriesAlone() {
        assertEquals(INPUT.replace("<resources>", "<resources><!-- example -->"),
                Aapt2DummyResources.normalize(INPUT.replace("<resources>", "<resources><!-- example -->")));
        String named = INPUT.replace("dummy_ae_0", "user_entry");
        assertEquals(named, Aapt2DummyResources.normalize(named));
    }
}
