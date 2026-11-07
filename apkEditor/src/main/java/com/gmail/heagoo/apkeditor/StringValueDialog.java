package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ClipboardUtil;
import java.lang.ref.WeakReference;

public class StringValueDialog extends Dialog implements View.OnClickListener {
    private WeakReference<Context> ctxRef;
    private TextView keyTv;
    private int position;
    private StringListAdapter strListAdapter;
    private EditText valueEt;
    private View view;

    public StringValueDialog(Context context, StringListAdapter strListAdapter, int position) {
        super(context);
        requestWindowFeature(1);
        this.ctxRef = new WeakReference<>(context);
        this.strListAdapter = strListAdapter;
        this.position = position;
        int layoutId = R.layout.dlg_stringvalue;
        this.view = LayoutInflater.from(context).inflate(layoutId, (ViewGroup) null);
        setContentView(this.view);
        this.keyTv = (TextView) this.view.findViewById(R.id.key);
        this.valueEt = (EditText) this.view.findViewById(R.id.value);
        View menu = this.view.findViewById(R.id.menu_clipboard);
        menu.setOnClickListener(this);
        Button okBtn = (Button) this.view.findViewById(R.id.btn_editstring_ok);
        okBtn.setOnClickListener(this);
        Button cancelBtn = (Button) this.view.findViewById(R.id.btn_editstring_cancel);
        cancelBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_editstring_ok) {
            String newValue = this.valueEt.getText().toString();
            this.strListAdapter.checkTextChange(this.position, newValue);
            dismiss();
        } else {
            if (id == R.id.btn_editstring_cancel) {
                cancel();
                return;
            }
            if (id == R.id.menu_clipboard) {
                Context ctx = this.ctxRef.get();
                String str = this.keyTv.getText().toString();
                ClipboardUtil.copyToClipboard(ctx, str);
                String msg = ctx.getString(R.string.copied_to_clipboard);
                Toast.makeText(ctx, String.format(msg, str), 0).show();
            }
        }
    }

    public void setKeyValue(String key, String val) {
        this.keyTv.setText(key);
        this.valueEt.setText(val);
        this.valueEt.setSelection(val != null ? val.length() : 0);
    }
}
