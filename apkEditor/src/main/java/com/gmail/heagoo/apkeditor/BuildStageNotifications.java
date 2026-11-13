package com.gmail.heagoo.apkeditor;

/** Progress callbacks repeat the same index many times; only new steps alert. */
final class BuildStageNotifications {
    private int lastStep;
    synchronized boolean shouldNotify(int step) {
        if (step <= lastStep) return false;
        lastStep = step;
        return true;
    }
    synchronized void reset() { lastStep = 0; }
}
