package com.gmail.heagoo.apkeditor.util;
import org.junit.Test;
import static org.junit.Assert.*;
public class BuildLogDisplayTest {
    @Test public void displaysStagesButPreservesPathsAndDiagnostics() {
        assertEquals("[10 +2ms] Начало этапа: компиляция classes.dex", BuildLogDisplay.render("[10 +2ms] Compose stage start: assemble classes.dex", "ru"));
        assertEquals("[t] Добавление в APK: res/merge.xml: sourceMethod=STORED", BuildLogDisplay.render("[t] merge res/merge.xml: sourceMethod=STORED", "ru"));
        String diagnostic="error: missing /tmp/merge.xml"; assertEquals(diagnostic, BuildLogDisplay.render(diagnostic,"ru"));
        assertEquals("ru",BuildLogDisplay.resolveLanguage("system","ru"));
        assertEquals("en",BuildLogDisplay.resolveLanguage("system","fr"));
        assertEquals("en",BuildLogDisplay.resolveLanguage("en","ru"));
    }
}
