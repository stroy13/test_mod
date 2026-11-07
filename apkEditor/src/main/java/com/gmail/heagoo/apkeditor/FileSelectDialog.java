package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.InputUtil;
import com.gmail.heagoo.common.PathUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.folderlist.FileRecord;
import com.gmail.heagoo.folderlist.FileSort;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileSelectDialog extends Dialog implements AdapterView.OnItemClickListener, View.OnClickListener, AdapterView.OnItemLongClickListener {
    private static final String LAST_DIR = "lastDirectory";
    private IFileSelection callback;
    private Context ctx;
    private CheckBox editCheckBox;
    private String extraStr;
    private ResListAdapter fileListAdapter;
    private TextView pathTv;
    private boolean selectFolder;
    private boolean showConfirmDlg;
    private String tag;
    private TextView titleTv;

    public interface IFileSelection {
        void fileSelectedInDialog(String str, String str2, boolean z);

        String getConfirmMessage(String str, String str2);

        boolean isInterestedFile(String str, String str2);
    }

    public FileSelectDialog(Context ctx, IFileSelection callback, String fileSuffix, String extraString, String strTitle) {
        this(ctx, callback, fileSuffix, extraString, strTitle, false, false, false, null);
    }

    public FileSelectDialog(Context ctx, IFileSelection callback, String fileSuffix, String extraString, String strTitle, boolean selectFolder, boolean showConfirmDlg, boolean showEditOption, String tag) {
        this(ctx, callback, fileSuffix, extraString, strTitle, selectFolder, showConfirmDlg, showEditOption, tag, null);
    }

    public FileSelectDialog(Context ctx, IFileSelection callback, String fileSuffix, String extraString, String strTitle, boolean selectFolder, boolean showConfirmDlg, boolean showEditOption, String tag, String defaultDir) {
        this(ctx, callback, fileSuffix, extraString, strTitle, selectFolder, showConfirmDlg, showEditOption, tag, defaultDir, false);
    }

    public FileSelectDialog(Context ctx, IFileSelection callback, String fileSuffix, String extraString, String strTitle, boolean selectFolder, boolean showConfirmDlg, boolean showEditOption, String tag, String defaultDir, boolean preferDefaultDirectory) {
        super(ctx);
        String key;
        String strTitle2;
        super.requestWindowFeature(1);
        this.ctx = ctx;
        this.callback = callback;
        this.extraStr = extraString;
        this.selectFolder = selectFolder;
        this.showConfirmDlg = showConfirmDlg;
        this.tag = tag;
        int resId = R.layout.dlg_fileselect;
        View view = LayoutInflater.from(ctx).inflate(resId, (ViewGroup) null, false);
        Button confirmBtn = (Button) view.findViewById(R.id.confirm);
        if (selectFolder) {
            confirmBtn.setVisibility(0);
            confirmBtn.setOnClickListener(this);
        } else {
            confirmBtn.setVisibility(8);
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (tag == null) {
            key = LAST_DIR;
        } else {
            String key2 = tag + "_" + LAST_DIR;
            key = key2;
        }
        String lastDir = sp.getString(key, "");
        if (preferDefaultDirectory && defaultDir != null && new File(defaultDir).isDirectory()) {
            lastDir = defaultDir;
        }
        if (!new File(lastDir).exists()) {
            if (defaultDir == null) {
                lastDir = SDCard.getRootDirectory();
            } else {
                lastDir = defaultDir;
            }
        }
        this.titleTv = (TextView) view.findViewById(R.id.tv_title);
        this.pathTv = (TextView) view.findViewById(R.id.tv_subtitle);
        if (strTitle == null) {
            strTitle2 = fileSuffix != null ? ctx.getString(R.string.select_file_replace) + " (" + fileSuffix + ")" : ctx.getString(R.string.select_file_replace);
        } else {
            strTitle2 = strTitle;
        }
        this.titleTv.setText(strTitle2);
        this.pathTv.setText(lastDir);
        final ListView fileList = (ListView) view.findViewById(R.id.file_list);
        this.fileListAdapter = new ResListAdapter(ctx, null, lastDir, "/", new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                File f = new File(dir, filename);
                return f.isDirectory() || FileSelectDialog.this.isInterestedFile(filename);
            }
        });
        fileList.setAdapter((ListAdapter) this.fileListAdapter);
        fileList.setOnItemClickListener(this);
        fileList.setOnItemLongClickListener(this);
        applyFileSort();
        view.findViewById(R.id.file_dialog_sort).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view2) {
                FileSelectDialog.this.m215lambda$new$0$comgmailheagooapkeditorFileSelectDialog(fileList, view2);
            }
        });
        this.editCheckBox = (CheckBox) view.findViewById(R.id.cb_edit_before_replace);
        if (showEditOption) {
            this.editCheckBox.setText(String.format(ctx.getString(R.string.edit_before_replace), PathUtil.getNameFromPath(extraString)));
            this.editCheckBox.setChecked(getHistoryEditOption());
            this.editCheckBox.setVisibility(0);
        } else {
            this.editCheckBox.setVisibility(8);
        }
        Button closeBtn = (Button) view.findViewById(R.id.close);
        closeBtn.setOnClickListener(this);
        setContentView(view);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
    }

    void m215lambda$new$0$comgmailheagooapkeditorFileSelectDialog(ListView fileList, View v) {
        showFileSortDialog(fileList);
    }

    public void applyFileSort() {
        SharedPreferences prefs = this.ctx.getSharedPreferences("config", 0);
        int key = prefs.getInt("file_sort_key", 0);
        boolean descending = prefs.getBoolean("file_sort_desc", false);
        synchronized (this.fileListAdapter.fileList) {
            String current = this.fileListAdapter.getData(null);
            for (FileRecord record : this.fileListAdapter.fileList) {
                if (!"..".equals(record.fileName)) {
                    File file = new File(current, record.fileName);
                    record.lastModified = file.lastModified();
                    record.totalSize = record.isDir ? -1L : file.length();
                }
            }
            Collections.sort(this.fileListAdapter.fileList, new FileSort(key, descending));
        }
        this.fileListAdapter.notifyDataSetChanged();
    }

    private void showFileSortDialog(final ListView listView) {
        final SharedPreferences sharedPreferences = this.ctx.getSharedPreferences("config", 0);
        int i = sharedPreferences.getInt("file_sort_key", 0);
        if (i < 0 || i > 3) {
            i = 0;
        }
        new AlertDialog.Builder(this.ctx).setTitle(R.string.file_sort_title).setSingleChoiceItems(R.array.file_sort_options, (i * 2) + (sharedPreferences.getBoolean("file_sort_desc", false) ? 1 : 0), new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i2) {
                FileSelectDialog.this.m216x2fed4e18(sharedPreferences, listView, dialogInterface, i2);
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    void m216x2fed4e18(SharedPreferences prefs, ListView fileList, DialogInterface dialog, int which) {
        prefs.edit().putInt("file_sort_key", which / 2).putBoolean("file_sort_desc", which % 2 != 0).apply();
        applyFileSort();
        fileList.setSelection(0);
        dialog.dismiss();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        parent.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                MenuItem item1 = menu.add(0, 1, 0, R.string.new_folder);
                item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        FileSelectDialog.this.createFolder();
                        return true;
                    }
                });
            }
        });
        return false;
    }

    public void createFolder() {
        final String dirPath = this.fileListAdapter.getData(null);
        AlertDialog.Builder inputDlg = new AlertDialog.Builder(this.ctx);
        inputDlg.setTitle(R.string.new_folder);
        inputDlg.setMessage(R.string.pls_input_foldername);
        final EditText input = new EditText(this.ctx);
        InputFilter filter = InputUtil.getFileNameFilter();
        input.setFilters(new InputFilter[]{filter});
        inputDlg.setView(input);
        inputDlg.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = input.getText().toString().trim();
                if ("".equals(name)) {
                    Toast.makeText(FileSelectDialog.this.ctx, R.string.empty_input_tip, 1).show();
                } else {
                    FileSelectDialog.this.fileListAdapter.addFolder(dirPath, name);
                    FileSelectDialog.this.applyFileSort();
                }
            }
        });
        inputDlg.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        inputDlg.show();
    }

    public boolean isInterestedFile(String filename) {
        return this.callback.isInterestedFile(filename, this.extraStr);
    }

    protected void close() {
        dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        String targetPath;
        List<FileRecord> fileList = new ArrayList<>();
        String oldDir = this.fileListAdapter.getData(fileList);
        FileRecord rec = fileList.get(position);
        if (rec == null) {
            return;
        }
        if (rec.isDir) {
            if (rec.fileName.equals("..")) {
                int pos = oldDir.lastIndexOf(47);
                targetPath = oldDir.substring(0, pos);
            } else {
                targetPath = oldDir + "/" + rec.fileName;
            }
            this.fileListAdapter.openDirectory(targetPath);
            applyFileSort();
            String curPath = this.fileListAdapter.getData(null);
            this.pathTv.setText(curPath);
            return;
        }
        if (!this.selectFolder && isInterestedFile(rec.fileName)) {
            String selectedPath = oldDir + "/" + rec.fileName;
            boolean editSelected = isEditSelected();
            this.callback.fileSelectedInDialog(selectedPath, this.extraStr, editSelected);
            saveLastDirectory(oldDir);
            saveEditOption(editSelected);
            close();
        }
    }

    public void saveLastDirectory(String lastDir) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        SharedPreferences.Editor editor = sp.edit();
        String key = LAST_DIR;
        if (this.tag != null) {
            key = this.tag + "_" + LAST_DIR;
        }
        editor.putString(key, lastDir);
        editor.apply();
    }

    private boolean getHistoryEditOption() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        String key = this.tag != null ? this.tag + "_editBeforeReplace" : "editBeforeReplace";
        return sp.getBoolean(key, false);
    }

    private void saveEditOption(boolean editChecked) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        SharedPreferences.Editor editor = sp.edit();
        String key = this.tag != null ? this.tag + "_editBeforeReplace" : "editBeforeReplace";
        editor.putBoolean(key, editChecked);
        editor.apply();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.close) {
            close();
            return;
        }
        if (id == R.id.confirm) {
            final String curDir = this.fileListAdapter.getData(null);
            if (this.showConfirmDlg) {
                new AlertDialog.Builder(this.ctx).setTitle(R.string.confirm_dir_replace).setMessage(this.callback.getConfirmMessage(curDir, this.extraStr)).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FileSelectDialog.this.callback.fileSelectedInDialog(curDir, FileSelectDialog.this.extraStr, FileSelectDialog.this.isEditSelected());
                        FileSelectDialog.this.saveLastDirectory(curDir);
                        FileSelectDialog.this.close();
                    }
                }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
                return;
            }
            this.callback.fileSelectedInDialog(curDir, this.extraStr, isEditSelected());
            saveLastDirectory(curDir);
            close();
        }
    }

    public boolean isEditSelected() {
        return this.editCheckBox.isChecked();
    }
}
