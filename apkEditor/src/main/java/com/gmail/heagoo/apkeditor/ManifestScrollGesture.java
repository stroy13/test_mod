package com.gmail.heagoo.apkeditor;

/** Distinguishes a vertical swipe from a tap, horizontal selection, or long press. */
final class ManifestScrollGesture {
    private ManifestScrollGesture() { }
    static boolean startsScroll(float dx, float dy, int touchSlop, long elapsed, long longPressTimeout, int range) {
        return range > 0 && Math.abs(dy) > touchSlop && Math.abs(dy) > Math.abs(dx)
                && elapsed < longPressTimeout;
    }
    static int clamp(int position, int range) { return Math.max(0, Math.min(range, position)); }
}
