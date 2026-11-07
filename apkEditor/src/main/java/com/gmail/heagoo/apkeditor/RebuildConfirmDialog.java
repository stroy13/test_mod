package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import com.gmail.heagoo.apkeditor.base.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RebuildConfirmDialog {
    private Context ctx;
    private CheckBox dexCb;
    private boolean manifestModified;
    private CheckBox resCb;
    private boolean resFileModified;
    private CheckBox signCb;
    private Map<String, Integer> smaliAddedFiles;
    private Set<String> smaliFolders;
    private Map<String, Integer> smaliModifiedFiles;
    private Map<String, Integer> smaliRemovedFiles;
    private boolean stringModified;
    private int resAddedFiles = 0;
    private int resRemovedFiles = 0;
    private int resModifiedFiles = 0;
    private int otherAddedFiles = 0;
    private int otherRemovedFiles = 0;
    private int otherModifiedFiles = 0;

    public RebuildConfirmDialog(Context ctx, boolean stringModified, boolean manifestModified, Map<String, String> added, Map<String, String> replaced, Set<String> deleted) {
        this.ctx = ctx;
        this.stringModified = stringModified;
        this.manifestModified = manifestModified;
        initData(added, replaced, deleted);
    }

    private void initData(Map<String, String> added, Map<String, String> replaced, Set<String> deleted) {
        this.resFileModified = false;
        this.smaliFolders = new HashSet();
        this.smaliAddedFiles = new HashMap();
        this.smaliRemovedFiles = new HashMap();
        this.smaliModifiedFiles = new HashMap();
        for (Map.Entry<String, String> entry : added.entrySet()) {
            String entryName = entry.getKey();
            if (entryName.startsWith("res/")) {
                if (!this.resFileModified && !ApkInfoActivity.isCommonImage(entryName)) {
                    this.resFileModified = true;
                }
                this.resAddedFiles++;
            } else {
                String smaliFoder = ApkInfoActivity.dealWithSmaliFile(entryName, this.smaliFolders);
                if (smaliFoder != null) {
                    Integer v = this.smaliAddedFiles.get(smaliFoder);
                    if (v == null) {
                        this.smaliAddedFiles.put(smaliFoder, 1);
                    } else {
                        this.smaliAddedFiles.put(smaliFoder, Integer.valueOf(v.intValue() + 1));
                    }
                } else {
                    this.otherAddedFiles++;
                }
            }
        }
        for (Map.Entry<String, String> entry2 : replaced.entrySet()) {
            String entryName2 = entry2.getKey();
            if (entryName2.startsWith("res/")) {
                if (!this.resFileModified && !ApkInfoActivity.isCommonImage(entryName2)) {
                    this.resFileModified = true;
                }
                this.resModifiedFiles++;
            } else {
                String smaliFoder2 = ApkInfoActivity.dealWithSmaliFile(entryName2, this.smaliFolders);
                if (smaliFoder2 != null) {
                    Integer v2 = this.smaliModifiedFiles.get(smaliFoder2);
                    if (v2 == null) {
                        this.smaliModifiedFiles.put(smaliFoder2, 1);
                    } else {
                        this.smaliModifiedFiles.put(smaliFoder2, Integer.valueOf(v2.intValue() + 1));
                    }
                } else {
                    this.otherModifiedFiles++;
                }
            }
        }
        for (String entryName3 : deleted) {
            if (entryName3.startsWith("res/")) {
                if (!this.resFileModified && !ApkInfoActivity.isCommonImage(entryName3)) {
                    this.resFileModified = true;
                }
                this.resRemovedFiles++;
            } else {
                String smaliFoder3 = ApkInfoActivity.dealWithSmaliFile(entryName3, this.smaliFolders);
                if (smaliFoder3 != null) {
                    Integer v3 = this.smaliRemovedFiles.get(smaliFoder3);
                    if (v3 == null) {
                        this.smaliRemovedFiles.put(smaliFoder3, 1);
                    } else {
                        this.smaliRemovedFiles.put(smaliFoder3, Integer.valueOf(v3.intValue() + 1));
                    }
                } else {
                    this.otherRemovedFiles++;
                }
            }
        }
    }

    private List<ModificationCategory> getModifications() {
        List<ModificationCategory> modList = new ArrayList<>();
        if (this.stringModified) {
            modList.add(new ModificationCategory(this.ctx.getString(R.string.string), -1, -1, -1));
        } else {
            modList.add(new ModificationCategory(this.ctx.getString(R.string.string), 0, 0, 0));
        }
        modList.add(new ModificationCategory(this.ctx.getString(R.string.resource), this.resAddedFiles, this.resRemovedFiles, this.resModifiedFiles));
        if (this.manifestModified) {
            modList.add(new ModificationCategory(this.ctx.getString(R.string.manifest), -1, -1, -1));
        } else {
            modList.add(new ModificationCategory(this.ctx.getString(R.string.manifest), 0, 0, 0));
        }
        for (String smaliFolder : this.smaliFolders) {
            Integer add = this.smaliAddedFiles.get(smaliFolder);
            Integer remove = this.smaliRemovedFiles.get(smaliFolder);
            Integer modify = this.smaliModifiedFiles.get(smaliFolder);
            modList.add(new ModificationCategory(smaliFolder, add == null ? 0 : add.intValue(), remove == null ? 0 : remove.intValue(), modify == null ? 0 : modify.intValue()));
        }
        if (this.otherAddedFiles > 0 || this.otherRemovedFiles > 0 || this.otherModifiedFiles > 0) {
            modList.add(new ModificationCategory(this.ctx.getString(R.string.others), this.otherAddedFiles, this.otherRemovedFiles, this.otherModifiedFiles));
        }
        return modList;
    }

    public void show() {
        AlertDialog.Builder confirmDlg = new AlertDialog.Builder(this.ctx);
        confirmDlg.setTitle(R.string.rebuild_the_apk);
        View layout = LayoutInflater.from(this.ctx).inflate(R.layout.dlg_rebuild_confirm, (ViewGroup) null);
        ExpandableListView list = (ExpandableListView) layout.findViewById(R.id.modificationList);
        List<ModificationCategory> modList = getModifications();
        list.setAdapter(new ModificationAdapter(this.ctx, modList));
        for (int i = 0; i < modList.size(); i++) {
            list.expandGroup(i);
        }
        int i2 = R.id.cb_rebuild_dex;
        this.dexCb = (CheckBox) layout.findViewById(i2);
        this.resCb = (CheckBox) layout.findViewById(R.id.cb_rebuild_res);
        this.signCb = (CheckBox) layout.findViewById(R.id.cb_resign);
        this.dexCb.setChecked(!this.smaliFolders.isEmpty());
        this.resCb.setChecked(this.stringModified || this.manifestModified || this.resFileModified);
        this.dexCb.setEnabled(false);
        this.resCb.setEnabled(false);
        this.signCb.setChecked(true);
        confirmDlg.setView(layout);
        confirmDlg.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                boolean bSign = RebuildConfirmDialog.this.signCb.isChecked();
                ((ApkInfoActivity) RebuildConfirmDialog.this.ctx).build(bSign);
            }
        });
        confirmDlg.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        confirmDlg.show();
    }
}
