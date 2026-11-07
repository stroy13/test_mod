package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.util.RoundedDialogUi;
import com.gmail.heagoo.common.ThemeRegistry;

public final class LabListPreference extends ListPreference {
    public LabListPreference(Context c, AttributeSet a) {
        super(c, a);
    }

    private int dp(int n) {
        return (int) ((n * getContext().getResources().getDisplayMetrics().density) + 0.5f);
    }

    private int color(int a) {
        TypedValue v = new TypedValue();
        getContext().getTheme().resolveAttribute(a, v, true);
        return v.data;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        CharSequence[] entries;
        Context c = getContext();
        CharSequence[] entries2 = getEntries();
        CharSequence[] values = getEntryValues();
        if (entries2 == null || values == null || entries2.length != values.length) {
            throw new IllegalStateException("Invalid list preference");
        }
        LinearLayout panel = RoundedDialogUi.container(c, getDialogTitle());
        LinearLayout linearLayout = new LinearLayout(c);
        int i = 1;
        linearLayout.setOrientation(1);
        int i2 = 0;
        while (i2 < entries2.length) {
            final String val = values[i2].toString();
            boolean active = val.equals(getValue());
            LinearLayout row = new LinearLayout(c);
            row.setGravity(16);
            row.setPadding(dp(14), dp(7), dp(8), dp(7));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(11));
            bg.setColor(color(active ? R.attr.labPressed : R.attr.labSurfaceAlt));
            bg.setStroke(dp(i), color(active ? R.attr.labAccent : R.attr.labBorder));
            row.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(7);
            linearLayout.addView(row, lp);
            if (!"AppTheme".equals(getKey())) {
                entries = entries2;
            } else {
                try {
                    int themeId = Integer.parseInt(val);
                    if (ThemeRegistry.valid(themeId)) {
                        View swatch = new View(c);
                        GradientDrawable dot = new GradientDrawable();
                        dot.setShape(i);
                        dot.setColor(ThemeRegistry.swatch(themeId));
                        dot.setStroke(dp(1), color(R.attr.labBorder));
                        swatch.setBackground(dot);
                        entries = entries2;
                        try {
                            LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(dp(16), dp(16));
                            swatchParams.rightMargin = dp(12);
                            row.addView(swatch, swatchParams);
                        } catch (NumberFormatException e) {
                        }
                    } else {
                        entries = entries2;
                    }
                } catch (NumberFormatException e2) {
                    entries = entries2;
                }
            }
            TextView label = new TextView(c);
            label.setText(entries[i2]);
            label.setTextSize(14.0f);
            label.setTextColor(color(R.attr.labText));
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1.0f));
            RadioButton check = new RadioButton(c);
            check.setButtonTintList(ColorStateList.valueOf(color(active ? R.attr.labAccent : R.attr.labMuted)));
            check.setChecked(active);
            check.setClickable(false);
            check.setFocusable(false);
            row.addView(check);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogInterface d = LabListPreference.this.getDialog();
                    if (d != null) {
                        d.dismiss();
                    }
                    if (LabListPreference.this.callChangeListener(val)) {
                        LabListPreference.this.setValue(val);
                    }
                }
            });
            i2++;
            entries2 = entries;
            i = 1;
        }
        panel.addView(RoundedDialogUi.scroll(c, linearLayout));
        RoundedDialogUi.cancel(panel, new Runnable() {
            @Override
            public final void run() {
                LabListPreference.this.m217xb74aebe3();
            }
        });
        builder.setTitle((CharSequence) null).setView(panel).setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null).setNegativeButton((CharSequence) null, (DialogInterface.OnClickListener) null);
    }

    void m217xb74aebe3() {
        if (getDialog() != null) {
            getDialog().cancel();
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        RoundedDialogUi.transparentWindow(getDialog());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
    }
}
