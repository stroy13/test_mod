package com.gmail.heagoo.apkeditor;

import org.junit.Test;
import static org.junit.Assert.*;

public class BuildStageNotificationsTest {
    @Test public void onlyNewStagesAlertAndNewBuildResets() {
        BuildStageNotifications stages = new BuildStageNotifications();
        assertFalse(stages.shouldNotify(0));
        assertTrue(stages.shouldNotify(1));
        assertFalse(stages.shouldNotify(1));
        assertTrue(stages.shouldNotify(2));
        assertFalse(stages.shouldNotify(1));
        stages.reset();
        assertTrue(stages.shouldNotify(1));
    }
}
