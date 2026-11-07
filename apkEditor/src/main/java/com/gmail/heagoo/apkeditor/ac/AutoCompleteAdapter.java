package com.gmail.heagoo.apkeditor.ac;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.neweditor.Document;
import java.util.ArrayList;
import java.util.List;

public class AutoCompleteAdapter extends BaseAdapter implements Filterable {
    private static final int MAX_RECORDS = 32;
    private Context ctx;
    private ItemFilter filter;
    public List<String> filteredData;
    private String[] historyWords;
    private String tag;

    public AutoCompleteAdapter(Context ctx, String tag) {
        this.ctx = ctx;
        this.tag = tag;
    }

    private void init() {
        this.filter = new ItemFilter();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        String history = sp.getString(this.tag, "");
        if (!history.equals("")) {
            this.historyWords = history.split(Document.LINE_SEPARATOR_UNIX);
        } else {
            this.historyWords = new String[0];
        }
        this.filteredData = new ArrayList();
        for (String word : this.historyWords) {
            this.filteredData.add(word);
        }
    }

    @Override
    public int getCount() {
        if (this.filteredData == null) {
            init();
        }
        return this.filteredData.size();
    }

    @Override
    public Object getItem(int position) {
        if (this.filteredData == null) {
            init();
        }
        return this.filteredData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        String word = this.filteredData.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(this.ctx).inflate(R.layout.item_autocomplete, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.filename = (TextView) convertView.findViewById(R.id.filename);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.filename.setText(word);
        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (this.filter == null) {
            init();
        }
        return this.filter;
    }

    public void addInputHistory(String input) {
        if (this.filteredData == null) {
            init();
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        SharedPreferences.Editor editor = sp.edit();
        ArrayList<String> updatedHistory = new ArrayList<>();
        updatedHistory.add(input);
        StringBuilder sb = new StringBuilder();
        sb.append(input);
        for (String word : this.historyWords) {
            if (!word.equals(input)) {
                updatedHistory.add(word);
                sb.append(Document.LINE_SEPARATOR_UNIX);
                sb.append(word);
                if (updatedHistory.size() >= MAX_RECORDS) {
                    break;
                }
            }
        }
        this.historyWords = (String[]) updatedHistory.toArray(new String[updatedHistory.size()]);
        editor.putString(this.tag, sb.toString());
        editor.commit();
    }

    static class ViewHolder {
        public TextView filename;

        ViewHolder() {
        }
    }

    private class ItemFilter extends Filter {
        private ItemFilter() {
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            if (constraint == null) {
                ArrayList<String> nlist = new ArrayList<>(AutoCompleteAdapter.this.historyWords.length);
                for (String word : AutoCompleteAdapter.this.historyWords) {
                    nlist.add(word);
                }
                Filter.FilterResults results = new Filter.FilterResults();
                results.values = nlist;
                results.count = nlist.size();
                return results;
            }
            String filterString = constraint.toString().toLowerCase();
            Filter.FilterResults results2 = new Filter.FilterResults();
            int count = AutoCompleteAdapter.this.historyWords.length;
            ArrayList<String> nlist2 = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String filterableString = AutoCompleteAdapter.this.historyWords[i];
                if (filterableString.toLowerCase().contains(filterString)) {
                    nlist2.add(filterableString);
                }
            }
            results2.values = nlist2;
            results2.count = nlist2.size();
            return results2;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            AutoCompleteAdapter.this.filteredData = (ArrayList) results.values;
            AutoCompleteAdapter.this.notifyDataSetChanged();
        }
    }
}
