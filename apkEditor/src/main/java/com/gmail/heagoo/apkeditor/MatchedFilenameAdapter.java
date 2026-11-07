package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MatchedFilenameAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener {
    private WeakReference<ApkInfoActivity> activityRef;
    private Set<Integer> checkedItems = new HashSet();
    private int layoutId = R.layout.item_file_selectable;
    private WeakReference<ResSelectionChangeListener> listenerRef;
    private ArrayList<String> matchedFiles;
    private String pathPrefix;

    public MatchedFilenameAdapter(ApkInfoActivity activity, ResSelectionChangeListener listener, String pathPrefix, ArrayList<String> matchedFiles) {
        this.activityRef = new WeakReference<>(activity);
        this.listenerRef = new WeakReference<>(listener);
        this.pathPrefix = pathPrefix;
        this.matchedFiles = matchedFiles;
    }

    private static int valuesLowerThan(List<Integer> values, int val) {
        int count = 0;
        for (int i = 0; i < values.size(); i++) {
            int curVal = values.get(i).intValue();
            if (curVal >= val) {
                break;
            }
            count++;
        }
        return count;
    }

    @Override
    public int getCount() {
        return this.matchedFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return this.matchedFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        String filePath = this.matchedFiles.get(position);
        String label = filePath.substring(this.pathPrefix.length() + 1);
        if (convertView == null) {
            convertView = LayoutInflater.from((Context) this.activityRef.get()).inflate(this.layoutId, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.fileIcon = (ImageView) convertView.findViewById(R.id.file_icon);
            viewHolder.pathTv = (TextView) convertView.findViewById(R.id.filename);
            viewHolder.detailTv = (TextView) convertView.findViewById(R.id.detail1);
            viewHolder.checkbox = (CheckBox) convertView.findViewById(R.id.checkBox);
            viewHolder.pathTv.setSingleLine(false);
            viewHolder.pathTv.setMaxLines(2);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.pathTv.setText(label);
        showDetail(viewHolder, filePath);
        viewHolder.checkbox.setId(position);
        if (!this.checkedItems.contains(Integer.valueOf(position))) {
            viewHolder.checkbox.setChecked(false);
        } else {
            viewHolder.checkbox.setChecked(true);
        }
        viewHolder.checkbox.setOnCheckedChangeListener(this);
        return convertView;
    }

    private void showDetail(ViewHolder viewHolder, String filePath) {
        int resId = R.drawable.folderutil_file;
        Bitmap thumbnail = null;
        String detailInfo = null;
        String filename = filePath.substring(filePath.lastIndexOf(47) + 1);
        int dotPos = filename.lastIndexOf(46);
        if (dotPos != -1) {
            String fileType = filename.substring(dotPos + 1);
            if ("xml".equals(fileType)) {
                resId = R.drawable.util_xml_file;
            } else if ("png".equals(fileType) || "jpg".equals(fileType)) {
                ResListAdapter resAdapter = this.activityRef.get().getResListAdapter();
                ImageThumbnailInfo info = resAdapter.getImageInfo(filePath);
                thumbnail = info.thumbnail;
                detailInfo = info.detailInfo;
            }
        }
        if (thumbnail != null) {
            viewHolder.fileIcon.setImageBitmap(thumbnail);
        } else {
            viewHolder.fileIcon.setImageResource(resId);
        }
        if (detailInfo != null) {
            viewHolder.detailTv.setVisibility(0);
            viewHolder.detailTv.setText(detailInfo);
        } else {
            viewHolder.detailTv.setVisibility(8);
        }
    }

    public List<Integer> getSeletedItems() {
        List<Integer> selected = new ArrayList<>();
        selected.addAll(this.checkedItems);
        Collections.sort(selected);
        return selected;
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        int id = view.getId();
        if (isChecked) {
            this.checkedItems.add(Integer.valueOf(id));
        } else {
            this.checkedItems.remove(Integer.valueOf(id));
        }
        if (this.listenerRef != null) {
            this.listenerRef.get().selectionChanged(this.checkedItems);
        }
    }

    public void selectAll() {
        for (int i = 0; i < this.matchedFiles.size(); i++) {
            this.checkedItems.add(Integer.valueOf(i));
        }
        notifyDataSetChanged();
    }

    public void selectNone() {
        this.checkedItems.clear();
        notifyDataSetChanged();
    }

    public void resetFileList(ArrayList<String> matchedFiles, List<Integer> removedIndexes) {
        Collections.sort(removedIndexes);
        Set<Integer> remainChecked = new HashSet<>();
        Iterator<Integer> it = this.checkedItems.iterator();
        while (it.hasNext()) {
            int item = it.next().intValue();
            if (!removedIndexes.contains(Integer.valueOf(item))) {
                int count = valuesLowerThan(removedIndexes, item);
                remainChecked.add(Integer.valueOf(item - count));
            }
        }
        this.checkedItems = remainChecked;
        this.matchedFiles = matchedFiles;
        notifyDataSetChanged();
    }

    public boolean isAllSelected() {
        return this.checkedItems.size() == this.matchedFiles.size();
    }

    public boolean isNonSelected() {
        return this.checkedItems.isEmpty();
    }

    private static class ViewHolder {
        public CheckBox checkbox;
        public TextView detailTv;
        public ImageView fileIcon;
        public TextView pathTv;

        private ViewHolder() {
        }
    }
}
