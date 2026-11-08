package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ThemeColors;
import common.types.StringItem;

final class TranslationComparisonAdapter extends BaseAdapter {
    private final Context context;
    private String summary;
    private TranslationComparison.Result result;

    TranslationComparisonAdapter(Context context, String summary, TranslationComparison.Result result) {
        this.context = context;
        this.summary = summary;
        this.result = result;
    }

    void update(String summary, TranslationComparison.Result result) {
        this.summary = summary;
        this.result = result;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return result.rows.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        return position == 0 ? summary : result.rows.get(position - 1);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return position > 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (position == 0) {
            View view = convertView == null
                    ? inflater.inflate(R.layout.item_translation_summary, parent, false) : convertView;
            ((TextView) view.findViewById(R.id.translation_summary)).setText(summary);
            return view;
        }
        View view = convertView == null
                ? inflater.inflate(R.layout.item_translation_comparison, parent, false) : convertView;
        TranslationComparison.Row row = result.rows.get(position - 1);
        TextView status = view.findViewById(R.id.translation_status);
        TextView name = view.findViewById(R.id.translation_name);
        TextView source = view.findViewById(R.id.translation_source);
        TextView target = view.findViewById(R.id.translation_target);
        TextView placeholders = view.findViewById(R.id.translation_placeholders);

        int accent = ThemeColors.color(context, R.attr.labAccent);
        int error = ThemeColors.color(context, R.attr.labError);
        switch (row.status) {
            case MISSING:
                status.setText("НЕТ ПЕРЕВОДА");
                status.setTextColor(error);
                break;
            case PLACEHOLDERS:
                status.setText("ОТЛИЧАЮТСЯ ПЛЕЙСХОЛДЕРЫ");
                status.setTextColor(error);
                break;
            case EXTRA:
                status.setText("ТОЛЬКО В ПЕРЕВОДЕ");
                status.setTextColor(accent);
                break;
            default:
                status.setText("ПЕРЕВЕДЕНО");
                status.setTextColor(accent);
                break;
        }
        name.setText(row.name);
        source.setText("Оригинал: " + value(row.source));
        target.setText(row.status == TranslationComparison.Status.MISSING
                ? "Перевод: отсутствует" : "Перевод: " + value(row.target));
        if (row.status == TranslationComparison.Status.PLACEHOLDERS) {
            placeholders.setVisibility(View.VISIBLE);
            placeholders.setText("Плейсхолдеры оригинала: " + row.sourceTokens
                    + "\nПлейсхолдеры перевода: " + row.targetTokens);
        } else if (row.status == TranslationComparison.Status.MISSING && !"нет".equals(row.sourceTokens)) {
            placeholders.setVisibility(View.VISIBLE);
            placeholders.setText("Нужны плейсхолдеры: " + row.sourceTokens);
        } else {
            placeholders.setVisibility(View.GONE);
        }
        return view;
    }

    private static String value(StringItem item) {
        if (item == null) {
            return "нет строки";
        }
        return item.value == null ? "" : item.value;
    }
}
