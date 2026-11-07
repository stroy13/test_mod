package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ApkInfoParser;
import java.util.ArrayList;
import java.util.List;

public class ApkListAdapter extends BaseAdapter {
    private Context ctx;
    private int themeId;
    List<String> fileList = new ArrayList();
    private LruCache<String, ApkInfoParser.AppInfo> apkInfoCache = new LruCache<String, ApkInfoParser.AppInfo>(64) {
    };

    public ApkListAdapter(Context ctx) {
        this.ctx = ctx;
        this.themeId = GlobalConfig.instance(ctx).getThemeId();
    }

    @Override
    public int getCount() {
        return this.fileList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        String apkFilePath = this.fileList.get(position);
        if (convertView == null) {
            int resId = R.layout.item_file;
            convertView = LayoutInflater.from(this.ctx).inflate(resId, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.file_icon);
            viewHolder.filename = (TextView) convertView.findViewById(R.id.filename);
            viewHolder.desc1 = (TextView) convertView.findViewById(R.id.detail1);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        ApkInfoParser.AppInfo apkInfo = this.apkInfoCache.get(apkFilePath);
        if (apkInfo == null) {
            try {
                apkInfo = new ApkInfoParser().parse(this.ctx, apkFilePath);
            } catch (Throwable th) {
            }
            if (apkInfo == null) {
                apkInfo = new ApkInfoParser.AppInfo();
                apkInfo.icon = this.ctx.getResources().getDrawable(R.drawable.apk_icon);
            }
        }
        this.apkInfoCache.put(apkFilePath, apkInfo);
        viewHolder.icon.setImageDrawable(apkInfo.icon);
        if (apkInfo.label != null) {
            viewHolder.filename.setText(apkInfo.label);
            viewHolder.desc1.setText(apkFilePath);
            viewHolder.desc1.setVisibility(0);
        } else {
            viewHolder.filename.setText(apkFilePath);
            viewHolder.desc1.setVisibility(8);
        }
        return convertView;
    }

    public void addApkFile(String apkPath) {
        this.fileList.add(apkPath);
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView desc1;
        TextView filename;
        ImageView icon;

        private ViewHolder() {
        }
    }
}
