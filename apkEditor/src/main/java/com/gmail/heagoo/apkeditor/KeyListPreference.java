package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;

public class KeyListPreference extends ListPreference {
    private static final int CUSTOM = 3;

    public KeyListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private int dp(float n) {
        return (int) ((getContext().getResources().getDisplayMetrics().density * n) + 0.5f);
    }

    private int color(int attr) {
        TypedValue t = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, t, true);
        return t.data;
    }

    private GradientDrawable background(boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(12.0f));
        d.setColor(color(selected ? R.attr.labPressed : R.attr.labSurfaceAlt));
        d.setStroke(dp(1.0f), color(selected ? R.attr.labAccent : R.attr.labBorder));
        return d;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        Context c = getContext();
        LinearLayout linearLayout = new LinearLayout(c);
        linearLayout.setOrientation(1);
        float f = 12.0f;
        linearLayout.setPadding(dp(20.0f), dp(20.0f), dp(20.0f), dp(12.0f));
        linearLayout.setBackgroundColor(color(R.attr.labSurface));
        TextView title = new TextView(c);
        title.setText(getDialogTitle());
        title.setTextColor(color(R.attr.labText));
        title.setTextSize(19.0f);
        title.setTypeface(Typeface.DEFAULT, 1);
        linearLayout.addView(title);
        TextView hint = new TextView(c);
        hint.setText(R.string.lab_key_hint);
        hint.setTextColor(color(R.attr.labMuted));
        hint.setTextSize(12.0f);
        hint.setPadding(0, dp(6.0f), 0, dp(14.0f));
        linearLayout.addView(hint);
        CharSequence[] entries = getEntries();
        CharSequence[] values = getEntryValues();
        String current = getValue();
        int i = 0;
        while (i < entries.length) {
            final int which = i;
            boolean active = values[i].toString().equals(current);
            LinearLayout linearLayout2 = new LinearLayout(c);
            linearLayout2.setGravity(16);
            TextView title2 = title;
            linearLayout2.setPadding(dp(f), dp(8.0f), dp(10.0f), dp(8.0f));
            linearLayout2.setBackground(background(active));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8.0f);
            linearLayout.addView(linearLayout2, lp);
            LinearLayout text = new LinearLayout(c);
            text.setOrientation(1);
            linearLayout2.addView(text, new LinearLayout.LayoutParams(0, -2, 1.0f));
            TextView label = new TextView(c);
            label.setText(entries[i]);
            label.setTextSize(14.0f);
            label.setTypeface(Typeface.DEFAULT, 1);
            label.setTextColor(color(R.attr.labText));
            text.addView(label);
            TextView desc = new TextView(c);
            desc.setText(i == 0 ? R.string.lab_key_test_desc : i == 1 ? R.string.lab_key_legacy_desc : i == 2 ? R.string.lab_key_editor_desc : R.string.lab_key_custom_desc);
            desc.setTextSize(11.0f);
            desc.setTextColor(color(R.attr.labMuted));
            desc.setPadding(0, dp(4.0f), 0, 0);
            text.addView(desc);
            RadioButton radio = new RadioButton(c);
            radio.setButtonTintList(ColorStateList.valueOf(color(active ? R.attr.labAccent : R.attr.labMuted)));
            radio.setChecked(active);
            radio.setClickable(false);
            radio.setFocusable(false);
            linearLayout2.addView(radio);
            linearLayout2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogInterface dialog = KeyListPreference.this.getDialog();
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    if (which != 3) {
                        if (which == 2) {
                            new AlertDialog.Builder(KeyListPreference.this.getContext()).setTitle(R.string.lab_key_editor_name).setMessage(R.string.lab_key_editor_desc).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    String val = KeyListPreference.this.getEntryValues()[which].toString();
                                    if (KeyListPreference.this.callChangeListener(val)) {
                                        KeyListPreference.this.setValue(val);
                                    }
                                }
                            }).show();
                            return;
                        }
                        String val = KeyListPreference.this.getEntryValues()[which].toString();
                        if (KeyListPreference.this.callChangeListener(val)) {
                            KeyListPreference.this.setValue(val);
                            return;
                        }
                        return;
                    }
                    new KeySelectDlgHelper(KeyListPreference.this).showDialog(KeyListPreference.this.getContext());
                }
            });
            i++;
            title = title2;
            f = 12.0f;
        }
        ScrollView scrollView = new ScrollView(c);
        scrollView.addView(linearLayout);
        builder.setTitle((CharSequence) null).setView(scrollView).setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
    }

    public void setCustomValue() {
        String value = getEntryValues()[3].toString();
        if (callChangeListener(value)) {
            setValue(value);
        }
    }
}
