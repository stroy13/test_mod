package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.EditorTheme;
import java.lang.ref.WeakReference;

public class AppAgreementDialog extends Dialog implements View.OnClickListener {
    private final WeakReference<MainActivity> activityRef;
    private EditText inputEt;

    /* JADX WARN: Multi-variable type inference failed */
    public AppAgreementDialog(MainActivity mainActivity) {
        super(mainActivity, EditorTheme.dialogStyle(mainActivity));
        requestWindowFeature(1);
        this.activityRef = new WeakReference<>(mainActivity);
        LayoutInflater inflater = LayoutInflater.from(mainActivity);
        View layout = inflater.inflate(R.layout.dlg_app_license, (ViewGroup) null);
        initView(layout);
        super.setContentView(layout);
    }

    public static boolean appLicenseAccepted(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean("app_agreement_accepted", false);
    }

    private void initView(View layout) {
        TextView tv = (TextView) layout.findViewById(R.id.tv_content);
        tv.setText(R.string.lab_app_agreement_text);
        this.inputEt = (EditText) layout.findViewById(R.id.et_input);
        Button cancelBtn = (Button) layout.findViewById(R.id.btn_cancel);
        Button acceptBtn = (Button) layout.findViewById(R.id.btn_accept);
        cancelBtn.setOnClickListener(this);
        acceptBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_cancel) {
            this.activityRef.get().finish();
            return;
        }
        if (id == R.id.btn_accept) {
            String input = this.inputEt.getText().toString();
            if (input != null && (input.trim().equalsIgnoreCase("accept") || input.trim().equalsIgnoreCase("принимаю"))) {
                this.activityRef.get().initFileWithPermissionCheck();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences((Context) this.activityRef.get());
                SharedPreferences.Editor e = sp.edit();
                e.putBoolean("app_agreement_accepted", true);
                e.commit();
                dismiss();
                return;
            }
            Toast.makeText((Context) this.activityRef.get(), R.string.input_agree_toast, 0).show();
        }
    }
}
