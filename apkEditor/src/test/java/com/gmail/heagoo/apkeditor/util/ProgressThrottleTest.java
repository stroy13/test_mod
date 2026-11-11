package com.gmail.heagoo.apkeditor.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ProgressThrottleTest {
    @Test public void limitsUpdatesAndAlwaysReportsCompletion() {
        ProgressThrottle throttle = new ProgressThrottle(500);
        assertTrue(throttle.shouldUpdate(0, false));
        assertFalse(throttle.shouldUpdate(1, false));
        assertFalse(throttle.shouldUpdate(499, false));
        assertTrue(throttle.shouldUpdate(500, false));
        assertFalse(throttle.shouldUpdate(501, false));
        assertTrue(throttle.shouldUpdate(502, true));
        assertFalse(throttle.shouldUpdate(503, false));
        assertTrue(throttle.shouldUpdate(1002, false));
    }
}
