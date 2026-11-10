package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.folderlist.util.OpenFiles;
import java.lang.ref.WeakReference;

class ManifestLongClickDlg extends Dialog {
    private WeakReference<Activity> activityRef;
    private IManifestChangeCallback deleteCallbacker;
    private LineRecord lineRec;
    private String xmlPath;

    ManifestLongClickDlg(Activity activity, String xmlPath, LineRecord lineRec, IManifestChangeCallback callback) {
        super(activity);
        requestWindowFeature(1);
        this.activityRef = new WeakReference<>(activity);
        this.xmlPath = xmlPath;
        this.lineRec = lineRec;
        this.deleteCallbacker = callback;
        init();
    }

    private void init() {
        View view = this.activityRef.get().getLayoutInflater().inflate(R.layout.dlg_manifestline, (ViewGroup) null, false);
        TextView contentTv = (TextView) view.findViewById(R.id.content);
        contentTv.setText(this.lineRec.lineData);
        TextView descTv = (TextView) view.findViewById(R.id.description);
        String desc = getDescription();
        descTv.setText(desc != null ? desc : "");
        TextView deleteTv = (TextView) view.findViewById(R.id.delete);
        deleteTv.setClickable(true);
        deleteTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String errMsg = ManifestLongClickDlg.this.deleteCallbacker.tryToDeleteSection(ManifestLongClickDlg.this.lineRec);
                if (errMsg == null) {
                    ManifestLongClickDlg.this.close();
                } else if (!errMsg.equals("")) {
                    Toast.makeText((Context) ManifestLongClickDlg.this.activityRef.get(), errMsg, 0).show();
                }
            }
        });
        TextView extractTv = (TextView) view.findViewById(R.id.extract);
        extractTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dlgTitle = ((Activity) ManifestLongClickDlg.this.activityRef.get()).getString(R.string.select_folder);
                FileSelectDialog.IFileSelection callback = new FileSelectDialog.IFileSelection() {
                    @Override
                    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
                        FileCopyDialog dlg = new FileCopyDialog((Context) ManifestLongClickDlg.this.activityRef.get(), ManifestLongClickDlg.this.xmlPath, filePath, null, null, null, 0);
                        dlg.show();
                    }

                    @Override
                    public boolean isInterestedFile(String filename, String extraStr) {
                        return true;
                    }

                    @Override
                    public String getConfirmMessage(String filePath, String extraStr) {
                        return null;
                    }
                };
                FileSelectDialog dlg = new FileSelectDialog((Context) ManifestLongClickDlg.this.activityRef.get(), callback, null, null, dlgTitle, true, false, false, null);
                dlg.show();
                ManifestLongClickDlg.this.close();
            }
        });
        TextView replaceTv = (TextView) view.findViewById(R.id.replace);
        replaceTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ManifestLongClickDlg.this.isProVersion()) {
                    ManifestListAdapter.showPromoteDialog((Context) ManifestLongClickDlg.this.activityRef.get());
                    return;
                }
                FileSelectDialog dlg = new FileSelectDialog((Context) ManifestLongClickDlg.this.activityRef.get(), new FileSelectDialog.IFileSelection() {
                    @Override
                    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
                        if (openFile) {
                            ((ApkInfoActivity) ManifestLongClickDlg.this.activityRef.get()).saveParams(filePath, extraStr, null);
                            OpenFiles.openFile((Activity) ManifestLongClickDlg.this.activityRef.get(), filePath, 1001);
                        } else {
                            ApkInfoActivity activity = (ApkInfoActivity) ManifestLongClickDlg.this.activityRef.get();
                            if (activity.replaceFile(extraStr, filePath)) activity.setManifestModified(true);
                        }
                    }

                    @Override
                    public boolean isInterestedFile(String filename, String extraStr) {
                        return filename.endsWith(".xml");
                    }

                    @Override
                    public String getConfirmMessage(String filePath, String extraStr) {
                        return null;
                    }
                }, ".xml", ManifestLongClickDlg.this.xmlPath, null, false, false, true, null);
                dlg.show();
                ManifestLongClickDlg.this.close();
            }
        });
        TextView openTv = (TextView) view.findViewById(R.id.open_in_new_window);
        openTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ManifestLongClickDlg.this.isProVersion()) {
                    ManifestListAdapter.showPromoteDialog((Context) ManifestLongClickDlg.this.activityRef.get());
                    return;
                }
                Intent intent = TextEditor.getEditorIntent(((Activity) ManifestLongClickDlg.this.activityRef.get()).getApplicationContext(), ManifestLongClickDlg.this.xmlPath, null);
                ((Activity) ManifestLongClickDlg.this.activityRef.get()).startActivityForResult(intent, 2);
                ManifestLongClickDlg.this.close();
            }
        });
        TextView closeTv = (TextView) view.findViewById(R.id.close);
        closeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ManifestLongClickDlg.this.close();
            }
        });
        setContentView(view);
    }

    public boolean isProVersion() {
        return true;
    }

    public void close() {
        dismiss();
    }

    private String getSearchKeywords() {
        String name = this.lineRec.getName();
        if (name == null || name.startsWith(".")) {
            return "AndroidManifest " + this.lineRec.getSectionTag();
        }
        return name;
    }

    private String getDescription() {
        String tag = this.lineRec.getSectionTag();
        if (tag != null) {
            return ManifestDesc.getDescription(tag);
        }
        return null;
    }
}
