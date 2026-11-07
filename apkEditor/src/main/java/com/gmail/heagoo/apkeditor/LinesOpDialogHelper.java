package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;

public class LinesOpDialogHelper {
    private Context context;
    private AlertDialog d;
    private EditText fromEt;
    private ILinesOperation opInterface;
    private EditText toEt;

    interface ILinesOperation {
        void operateLines(int i, int i2);
    }

    public void showDialog(Context context, int titleResId, ILinesOperation opInterface) {
        this.context = context;
        this.opInterface = opInterface;
        LayoutInflater inflater = LayoutInflater.from(context);
        View content = inflater.inflate(R.layout.dlg_lines_op, (ViewGroup) null);
        initView(content);
        this.d = new AlertDialog.Builder(context).setView(content).setTitle(titleResId).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        this.d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = LinesOpDialogHelper.this.d.getButton(-1);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LinesOpDialogHelper.this.positiveBtnClicked();
                    }
                });
            }
        });
        this.d.show();
    }

    public void positiveBtnClicked() {
        String fromStr = this.fromEt.getText().toString();
        String toStr = this.toEt.getText().toString();
        if ("".equals(fromStr)) {
            Toast.makeText(this.context, R.string.empty_input_tip, 1).show();
            this.fromEt.requestFocus();
            return;
        }
        if ("".equals(toStr)) {
            Toast.makeText(this.context, R.string.empty_input_tip, 1).show();
            this.toEt.requestFocus();
            return;
        }
        try {
            int from = Integer.valueOf(fromStr).intValue();
            int to = Integer.valueOf(toStr).intValue();
            if (from > to) {
                Toast.makeText(this.context, R.string.err_from_greater_than_to, 1).show();
                this.fromEt.requestFocus();
            } else if (this.opInterface != null) {
                this.opInterface.operateLines(from, to);
                this.d.dismiss();
            }
        } catch (Exception e) {
            Toast.makeText(this.context, R.string.unknown_error, 1).show();
        }
    }

    private void initView(View content) {
        this.fromEt = (EditText) content.findViewById(R.id.et_from);
        this.toEt = (EditText) content.findViewById(R.id.et_to);
    }
}
