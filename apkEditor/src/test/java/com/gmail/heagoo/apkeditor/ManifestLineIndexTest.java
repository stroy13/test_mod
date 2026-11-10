package com.gmail.heagoo.apkeditor;

import org.junit.Test;
import java.util.Random;
import static org.junit.Assert.assertEquals;

public class ManifestLineIndexTest {
    @Test public void editsKeepPhysicalLineNumbersCorrect() {
        Random random = new Random(421);
        ManifestLineIndex index = new ManifestLineIndex();
        StringBuilder text = new StringBuilder();
        for (int edit = 0; edit < 3000; edit++) {
            int start = random.nextInt(text.length() + 1);
            int before = random.nextInt(text.length() - start + 1);
            String[] additions = {"", "x", "\n", "a\nb\n", "\n\n", "xy"};
            String added = additions[random.nextInt(additions.length)];
            text.replace(start, start + before, added);
            index.replace(text, start, before, added.length());
            int line = 1;
            for (int i = 0; i <= text.length(); i++) {
                if (i > 0 && text.charAt(i - 1) == '\n') line++;
                assertEquals("edit " + edit + " offset " + i, line, index.lineAt(i));
            }
        }
    }
}
