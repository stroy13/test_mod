package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public final class HomeContentLayout extends LinearLayout {
    public HomeContentLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int mode = View.MeasureSpec.getMode(widthSpec);
        int size = View.MeasureSpec.getSize(widthSpec);
        int max = (int) ((getResources().getDisplayMetrics().density * 440.0f) + 0.5f);
        if (mode != 0 && size > max) {
            widthSpec = View.MeasureSpec.makeMeasureSpec(max, mode);
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
