package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.EditorTheme;
import java.lang.ref.WeakReference;

public class ProcessingDialog extends Dialog implements View.OnClickListener {
    private WeakReference<Activity> activityRef;
    private ProcessingInterface processor;
    private boolean started;
    private int successTipResId;

    public interface ProcessingInterface {
        void afterProcess();

        void process() throws Exception;
    }

    public ProcessingDialog(Activity activity, ProcessingInterface processor, int okTipResId) {
        super(activity, EditorTheme.dialogStyle(activity));
        this.activityRef = new WeakReference<>(activity);
        this.processor = processor;
        this.successTipResId = okTipResId;
        requestWindowFeature(1);
        LayoutInflater inflater = LayoutInflater.from(activity);
        View layout = inflater.inflate(R.layout.dlg_processing, (ViewGroup) null);
        super.setContentView(layout);
        super.setCancelable(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!this.started) {
            this.started = true;
            new ProcessingThread(this).start();
        }
    }

    protected void processCompleted(final String errMsg) {
        Activity activity = this.activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ProcessingDialog.this.processor.afterProcess();
                    if (errMsg != null) {
                        ProcessingDialog.this.showTip("Failed: " + errMsg);
                    } else {
                        ProcessingDialog.this.showTip(ProcessingDialog.this.successTipResId);
                    }
                    if (ProcessingDialog.this.isShowing()) {
                        ProcessingDialog.this.dismissWithoutThrow();
                    }
                }
            });
        }
    }

    public void dismissWithoutThrow() {
        try {
            dismiss();
        } catch (Exception e) {
        }
    }

    protected void showTip(String msg) {
        Activity activity = this.activityRef.get();
        if (activity != null) {
            Toast.makeText(activity, msg, 0).show();
        }
    }

    protected void showTip(int resId) {
        Activity activity;
        if (resId != -1 && (activity = this.activityRef.get()) != null) {
            Toast.makeText(activity, resId, 0).show();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.close_button) {
            dismissWithoutThrow();
        }
    }

    static class ProcessingThread extends Thread {
        private WeakReference<ProcessingDialog> dlgRef;

        public ProcessingThread(ProcessingDialog dlg) {
            this.dlgRef = new WeakReference<>(dlg);
        }

        @Override
        public void run() {
            String errMsg = null;
            ProcessingDialog dlg = this.dlgRef.get();
            if (dlg != null) {
                try {
                    dlg.processor.process();
                } catch (Exception e) {
                    errMsg = e.getMessage();
                }
                dlg.processCompleted(errMsg);
            }
        }
    }
}
