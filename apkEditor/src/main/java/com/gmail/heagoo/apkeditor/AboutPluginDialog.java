package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import com.gmail.heagoo.apkeditor.base.R;

public class AboutPluginDialog extends Dialog implements View.OnClickListener {
    public AboutPluginDialog(Activity activity) {
        super(activity);
        requestWindowFeature(1);
        init(activity);
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        getWindow().setLayout((width * 7) / 8, -2);
    }

    private void init(Context context) {
        GlobalConfig.instance(context).isDarkTheme();
        int resId = R.layout.dlg_about_translate_plugin;
        View view = LayoutInflater.from(context).inflate(resId, (ViewGroup) null);
        Button btn = (Button) view.findViewById(R.id.btn_close);
        btn.setOnClickListener(this);
        WebView webView = (WebView) view.findViewById(R.id.web_instructions);
        ThemedDocument.load(webView, R.raw.about_translate_plugin);
        setContentView(view);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            dismiss();
        }
    }
}
