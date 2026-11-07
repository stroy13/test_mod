package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.RefInvoke;
import java.lang.ref.WeakReference;

public class AboutDialog extends Dialog implements View.OnClickListener {
    private final WeakReference<Activity> activityRef;

    public AboutDialog(final Activity activity) {
        super(activity, R.style.Dialog_No_Border_2);
        this.activityRef = new WeakReference<>(activity);
        requestWindowFeature(1);
        LayoutInflater inflater = LayoutInflater.from(activity);
        View layout = inflater.inflate(R.layout.dlg_about, (ViewGroup) null);
        super.setContentView(layout);
        Drawable icon = activity.getApplicationInfo().loadIcon(activity.getPackageManager());
        try {
            Object selected = RefInvoke.invokeStaticMethod("com.gmail.heagoo.seticon.SetIcon", "getSelectedIcon", new Class[]{Activity.class}, new Object[]{activity});
            if (selected instanceof Integer) {
                icon = activity.getDrawable(((Integer) selected).intValue());
            }
        } catch (RuntimeException e) {
        }
        ImageView iv = (ImageView) layout.findViewById(R.id.icon);
        iv.setImageDrawable(icon);
        Button getBtn = (Button) layout.findViewById(R.id.btn_get_pro);
        getBtn.setOnClickListener(this);
        Button tg = (Button) layout.findViewById(R.id.telegram);
        tg.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                AboutDialog.lambda$new$0(activity, view);
            }
        });
        layout.findViewById(R.id.btn_close).setOnClickListener(this);
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            TextView tv = (TextView) layout.findViewById(R.id.tv_version);
            tv.setText("Version " + pInfo.versionName);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        TextView depVer = (TextView) layout.findViewById(R.id.tv_dep_version);
        boolean upgraded = MainActivity.upgradedFromOldVersion(activity);
        boolean fromPlay = false;
        String pkgName = activity.getPackageName();
        String installer = activity.getPackageManager().getInstallerPackageName(pkgName);
        if (installer != null && installer.endsWith(".vending")) {
            fromPlay = true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("smali: v3.0.10\nandroid.jar: Android API 37");
        if (!upgraded && fromPlay) {
            sb.append("\n\nIf you cannot use, please send email to timscriptov@gmail.com with a title of \"VERSION 1.9 ISSUES\"");
        }
        depVer.setText(sb.toString());
        getBtn.setVisibility(8);
        layout.findViewById(R.id.textView2).setVisibility(8);
        layout.findViewById(R.id.textView3).setVisibility(8);
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        getWindow().setLayout((width * 7) / 8, -2);
    }

    static void lambda$new$0(Activity activity, View v) {
        Intent i = new Intent("android.intent.action.VIEW");
        i.setData(Uri.parse("https://t.me/Trykath"));
        try {
            activity.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.about_link_unavailable, 1).show();
        }
    }

    protected static void viewProVersion(Context ctx) {
        String pkgName = ctx.getPackageName() + ".pro";
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent goToMarket = new Intent("android.intent.action.VIEW", uri);
        try {
            ctx.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            ctx.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://play.google.com/store/apps/details?id=" + pkgName)));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_get_pro) {
            viewProVersion(this.activityRef.get());
        } else if (id == R.id.btn_close) {
            dismiss();
        }
    }
}
