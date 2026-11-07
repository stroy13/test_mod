package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class GroupAdapter extends BaseAdapter {
    private List<Integer> checkedIndice = new ArrayList();
    private WeakReference<Context> contextRef;
    private List<String> list;

    GroupAdapter(Context context, List<String> list) {
        this.contextRef = new WeakReference<>(context);
        this.list = list;
    }

    @Override
    public int getCount() {
        return this.list.size();
    }

    @Override
    public Object getItem(int position) {
        return this.list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(this.contextRef.get()).inflate(R.layout.popup_item_big, (ViewGroup) null);
            holder = new ViewHolder();
            convertView.setTag(holder);
            holder.groupItem = (TextView) convertView.findViewById(R.id.groupItem);
            holder.checkImage = (ImageView) convertView.findViewById(R.id.checkImage);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.groupItem.setText(this.list.get(position));
        boolean checked = this.checkedIndice.contains(Integer.valueOf(position));
        holder.checkImage.setVisibility(checked ? 0 : 4);
        return convertView;
    }

    public void setCheckIndice(List<Integer> checkedIndice) {
        this.checkedIndice = checkedIndice;
    }

    static class ViewHolder {
        ImageView checkImage;
        TextView groupItem;

        ViewHolder() {
        }
    }
}
