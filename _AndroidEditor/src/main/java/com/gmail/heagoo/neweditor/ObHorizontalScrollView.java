package com.gmail.heagoo.neweditor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

public class ObHorizontalScrollView extends HorizontalScrollView {
    boolean mFastDirty;

    public ObHorizontalScrollView(Context context) {
        super(context);
        this.mFastDirty = true;
    }

    public ObHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFastDirty = true;
    }

    public ObHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFastDirty = true;
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        ObEditText child2 = (ObEditText) child;
        child2.fastMeasure(getChildMeasureSpec(parentWidthMeasureSpec, lp.leftMargin + lp.rightMargin + widthUsed, lp.width), getChildMeasureSpec(parentHeightMeasureSpec, lp.topMargin + lp.bottomMargin + heightUsed, lp.height));
    }
}
