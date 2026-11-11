package com.gmail.heagoo.apkeditor.util;

/** Limits progress notifications without suppressing completion. Times are monotonic milliseconds. */
public final class ProgressThrottle {
    private final long intervalMillis;
    private boolean hasUpdate;
    private long lastUpdateMillis;

    public ProgressThrottle(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    public boolean shouldUpdate(long nowMillis, boolean complete) {
        if (!hasUpdate || complete || nowMillis - lastUpdateMillis >= intervalMillis) {
            hasUpdate = true;
            lastUpdateMillis = nowMillis;
            return true;
        }
        return false;
    }
}
