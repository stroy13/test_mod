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

public class MenuListAdapter extends BaseAdapter {
    public static final int ITEM_ABOUT = 3;
    public static final int ITEM_DONATE = 1;
    public static final int ITEM_PROJECT = 0;
    public static final int ITEM_SETTING = 2;
    public static final int ITEM_TOOLS = 4;
    private WeakReference<Context> ctxRef;
    private boolean isDark;
    // "About/Info" lives on the home screen; do not duplicate it in the drawer.
    private static final int[] titles = {R.string.projects, R.string.apk_inspection_tools};
    private static final int[] drawables = {R.drawable.ic_project, R.drawable.ic_setting};
    private static final int[] drawables_dark = {R.drawable.ic_project_white, R.drawable.ic_setting_white};
    private static final int[] itemIds = {0, 4};

    public MenuListAdapter(Context ctx, boolean isDark) {
        this.ctxRef = new WeakReference<>(ctx);
        this.isDark = isDark;
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public Object getItem(int i) {
        return Integer.valueOf(titles[i]);
    }

    @Override
    public long getItemId(int i) {
        return itemIds[i];
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (convertView == null) {
            int resId = R.layout.item_main_menu;
            convertView = LayoutInflater.from(this.ctxRef.get()).inflate(resId, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.iconIv = (ImageView) convertView.findViewById(R.id.menu_icon);
            viewHolder.titleTv = (TextView) convertView.findViewById(R.id.menu_title);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.iconIv.setImageResource(this.isDark ? drawables_dark[i] : drawables[i]);
        viewHolder.titleTv.setText(titles[i]);
        return convertView;
    }

    private class ViewHolder {
        public ImageView iconIv;
        public TextView titleTv;

        private ViewHolder() {
        }
    }
}
