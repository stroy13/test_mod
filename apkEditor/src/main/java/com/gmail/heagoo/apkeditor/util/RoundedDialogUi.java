package com.gmail.heagoo.apkeditor.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;

/* loaded from: /tmp/apkx/classes9.dex */
public final class RoundedDialogUi {
    private RoundedDialogUi() {
    }

    public static int dp(Context c, int n) {
        return (int) ((n * c.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public static int color(Context c, int attr) {
        TypedValue t = new TypedValue();
        c.getTheme().resolveAttribute(attr, t, true);
        return t.data;
    }

    public static LinearLayout container(Context c, CharSequence title) {
        LinearLayout panel = new LinearLayout(c);
        panel.setOrientation(1);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color(c, R.attr.labSurface));
        bg.setCornerRadius(dp(c, 18));
        panel.setBackground(bg);
        panel.setClipToOutline(true);
        int p = dp(c, 18);
        panel.setPadding(p, p, p, p);
        TextView heading = new TextView(c);
        heading.setText(title);
        heading.setTextSize(18.0f);
        heading.setTypeface(null, 1);
        heading.setTextColor(color(c, R.attr.labText));
        heading.setPadding(0, 0, 0, dp(c, 12));
        panel.addView(heading);
        return panel;
    }

    public static ScrollView scroll(Context c, View child) {
        final int available = Math.max(dp(c, 160), c.getResources().getDisplayMetrics().heightPixels - dp(c, 240));
        ScrollView scroll = new ScrollView(c) { // from class: com.gmail.heagoo.apkeditor.util.RoundedDialogUi.1
            @Override // android.widget.ScrollView, android.widget.FrameLayout, android.view.View
            protected void onMeasure(int w, int h) {
                int size = View.MeasureSpec.getSize(h);
                int mode = View.MeasureSpec.getMode(h);
                int max = available;
                if (mode != 0) {
                    max = Math.min(size, max);
                }
                super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(max, Integer.MIN_VALUE));
            }
        };
        scroll.setFillViewport(false);
        scroll.addView(child);
        return scroll;
    }

    public static void cancel(LinearLayout panel, final Runnable action) {
        Context c = panel.getContext();
        TextView cancel = new TextView(c);
        cancel.setText(android.R.string.cancel);
        cancel.setTextSize(14.0f);
        cancel.setTextColor(color(c, R.attr.labText));
        cancel.setGravity(17);
        cancel.setBackgroundResource(R.drawable.lab_list_selector);
        cancel.setMinHeight(dp(c, 44));
        cancel.setClickable(true);
        cancel.setFocusable(true);
        cancel.setOnClickListener(new View.OnClickListener() { // from class: com.gmail.heagoo.apkeditor.util.RoundedDialogUi$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                action.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(c, 8);
        panel.addView(cancel, lp);
    }

    public static void transparentWindow(Dialog d) {
        if (d == null || d.getWindow() == null) {
            return;
        }
        d.getWindow().setBackgroundDrawable(new ColorDrawable(0));
    }
}
