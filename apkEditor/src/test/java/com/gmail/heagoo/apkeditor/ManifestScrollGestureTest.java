package com.gmail.heagoo.apkeditor;

import org.junit.Test;
import static org.junit.Assert.*;

public class ManifestScrollGestureTest {
    @Test public void swipesScrollButTapsAndSelectionDoNot() {
        assertTrue(ManifestScrollGesture.startsScroll(2, -40, 8, 100, 500, 5000));
        assertTrue(ManifestScrollGesture.startsScroll(2, 40, 8, 100, 500, 5000));
        assertFalse(ManifestScrollGesture.startsScroll(0, 4, 8, 100, 500, 5000));
        assertFalse(ManifestScrollGesture.startsScroll(50, 10, 8, 100, 500, 5000));
        assertFalse(ManifestScrollGesture.startsScroll(0, 40, 8, 600, 500, 5000));
        assertFalse(ManifestScrollGesture.startsScroll(0, 40, 8, 100, 500, 0));
        assertEquals(0, ManifestScrollGesture.clamp(-50, 100));
        assertEquals(100, ManifestScrollGesture.clamp(150, 100));
    }
}
