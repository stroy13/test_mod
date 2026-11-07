package com.gmail.heagoo.apkeditor;

import android.R;
import android.app.Activity;
import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;

final class EdgeInsets {
    private EdgeInsets() {
    }

    static void install(Activity activity) {
        View content = activity.findViewById(R.id.content);
        if (content == null) {
            return;
        }
        final int l = content.getPaddingLeft();
        final int t = content.getPaddingTop();
        final int r = content.getPaddingRight();
        final int b = content.getPaddingBottom();
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public final WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                return EdgeInsets.lambda$install$0(l, t, r, b, view, windowInsets);
            }
        });
        content.requestApplyInsets();
    }

    static WindowInsets lambda$install$0(int l, int t, int r, int b, View view, WindowInsets insets) {
        int types = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout();
        Insets bars = insets.getInsets(types);
        view.setPadding(bars.left + l, bars.top + t, bars.right + r, bars.bottom + b);
        return new WindowInsets.Builder(insets).setInsets(types, Insets.NONE).build();
    }
}
