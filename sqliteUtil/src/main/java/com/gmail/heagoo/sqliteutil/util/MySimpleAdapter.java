package com.gmail.heagoo.sqliteutil.util;

import android.R;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import java.util.List;
import java.util.Map;

public class MySimpleAdapter extends SimpleAdapter {
    private int color1;
    private int color2;

    public MySimpleAdapter(Context context, List<Map<String, String>> items, int resource, String[] from, int[] to, boolean isDark) {
        super(context, items, resource, from, to);
        if (isDark) {
            this.color1 = -3355444;
            this.color2 = -5592406;
        } else {
            this.color1 = -13421773;
            this.color2 = -11184811;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView tv1 = (TextView) view.findViewById(R.id.text1);
        tv1.setTextColor(this.color1);
        TextView tv2 = (TextView) view.findViewById(R.id.text2);
        tv2.setTextColor(this.color2);
        return view;
    }
}
