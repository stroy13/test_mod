package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.AssetManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.IOUtils;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class SmaliCodeDialog extends Dialog implements View.OnClickListener {
    private WeakReference<Activity> activityRef;
    private EditText codeEt;
    boolean isPro;
    private String smaliRootFolder;
    private Spinner spinner;
    static int[] smaliNameIds = {R.string.show_a_toast, R.string.log_a_message, R.string.dump_a_value, R.string.print_stack_trace};
    static String[] smaliCodes = {"    const-string v0, \"This is a toast.\"\n    # p0 (this object) must be an object of Context\n    invoke-static {p0, v0}, Lapkeditor/Utils;->showToast(Landroid/content/Context;Ljava/lang/String;)V", "    # use 'adb logcat APKEDITOR:* *:S' to view the log\n    const-string v0, \"I am here.\"\n    invoke-static {v0}, Lapkeditor/Utils;->log(Ljava/lang/String;)V", "    # use 'adb logcat APKEDITOR:* *:S' to view the value\n    invoke-static {v0}, Lapkeditor/Utils;->dumpValue(Ljava/lang/Object;)V", "    # use 'adb logcat APKEDITOR:* *:S' to view the stack trace\n    invoke-static {}, Lapkeditor/Utils;->printCallStack()V"};

    public SmaliCodeDialog(Activity activity, String filePath) {
        super(activity);
        requestWindowFeature(1);
        this.activityRef = new WeakReference<>(activity);
        this.isPro = true;
        this.smaliRootFolder = getSmaliRootFolder(filePath);
        init(activity);
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        getWindow().setLayout((width * 6) / 7, -2);
    }

    private String getSmaliRootFolder(String filePath) {
        String[] dirs = filePath.split("/");
        String smaliPath = "";
        for (String dir : dirs) {
            smaliPath = smaliPath + dir + "/";
            if ("smali".equals(dir) || dir.startsWith("smali_")) {
                break;
            }
        }
        return smaliPath;
    }

    private void init(Activity activity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dlg_smalicode, (ViewGroup) null);
        this.spinner = (Spinner) view.findViewById(R.id.spinner_codename);
        String[] names = new String[smaliNameIds.length];
        for (int i = 0; i < smaliNameIds.length; i++) {
            names[i] = activity.getString(smaliNameIds[i]);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinner.setAdapter((SpinnerAdapter) adapter);
        this.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                SmaliCodeDialog.this.updateSmaliCode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        this.codeEt = (EditText) view.findViewById(R.id.et_samplecode);
        Button copyBtn = (Button) view.findViewById(R.id.btn_copy);
        copyBtn.setOnClickListener(this);
        Button closeBtn = (Button) view.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);
        setContentView(view);
    }

    protected void updateSmaliCode(int position) {
        if (position < smaliCodes.length) {
            this.codeEt.setText(smaliCodes[position]);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            dismiss();
            return;
        }
        if (id == R.id.btn_copy) {
            Activity activity = this.activityRef.get();
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService("clipboard");
            ClipData clip = ClipData.newPlainText("code", this.codeEt.getText().toString());
            clipboard.setPrimaryClip(clip);
            copyUtilSmali();
            Toast.makeText(activity, R.string.smali_copied, 0).show();
        }
    }

    private void copyUtilSmali() {
        String dirPath = this.smaliRootFolder + "apkeditor";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String filePath = dirPath + "/Utils.smali";
        File file = new File(filePath);
        if (!file.exists()) {
            FileOutputStream fos = null;
            InputStream is = null;
            try {
                try {
                    fos = new FileOutputStream(file);
                    AssetManager am = this.activityRef.get().getAssets();
                    is = am.open("smali_patch/Utils.smali");
                    IOUtils.copy(is, fos);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                closeQuietly(fos);
                closeQuietly(is);
            }
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }
}
