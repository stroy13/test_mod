package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import common.types.StringItem;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StringListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
    private WeakReference<Activity> activityRef;
    private final List<StringItem> valueList = new ArrayList();
    int editingIndex = -1;
    private Map<String, Map<String, String>> changedValues = new HashMap();
    private boolean editingEnabled = true;
    private String curConfig = null;
    private int layoutId = R.layout.item_stringvaluestatic;

    void setEditingEnabled(boolean enabled) {
        this.editingEnabled = enabled;
    }

    StringListAdapter(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public int getCount() {
        int size;
        synchronized (this.valueList) {
            size = this.valueList.size();
        }
        return size;
    }

    @Override
    public Object getItem(int position) {
        StringItem stringItem;
        synchronized (this.valueList) {
            stringItem = this.valueList.get(position);
        }
        return stringItem;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        synchronized (this.valueList) {
            StringItem value = this.valueList.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(this.activityRef.get()).inflate(this.layoutId, (ViewGroup) null);
                viewHolder = new ViewHolder();
                viewHolder.name = (TextView) convertView.findViewById(R.id.string_name);
                viewHolder.value = (TextView) convertView.findViewById(R.id.string_value);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.name.setText(value.name);
            viewHolder.value.setText(value.value);
        }
        return convertView;
    }

    void updateData(String curConfig, List<StringItem> list) {
        synchronized (this.valueList) {
            this.curConfig = curConfig;
            this.valueList.clear();
            for (StringItem item : list) {
                this.valueList.add(item);
            }
        }
        notifyDataSetChanged();
    }

    void checkTextChange(int position, String newValue) {
        boolean valueChanged = false;
        synchronized (this.valueList) {
            if (position >= 0) {
                if (position < this.valueList.size()) {
                    StringItem item = this.valueList.get(position);
                    if (!item.value.equals(newValue)) {
                        if (this.curConfig != null) {
                            item.value = newValue;
                            Map<String, String> valueMap = this.changedValues.get(this.curConfig);
                            if (valueMap == null) {
                                valueMap = new HashMap();
                                this.changedValues.put(this.curConfig, valueMap);
                            }
                            valueMap.put(item.name, newValue);
                        }
                        valueChanged = true;
                    }
                }
            }
        }
        if (valueChanged) {
            notifyDataSetChanged();
        }
    }

    Map<String, Map<String, String>> getChangedValues() {
        return this.changedValues;
    }

    void setChangedValues(Map<String, Map<String, String>> changedStringValues) {
        this.changedValues = changedStringValues;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
        if (!this.editingEnabled) {
            Activity activity = this.activityRef.get();
            if (activity != null) {
                Toast.makeText(activity, R.string.wait_for_decoding, 0).show();
                return;
            }
            return;
        }
        StringValueDialog dlg = new StringValueDialog(this.activityRef.get(), this, position);
        synchronized (this.valueList) {
            StringItem item = this.valueList.get(position);
            dlg.setKeyValue(item.name, item.value);
        }
        dlg.show();
    }

    private static class ViewHolder {
        public TextView name;
        public TextView value;

        private ViewHolder() {
        }
    }
}
