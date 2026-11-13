package com.gmail.heagoo.apkeditor;

import org.junit.Test;
import static org.junit.Assert.*;

public class NotificationAccessTest {
    @Test public void permissionRequestedOnlyOnceAndOnlyWhereRequired() {
        assertFalse(NotificationAccess.shouldRequest(32, false, false));
        assertTrue(NotificationAccess.shouldRequest(33, false, false));
        assertTrue(NotificationAccess.shouldRequest(37, false, false));
        assertFalse(NotificationAccess.shouldRequest(33, true, false));
        assertFalse(NotificationAccess.shouldRequest(33, false, true));
    }
}
