package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.apkeditor.base.R;
import java.io.File;
import java.lang.ref.WeakReference;

public class KeySelectDlgHelper implements View.OnClickListener, FileSelectDialog.IFileSelection {
    private Context context;
    private EditText pk8PathEt;
    private WeakReference<KeyListPreference> preferenceRef;
    private View view;
    private EditText x509PathEt;

    public KeySelectDlgHelper(KeyListPreference p) {
        this.preferenceRef = new WeakReference<>(p);
    }

    public void showDialog(Context context) {
        this.context = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        this.view = inflater.inflate(R.layout.dlg_keyselect, (ViewGroup) null, false);
        initView();
        AlertDialog.Builder db = new AlertDialog.Builder(context);
        db.setView(this.view);
        db.setTitle(R.string.custom_key_setting);
        db.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                KeySelectDlgHelper.this.setCustomKey();
            }
        });
        db.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        db.show();
    }

    private void initView() {
        this.pk8PathEt = (EditText) this.view.findViewById(R.id.et_pk8path);
        this.x509PathEt = (EditText) this.view.findViewById(R.id.et_x509path);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.context);
        this.pk8PathEt.setText(sp.getString(SettingActivity.STR_PRIVATEKEYPATH, ""));
        this.x509PathEt.setText(sp.getString(SettingActivity.STR_PUBLICKEYPATH, ""));
        ImageButton btn1 = (ImageButton) this.view.findViewById(R.id.btn_select_pk8);
        btn1.setOnClickListener(this);
        ImageButton btn2 = (ImageButton) this.view.findViewById(R.id.btn_select_x509);
        btn2.setOnClickListener(this);
    }

    protected void setCustomKey() {
        String privateKeyPath = this.pk8PathEt.getText().toString();
        String publicKeyPath = this.x509PathEt.getText().toString();
        if ("".equals(privateKeyPath) || "".equals(publicKeyPath)) {
            Toast.makeText(this.context, R.string.error_filepath_empty, 1).show();
            return;
        }
        File f1 = new File(publicKeyPath);
        File f2 = new File(privateKeyPath);
        if (!f1.exists() || !f2.exists()) {
            Toast.makeText(this.context, R.string.error_filepath_notexist, 1).show();
            return;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SettingActivity.STR_PRIVATEKEYPATH, privateKeyPath);
        editor.putString(SettingActivity.STR_PUBLICKEYPATH, publicKeyPath);
        editor.commit();
        this.preferenceRef.get().setCustomValue();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_select_pk8) {
            new FileSelectDialog(this.context, this, ".pk8", ".pk8", this.context.getString(R.string.select_key_file)).show();
        } else if (id == R.id.btn_select_x509) {
            new FileSelectDialog(this.context, this, ".x509.pem", ".pem", this.context.getString(R.string.select_key_file)).show();
        }
    }

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        if (".pk8".equals(extraStr)) {
            this.pk8PathEt.setText(filePath);
        } else {
            this.x509PathEt.setText(filePath);
        }
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return filename.endsWith(".pk8") || filename.endsWith(extraStr);
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }
}
