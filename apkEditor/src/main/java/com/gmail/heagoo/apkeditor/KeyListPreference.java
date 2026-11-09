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
    /** Persisted value of the custom-key entry; never assume its row index. */
    static final String CUSTOM_VALUE = "customkey";
    private static final String EDITOR_VALUE = "editorkey";

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

    private static int descriptionFor(String value) {
        if (CUSTOM_VALUE.equals(value)) {
            return R.string.lab_key_custom_desc;
        }
        if (EDITOR_VALUE.equals(value)) {
            return R.string.lab_key_editor_desc;
        }
        return R.string.lab_key_test_desc;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        Context c = getContext();
        LinearLayout linearLayout = new LinearLayout(c);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(20.0f), dp(20.0f), dp(20.0f), dp(12.0f));
        linearLayout.setBackgroundColor(color(R.attr.labSurface));
        TextView title = new TextView(c);
        title.setText(getDialogTitle());
        title.setTextColor(color(R.attr.labText));
        title.setTextSize(19.0f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
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
        if (entries == null || values == null) {
            builder.setTitle((CharSequence) null).setView(linearLayout)
                    .setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return;
        }
        int count = Math.min(entries.length, values.length);
        for (int i = 0; i < count; i++) {
            final String value = values[i].toString();
            boolean active = value.equals(current);
            LinearLayout row = new LinearLayout(c);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12.0f), dp(8.0f), dp(10.0f), dp(8.0f));
            row.setBackground(background(active));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8.0f);
            linearLayout.addView(row, lp);
            LinearLayout text = new LinearLayout(c);
            text.setOrientation(LinearLayout.VERTICAL);
            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1.0f));
            TextView label = new TextView(c);
            label.setText(entries[i]);
            label.setTextSize(14.0f);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            label.setTextColor(color(R.attr.labText));
            text.addView(label);
            TextView desc = new TextView(c);
            desc.setText(descriptionFor(value));
            desc.setTextSize(11.0f);
            desc.setTextColor(color(R.attr.labMuted));
            desc.setPadding(0, dp(4.0f), 0, 0);
            text.addView(desc);
            RadioButton radio = new RadioButton(c);
            radio.setButtonTintList(ColorStateList.valueOf(color(active ? R.attr.labAccent : R.attr.labMuted)));
            radio.setChecked(active);
            radio.setClickable(false);
            radio.setFocusable(false);
            row.addView(radio);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogInterface dialog = KeyListPreference.this.getDialog();
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    if (CUSTOM_VALUE.equals(value)) {
                        new KeySelectDlgHelper(KeyListPreference.this).showDialog(KeyListPreference.this.getContext());
                        return;
                    }
                    if (EDITOR_VALUE.equals(value)) {
                        new AlertDialog.Builder(KeyListPreference.this.getContext())
                                .setTitle(R.string.lab_key_editor_name)
                                .setMessage(R.string.lab_key_editor_desc)
                                .setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d, int w) {
                                        select(value);
                                    }
                                }).show();
                        return;
                    }
                    select(value);
                }
            });
        }
        ScrollView scrollView = new ScrollView(c);
        scrollView.addView(linearLayout);
        builder.setTitle((CharSequence) null).setView(scrollView)
                .setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null)
                .setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
    }

    private void select(String value) {
        if (callChangeListener(value)) {
            setValue(value);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
    }

    public void setCustomValue() {
        select(CUSTOM_VALUE);
    }
}
