package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import com.gmail.heagoo.common.Display;
import java.util.List;

class ModificationAdapter extends BaseExpandableListAdapter {
    public Context ctx;
    public List<ModificationCategory> modificationList;

    public ModificationAdapter(Context ctx, List<ModificationCategory> modList) {
        this.ctx = ctx;
        this.modificationList = modList;
    }

    @Override
    public int getGroupCount() {
        return this.modificationList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.modificationList.get(groupPosition).getItems();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.modificationList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.modificationList.get(groupPosition).getItemDescription(this.ctx, childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return (groupPosition << 16) | childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String text = this.modificationList.get(groupPosition).name;
        if (convertView == null) {
            convertView = new TextView(this.ctx);
        }
        int left = Display.dip2px(this.ctx, 32.0f);
        ((TextView) convertView).setPadding(left, left / 8, 0, 0);
        ((TextView) convertView).setTypeface(null, 1);
        ((TextView) convertView).setText(text);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String text = this.modificationList.get(groupPosition).getItemDescription(this.ctx, childPosition);
        if (convertView == null) {
            convertView = new TextView(this.ctx);
        }
        int left = Display.dip2px(this.ctx, 48.0f);
        ((TextView) convertView).setPadding(left, 0, 0, 0);
        ((TextView) convertView).setText(text);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
