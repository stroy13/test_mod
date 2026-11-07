package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AppListAdapter extends BaseAdapter {
    private Context ctx;
    private APPLIST_ORDER order;
    private PackageManager pm;
    private int themeId;
    public List<AppInfo> appList = new ArrayList();
    private LruCache<String, Drawable> pkgDrawables = new LruCache<String, Drawable>(32) {
        @Override
        public void entryRemoved(boolean evicted, String key, Drawable oldValue, Drawable newValue) {
            oldValue.setCallback(null);
        }
    };

    private enum APPLIST_ORDER {
        BY_NAME,
        BY_INSTALL_TIME
    }

    public AppListAdapter(Context ctx) {
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
        this.themeId = GlobalConfig.instance(ctx).getThemeId();
    }

    public void setAppList(List<AppInfo> appList, String order) {
        String[] orderConsts = this.ctx.getResources().getStringArray(R.array.order_value);
        if (order.equals(orderConsts[0])) {
            this.order = APPLIST_ORDER.BY_NAME;
        } else if (order.equals(orderConsts[1])) {
            this.order = APPLIST_ORDER.BY_INSTALL_TIME;
        } else {
            this.order = APPLIST_ORDER.BY_NAME;
        }
        sortAppList(appList);
        synchronized (this.appList) {
            this.appList.clear();
            this.appList.addAll(appList);
        }
    }

    private void sortAppList(List<AppInfo> appList) {
        final Locale locale = Locale.getDefault();
        Comparator<AppInfo> comparator = null;
        switch (this.order) {
            case BY_NAME:
                comparator = new Comparator<AppInfo>() {
                    @Override
                    public int compare(AppInfo arg0, AppInfo arg1) {
                        return arg0.appName.toLowerCase(locale).compareTo(arg1.appName.toLowerCase(locale));
                    }
                };
                break;
            case BY_INSTALL_TIME:
                comparator = new Comparator<AppInfo>() {
                    @Override
                    public int compare(AppInfo arg0, AppInfo arg1) {
                        return arg0.lastUpdateTime < arg1.lastUpdateTime ? 1 : -1;
                    }
                };
                break;
        }
        Collections.sort(appList, comparator);
    }

    public List<AppInfo> getAppList() {
        List<AppInfo> retList;
        synchronized (this.appList) {
            retList = new ArrayList<>();
            retList.addAll(this.appList);
        }
        return retList;
    }

    @Override
    public int getCount() {
        int size;
        synchronized (this.appList) {
            size = this.appList.size();
        }
        return size;
    }

    @Override
    public Object getItem(int arg0) {
        AppInfo appInfo;
        synchronized (this.appList) {
            appInfo = this.appList.get(arg0);
        }
        return appInfo;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        AppInfo appInfo = (AppInfo) getItem(position);
        if (appInfo == null) {
            return null;
        }
        if (convertView == null) {
            int resId = R.layout.item_applist;
            convertView = LayoutInflater.from(this.ctx).inflate(resId, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.app_icon);
            viewHolder.appName = (TextView) convertView.findViewById(R.id.app_name);
            viewHolder.desc1 = (TextView) convertView.findViewById(R.id.app_desc1);
            viewHolder.desc2 = (TextView) convertView.findViewById(R.id.app_desc2);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        try {
            viewHolder.appName.setText(appInfo.appName);
            viewHolder.desc1.setText(appInfo.packagePath);
            Drawable icon = this.pkgDrawables.get(appInfo.packagePath);
            if (icon == null) {
                icon = appInfo.applicationInfo.loadIcon(this.pm);
                this.pkgDrawables.put(appInfo.packagePath, icon);
            }
            viewHolder.icon.setImageDrawable(icon);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return convertView;
    }

    static class ViewHolder {
        public TextView appName;
        public TextView desc1;
        public TextView desc2;
        public ImageView icon;

        ViewHolder() {
        }
    }
}
