package com.gmail.heagoo.apkeditor.translate;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;

public class TranslationOutputTest {
    @Test public void removesClosedLeadingReasoning() throws Exception {
        assertEquals("Привет %s", TranslationOutput.clean("Hello %s", "<think>reasoning</think>\nПривет %s"));
        assertEquals("Привет", TranslationOutput.clean("Hello", "\n</think>\nПривет"));
        assertEquals("Привет", TranslationOutput.clean("Hello", "<think>\n\n</think>\nПривет"));
    }
    @Test public void rejectsIncompleteOrAmbiguousReasoning() {
        for (String output : new String[]{"<think>reasoning", "reasoning</think>Привет", "<think>x<think>y</think>Привет", "<think>x</think>", "Привет<think>x</think>"}) {
            try { TranslationOutput.clean("Hello", output); fail(output); } catch (IOException expected) { }
        }
    }
    @Test public void stillChecksPlaceholders() {
        try { TranslationOutput.clean("Hello %s", "<think>x</think>Привет"); fail(); }
        catch (IOException expected) { }
    }
    @Test public void rejectsAmbiguousSourceMarkers() {
        try { TranslationOutput.clean("<think> hi </think>", "<think> hi </think>"); fail(); }
        catch (IOException expected) { }
    }
}
