package com.gmail.heagoo.apkeditor.util;
import org.junit.Test;
import static org.junit.Assert.*;
public class BuildLogDisplayTest {
    @Test public void usesAppropriateUnitsWithoutChangingDiagnostics() {
        assertEquals("999 мс", BuildLogDisplay.formatDuration(999, "ru"));
        assertEquals("1 с", BuildLogDisplay.formatDuration(1000, "ru"));
        assertEquals("5.21 с", BuildLogDisplay.formatDuration(5210, "ru"));
        assertEquals("1 мин 1.234 с", BuildLogDisplay.formatDuration(61234, "ru"));
        assertEquals("1 min 1.234 s", BuildLogDisplay.formatDuration(61234, "en"));
        assertEquals("[21:58 +1.5 с] Компоновка ресурсов: 5.21 с", BuildLogDisplay.render("[21:58 +1500ms] Resource compose total: 5210 ms", "ru"));
        assertEquals("[t +2 мс] error: /tmp/123 ms/foo", BuildLogDisplay.render("[t +2ms] error: /tmp/123 ms/foo", "ru"));
        assertEquals("[t +1 min 1.234 s] Total elapsed: 1 min 1.234 s", BuildLogDisplay.render("[t +61234ms] Total elapsed: 61234 ms", "en"));
    }
    @Test public void displaysStagesButPreservesPathsAndDiagnostics() {
        assertEquals("[10 +2 мс] Начало этапа: компиляция classes.dex", BuildLogDisplay.render("[10 +2ms] Compose stage start: assemble classes.dex", "ru"));
        assertEquals("[t] Добавление в APK: res/merge.xml: sourceMethod=STORED", BuildLogDisplay.render("[t] merge res/merge.xml: sourceMethod=STORED", "ru"));
        String diagnostic="error: missing /tmp/merge.xml"; assertEquals(diagnostic, BuildLogDisplay.render(diagnostic,"ru"));
        assertEquals("ru",BuildLogDisplay.resolveLanguage("system","ru"));
        assertEquals("en",BuildLogDisplay.resolveLanguage("system","fr"));
        assertEquals("en",BuildLogDisplay.resolveLanguage("en","ru"));
    }
}
