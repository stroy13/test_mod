package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import java.lang.ref.WeakReference;

public class DecodeModeDialog extends Dialog implements View.OnClickListener {
    private String apkPath;
    private IDecodeModeSelected callback;
    private WeakReference<Context> ctxRef;
    private String packageName;
    private View view;

    public interface IDecodeModeSelected {
        void decodeModeSelected(int i, String str);
    }

    public DecodeModeDialog(Context context, IDecodeModeSelected callback, String apkPath) {
        this(context, callback, apkPath, null);
    }

    public DecodeModeDialog(Context context, IDecodeModeSelected callback, String apkPath, String packageName) {
        super(context);
        this.ctxRef = new WeakReference<>(context);
        this.callback = callback;
        this.apkPath = apkPath;
        this.packageName = packageName;
        requestWindowFeature(1);
        this.view = LayoutInflater.from(context).inflate(R.layout.dlg_decodemode, (ViewGroup) null);
        setContentView(this.view);
        initView();
    }

    private void initView() {
        TextView tv1 = (TextView) this.view.findViewById(R.id.decode_all_files);
        TextView tv2 = (TextView) this.view.findViewById(R.id.decode_partial_files);
        TextView tv3 = (TextView) this.view.findViewById(R.id.cancel);
        tv1.setOnClickListener(this);
        tv2.setOnClickListener(this);
        tv3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.decode_all_files) {
            this.callback.decodeModeSelected(0, this.apkPath);
        } else if (id == R.id.decode_partial_files) {
            this.callback.decodeModeSelected(1, this.apkPath);
        }
        dismiss();
    }
}
