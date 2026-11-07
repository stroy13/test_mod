package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.PreferenceUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/* loaded from: /tmp/apkx/classes5.dex */
public class SmaliNoticeDialog extends Dialog implements View.OnClickListener {
    private Context context;
    private View view;

    public SmaliNoticeDialog(Context context) {
        super(context);
        requestWindowFeature(1);
        this.context = context;
        int layoutId = R.layout.dlg_smali_license;
        this.view = LayoutInflater.from(context).inflate(layoutId, (ViewGroup) null);
        setContentView(this.view);
        initView();
    }

    /* JADX WARN: Code restructure failed: missing block: B:14:0x0050, code lost:
    
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x004e, code lost:
    
        if (r0 == null) goto L16;
     */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x0047, code lost:
    
        if (r0 != null) goto L26;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void initView() {
        InputStream in = null;
        StringBuilder sb = new StringBuilder();
        try {
            try {
                in = this.context.getAssets().open(this.context.getString(R.string.lab_smali_notice_asset));
                InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(reader);
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    sb.append(line + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            TextView tv = (TextView) this.view.findViewById(R.id.content);
            tv.setText(this.context.getString(R.string.smali_notice_intro) + "\n\n" + sb.toString());
            Button originalBtn = (Button) this.view.findViewById(R.id.original_button);
            originalBtn.setOnClickListener(new View.OnClickListener() { // from class: com.gmail.heagoo.apkeditor.SmaliNoticeDialog$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    SmaliNoticeDialog.this.m295lambda$initView$0$comgmailheagooapkeditorSmaliNoticeDialog(view);
                }
            });
            Button closeBtn = (Button) this.view.findViewById(R.id.close_button);
            closeBtn.setOnClickListener(this);
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e2) {
                }
            }
            throw th;
        }
    }

    /* renamed from: lambda$initView$0$com-gmail-heagoo-apkeditor-SmaliNoticeDialog, reason: not valid java name */
    /* synthetic */ void m295lambda$initView$0$comgmailheagooapkeditorSmaliNoticeDialog(View v) {
        try {
            InputStream source = this.context.getAssets().open("smali-NOTICE");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));
                try {
                    StringBuilder original = new StringBuilder();
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        } else {
                            original.append(line).append('\n');
                        }
                    }
                    new AlertDialog.Builder(this.context).setTitle(R.string.lab_license_original).setMessage(original.toString()).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
                    reader.close();
                    if (source != null) {
                        source.close();
                    }
                } finally {
                }
            } finally {
            }
        } catch (IOException error) {
            Toast.makeText(this.context, error.toString(), 1).show();
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View v) {
        CheckBox cb = (CheckBox) this.view.findViewById(R.id.cb_show_once);
        if (cb.isChecked()) {
            PreferenceUtil.setBoolean(this.context, "smali_license_showed", true);
        }
        dismiss();
    }
}
