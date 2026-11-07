package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ActivityUtil;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ApkComposeFailAdapter extends BaseAdapter {
    private WeakReference<ApkComposeActivity> activityRef;
    private boolean isDark;
    private List<String> lines = new ArrayList();

    /* JADX WARN: Multi-variable type inference failed */
    public ApkComposeFailAdapter(ApkComposeActivity apkComposeActivity, String errMessage) {
        this.activityRef = new WeakReference<>(apkComposeActivity);
        this.isDark = GlobalConfig.instance(apkComposeActivity).isDarkTheme();
        updateMessage(errMessage);
    }

    public void updateMessage(String errMessage) {
        this.lines.clear();
        if (errMessage != null) {
            String[] arr = errMessage.split("\\r?\\n");
            for (String line : arr) {
                if (!"".equals(line)) {
                    this.lines.add(line);
                }
            }
        }
    }

    @Override
    public int getCount() {
        return this.lines.size();
    }

    @Override
    public Object getItem(int position) {
        return this.lines.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            int layoutId = R.layout.item_failed_message;
            convertView = LayoutInflater.from((Context) this.activityRef.get()).inflate(layoutId, (ViewGroup) null);
            holder = new ViewHolder();
            holder.messageTv = (TextView) convertView.findViewById(R.id.message);
            holder.viewBtn = (Button) convertView.findViewById(R.id.btn_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        String strLine = this.lines.get(position);
        boolean viewable = false;
        int commaPos = strLine.indexOf(58);
        if (commaPos != -1) {
            final String filePath = strLine.substring(0, commaPos);
            if (new File(filePath).exists()) {
                viewable = true;
                holder.viewBtn.setVisibility(0);
                int lineNum = -1;
                int nextCommaPos = strLine.indexOf(58, commaPos + 1);
                if (nextCommaPos != -1) {
                    try {
                        lineNum = Integer.valueOf(strLine.substring(commaPos + 1, nextCommaPos)).intValue();
                    } catch (Exception e) {
                    }
                }
                final int lineIndex = lineNum;
                holder.viewBtn.setOnClickListener(new View.OnClickListener() {
                    /* JADX WARN: Type inference failed for: r0v3, types: [android.content.Context, com.gmail.heagoo.apkeditor.ApkComposeActivity] */
                    @Override
                    public void onClick(View v) {
                        ApkComposeActivity r0 = (ApkComposeActivity) ApkComposeFailAdapter.this.activityRef.get();
                        Intent intent = TextEditor.getEditorIntent(r0, filePath, r0.srcApkPath);
                        if (lineIndex > 0) {
                            ActivityUtil.attachParam(intent, "startLine", "" + lineIndex);
                        }
                        r0.startActivity(intent);
                    }
                });
            }
        }
        if (!viewable) {
            holder.viewBtn.setVisibility(4);
        }
        holder.messageTv.setText(this.lines.get(position));
        return convertView;
    }

    private static class ViewHolder {
        TextView messageTv;
        Button viewBtn;

        private ViewHolder() {
        }
    }
}
