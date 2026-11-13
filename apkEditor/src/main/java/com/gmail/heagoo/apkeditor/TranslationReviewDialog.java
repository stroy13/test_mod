package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.translate.TranslationReviewEdits;
import common.types.StringItem;
import java.util.List;
import java.util.Map;

/** Virtualized review of every result; changes are staged until the user saves. */
final class TranslationReviewDialog {
    private TranslationReviewDialog() {}

    static void show(Context context, List<StringItem> source, List<StringItem> result,
            Map<String, String> translated, List<String> rejected, boolean sample, boolean googleEngine,
            Runnable copyLog, java.util.function.Consumer<Runnable> save) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 16);
        content.setPadding(pad, dp(context, 8), pad, 0);
        TextView header = new TextView(context);
        header.setText(context.getString(sample ? R.string.ai_sample_note : R.string.ai_review_hint));
        header.setPadding(0, 0, 0, dp(context, 8));
        content.addView(header);
        if (googleEngine) {
            android.widget.ImageView attribution = new android.widget.ImageView(context);
            attribution.setImageResource(R.drawable.google_translate_attribution);
            attribution.setContentDescription(context.getString(R.string.ai_google_attribution));
            attribution.setScaleType(android.widget.ImageView.ScaleType.FIT_START);
            content.addView(attribution, new LinearLayout.LayoutParams(dp(context, 150), dp(context, 32)));
        }
        ListView list = new ListView(context);
        list.setDividerHeight(dp(context, 1));
        int height = Math.min(dp(context, 520), (int) (context.getResources().getDisplayMetrics().heightPixels * 0.55f));
        content.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return result.size(); }
            @Override public Object getItem(int position) { return result.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                LinearLayout row;
                if (recycled instanceof LinearLayout) row = (LinearLayout) recycled;
                else {
                    row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(dp(context, 8), dp(context, 10), dp(context, 8), dp(context, 10));
                    TypedValue selectable = new TypedValue();
                    if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectable, true))
                        row.setBackgroundResource(selectable.resourceId);
                    for (int i = 0; i < 4; i++) {
                        TextView line = new TextView(context);
                        line.setTextSize(i == 0 ? 14 : 16);
                        if (i == 0) line.setTypeface(null, Typeface.BOLD);
                        line.setMaxLines(i == 0 ? 2 : 3);
                        line.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        row.addView(line);
                    }
                }
                StringItem original = source.get(position), output = result.get(position);
                ((TextView) row.getChildAt(0)).setText(original.name);
                ((TextView) row.getChildAt(1)).setText(context.getString(R.string.ai_review_source, original.value));
                ((TextView) row.getChildAt(2)).setText(context.getString(R.string.ai_review_result, output.value));
                int status = rejected.contains(original.name) ? R.string.ai_review_rejected
                        : translated.containsKey(original.name) ? R.string.ai_review_translated : R.string.ai_review_unchanged;
                ((TextView) row.getChildAt(3)).setText(status);
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            StringItem original = source.get(position);
            if (!TranslationReviewEdits.editable(original)) {
                Toast.makeText(context, R.string.ai_review_uneditable, Toast.LENGTH_LONG).show();
                return;
            }
            LinearLayout editor = new LinearLayout(context);
            editor.setOrientation(LinearLayout.VERTICAL);
            editor.setPadding(pad, 0, pad, 0);
            TextView originalText = new TextView(context);
            originalText.setText(context.getString(R.string.ai_review_source, original.value));
            editor.addView(originalText);
            EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            input.setMinLines(2);
            input.setText(result.get(position).value);
            editor.addView(input);
            AlertDialog edit = new AlertDialog.Builder(context).setTitle(original.name).setView(editor)
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, null).create();
            edit.setOnShowListener(ignored -> edit.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = input.getText().toString();
                if (!TranslationReviewEdits.apply(position, source, result, translated, rejected, value)) {
                    input.setError(context.getString(R.string.ai_review_invalid));
                    return;
                }
                adapter.notifyDataSetChanged();
                edit.dismiss();
            }));
            edit.show();
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(sample ? R.string.ai_sample_title : R.string.ai_preview).setView(content)
                .setNeutralButton(R.string.ai_copy_log, null);
        if (sample) builder.setPositiveButton(android.R.string.ok, null);
        else builder.setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.ai_save, null);
        AlertDialog review = builder.create();
        review.setOnShowListener(ignored -> {
            review.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> copyLog.run());
            if (!sample) review.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> save.accept(review::dismiss));
        });
        review.show();
    }

    private static int dp(Context context, int value) {
        return (int) (context.getResources().getDisplayMetrics().density * value + 0.5f);
    }
}
