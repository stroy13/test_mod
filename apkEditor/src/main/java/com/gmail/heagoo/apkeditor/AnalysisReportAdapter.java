package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ThemeColors;
import java.util.List;

final class AnalysisReportAdapter extends BaseAdapter {
    private final Context context;
    private final AnalysisReportFormatter.Summary summary;
    private final List<AnalysisReportFormatter.Entry> entries;

    AnalysisReportAdapter(Context context, AnalysisReportFormatter.Summary summary,
                          List<AnalysisReportFormatter.Entry> entries) {
        this.context = context;
        this.summary = summary;
        this.entries = entries;
    }

    @Override
    public int getCount() {
        return entries.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        return position == 0 ? summary : entries.get(position - 1);
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
                    ? inflater.inflate(R.layout.item_analysis_summary, parent, false) : convertView;
            TextView headline = view.findViewById(R.id.analysis_headline);
            headline.setText(summary.headline);
            headline.setTextColor(ThemeColors.color(context, summary.positive ? R.attr.labAccent : R.attr.labText));
            ((TextView) view.findViewById(R.id.analysis_summary)).setText(summary.body);
            return view;
        }
        View view = convertView == null
                ? inflater.inflate(R.layout.item_analysis_finding, parent, false) : convertView;
        AnalysisReportFormatter.Entry entry = entries.get(position - 1);
        TextView kind = view.findViewById(R.id.analysis_kind);
        kind.setText(entry.label);
        int color;
        switch (entry.finding.kind) {
            case ERROR:
                color = ThemeColors.color(context, R.attr.labError);
                break;
            case WARNING:
                color = ThemeColors.color(context, R.attr.labAccent);
                break;
            default:
                color = ThemeColors.color(context, R.attr.labMuted);
                break;
        }
        kind.setTextColor(color);
        ((TextView) view.findViewById(R.id.analysis_title)).setText(entry.title);
        ((TextView) view.findViewById(R.id.analysis_location)).setText(entry.location);
        ((TextView) view.findViewById(R.id.analysis_explanation)).setText(entry.explanation);
        ((TextView) view.findViewById(R.id.analysis_action)).setText("Что делать: " + entry.action);
        return view;
    }
}
