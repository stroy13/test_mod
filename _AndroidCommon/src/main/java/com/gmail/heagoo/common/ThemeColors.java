package com.gmail.heagoo.common;

import android.content.Context;
import android.util.TypedValue;

/** Resolves palette attributes for code-built views just as XML does. */
public final class ThemeColors {
    private ThemeColors() {
    }

    public static int color(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}
