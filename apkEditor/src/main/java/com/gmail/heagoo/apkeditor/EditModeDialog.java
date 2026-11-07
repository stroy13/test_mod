package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import java.lang.ref.WeakReference;

public class EditModeDialog extends Dialog implements View.OnClickListener {
    public static final int COMMON_EDIT = 2;
    public static final int DATA_EDIT = 3;
    public static final int FULL_EDIT = 0;
    public static final int SIMPLE_EDIT = 1;
    public static final int XML_FILE_EDIT = 4;
    private static Boolean bX86;
    private String apkPath;
    private IEditModeSelected callback;
    private WeakReference<Context> ctxRef;
    private boolean isProVersion;
    private String packageName;
    private View view;

    public interface IEditModeSelected {
        void editModeSelected(int i, String str);
    }

    public EditModeDialog(Context context, IEditModeSelected callback, String apkPath) {
        this(context, callback, apkPath, null);
    }

    public EditModeDialog(Context context, IEditModeSelected callback, String apkPath, String packageName) {
        super(context);
        this.ctxRef = new WeakReference<>(context);
        this.callback = callback;
        this.apkPath = apkPath;
        this.packageName = packageName;
        this.isProVersion = true;
        requestWindowFeature(1);
        this.view = LayoutInflater.from(context).inflate(R.layout.dlg_editmode, (ViewGroup) null);
        setContentView(this.view);
        initView();
    }

    private static boolean isX86() {
        if (bX86 == null) {
            int isX86Val = MainActivity.isX86();
            bX86 = Boolean.valueOf(isX86Val != 0);
        }
        return bX86.booleanValue();
    }

    private void initView() {
        TextView seTv = (TextView) this.view.findViewById(R.id.simple_edit);
        TextView feTv = (TextView) this.view.findViewById(R.id.full_edit);
        TextView ceTv = (TextView) this.view.findViewById(R.id.common_edit);
        TextView xmlTv = (TextView) this.view.findViewById(R.id.xml_edit);
        seTv.setOnClickListener(this);
        feTv.setOnClickListener(this);
        ceTv.setOnClickListener(this);
        xmlTv.setOnClickListener(this);
        if (this.packageName != null && this.isProVersion) {
            View divideView = this.view.findViewById(R.id.data_edit_divide);
            divideView.setVisibility(0);
            TextView deTv = (TextView) this.view.findViewById(R.id.data_edit);
            deTv.setVisibility(0);
            deTv.setOnClickListener(this);
        }
        if (isX86()) {
            View xmlEditLayout = this.view.findViewById(R.id.xml_edit_layout);
            xmlEditLayout.setVisibility(8);
        } else {
            int i = Build.VERSION.SDK_INT;
        }
    }

    @Override
    public void onClick(View v) {
        Context ctx;
        int id = v.getId();
        dismiss();
        if ((id == R.id.simple_edit || id == R.id.full_edit || id == R.id.common_edit || id == R.id.xml_edit) && (ctx = this.ctxRef.get()) != null) {
            RecentFiles.record(ctx, this.apkPath, this.packageName);
        }
        if (id == R.id.simple_edit) {
            this.callback.editModeSelected(1, this.apkPath);
            return;
        }
        if (id == R.id.full_edit) {
            this.callback.editModeSelected(0, this.apkPath);
            return;
        }
        if (id == R.id.common_edit) {
            this.callback.editModeSelected(2, this.apkPath);
        } else if (id == R.id.xml_edit) {
            this.callback.editModeSelected(4, this.apkPath);
        } else if (id == R.id.data_edit) {
            this.callback.editModeSelected(3, this.packageName);
        }
    }
}
