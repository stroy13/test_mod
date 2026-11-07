package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ResNavigationMgr implements View.OnClickListener {
    private WeakReference<ApkInfoActivity> activityRef;
    private List<View> addedViewList = new ArrayList();
    private String currDir;
    private String rootDir;
    private HorizontalScrollView scrollView;
    private LinearLayout viewContainer;

    public ResNavigationMgr(ApkInfoActivity ctx, String rootDir, LinearLayout container, HorizontalScrollView scrollView) {
        this.activityRef = new WeakReference<>(ctx);
        this.rootDir = rootDir;
        this.currDir = rootDir;
        this.viewContainer = container;
        this.scrollView = scrollView;
    }

    public void gotoDirectory(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!path.equals(this.currDir)) {
            if (!path.startsWith(this.currDir + "/")) {
                if (this.currDir.startsWith(path + "/")) {
                    String unusedPath = this.currDir.substring(path.length() + 1);
                    String[] dirs = unusedPath.split("/");
                    for (int i = dirs.length - 1; i >= 0; i--) {
                        int position = this.addedViewList.size() - 1;
                        View view = this.addedViewList.get(position);
                        this.addedViewList.remove(position);
                        this.viewContainer.removeView(view);
                    }
                    this.currDir = path;
                }
            } else {
                String subPath = path.substring(this.currDir.length() + 1);
                String[] subDirs = subPath.split("/");
                for (String dir : subDirs) {
                    String nextDir = this.currDir + "/" + dir;
                    View tabView = createTab(nextDir);
                    this.viewContainer.addView(tabView);
                    this.addedViewList.add(tabView);
                    this.currDir = nextDir;
                }
            }
            this.scrollView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ResNavigationMgr.this.scrollView.fullScroll(66);
                }
            }, 100L);
        }
    }

    private View createTab(String path) {
        int resId = R.layout.item_navigation_dir;
        View view = LayoutInflater.from((Context) this.activityRef.get()).inflate(resId, (ViewGroup) null);
        View tab = view.findViewById(R.id.menu_dirtab);
        tab.setTag(path);
        tab.setOnClickListener(this);
        TextView titleTv = (TextView) view.findViewById(R.id.dirname);
        titleTv.setText(getNameByPath(path));
        return view;
    }

    private String getNameByPath(String path) {
        int pos = path.lastIndexOf(47);
        return path.substring(pos + 1);
    }

    @Override
    public void onClick(View v) {
        String path = (String) v.getTag();
        if (path == null) {
            return;
        }
        String oldPath = this.currDir;
        gotoDirectory(path);
        if (this.activityRef.get() != null) {
            int upLevel = getUpLevel(oldPath, path);
            this.activityRef.get().resDirectoryChanged(path, upLevel);
        }
    }

    private int getUpLevel(String oldPath, String newPath) {
        if (oldPath.startsWith(newPath + "/")) {
            String unusedPath = oldPath.substring(newPath.length() + 1);
            String[] dirs = unusedPath.split("/");
            return dirs.length;
        }
        return 0;
    }

    public void gotoHome() {
        for (int i = this.addedViewList.size(); i > 0; i--) {
            int position = i - 1;
            View view = this.addedViewList.get(position);
            this.addedViewList.remove(position);
            this.viewContainer.removeView(view);
        }
        this.currDir = this.rootDir;
    }
}
