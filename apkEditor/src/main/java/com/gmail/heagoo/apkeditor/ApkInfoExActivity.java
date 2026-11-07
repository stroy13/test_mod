package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.ApkInfoExActivity;
import com.gmail.heagoo.apkeditor.ac.AutoCompleteAdapter;
import com.gmail.heagoo.apkeditor.ac.EditTextWithTip;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ui.AddFolderDialog;
import com.gmail.heagoo.common.ClipboardUtil;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.ZipUtil;
import com.gmail.heagoo.folderlist.FileRecord;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkInfoExActivity extends ApkInfoActivity {
    private MenuClickListener clickListener = new MenuClickListener();
    private boolean isDarkTheme;
    private View menuItem_addDir;
    private View menuItem_addFile;
    private View menuItem_delete;
    private View menuItem_details;
    private View menuItem_done;
    private View menuItem_home;
    private View menuItem_replace;
    private View menuItem_save;
    private View menuItem_search;
    private View menuItem_searchCaseS;
    private View menuItem_searchOptions;
    private View menuItem_select;
    private LinearLayout menuLayout;

    /* JADX WARN: Multi-variable type inference failed */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.isDarkTheme = GlobalConfig.instance(this).isDarkTheme();
        initResourceMenu();
    }

    public void reverseSearchOption() {
        int id;
        this.searchTextContent = !this.searchTextContent;
        if (this.isDarkTheme) {
            id = this.searchTextContent ? R.drawable.searchtxt_checked_white : R.drawable.searchtxt_unchecked_white;
        } else {
            id = this.searchTextContent ? R.drawable.searchtxt_checked : R.drawable.searchtxt_unchecked;
        }
        this.searchOptionImage.setImageResource(id);
    }

    public void reverseSearchCaseSensitive() {
        int id;
        this.searchResSensitive = !this.searchResSensitive;
        if (this.isDarkTheme) {
            id = this.searchResSensitive ? R.drawable.ic_case_sensitive_white : R.drawable.ic_case_insensitive_white;
        } else {
            id = this.searchResSensitive ? R.drawable.ic_case_sensitive : R.drawable.ic_case_insensitive;
        }
        this.searchOptionCase.setImageResource(id);
    }

    private void initResourceMenu() {
        this.menuItem_home = findViewById(R.id.menu_home);
        this.menuItem_done = findViewById(R.id.menu_done);
        this.menuItem_select = findViewById(R.id.menu_select);
        this.menuItem_addFile = findViewById(R.id.menu_addfile);
        this.menuItem_addDir = findViewById(R.id.menu_addfolder);
        this.menuItem_searchOptions = findViewById(R.id.menu_searchoptions);
        this.menuItem_searchCaseS = findViewById(R.id.menu_caseinsensitive);
        this.menuItem_home.setOnClickListener(this.clickListener);
        this.menuItem_done.setOnClickListener(this.clickListener);
        this.menuItem_select.setOnClickListener(this.clickListener);
        this.menuItem_addFile.setOnClickListener(this.clickListener);
        this.menuItem_addDir.setOnClickListener(this.clickListener);
        this.menuItem_searchOptions.setOnClickListener(this.clickListener);
        this.menuItem_searchCaseS.setOnClickListener(this.clickListener);
        this.menuLayout = (LinearLayout) findViewById(R.id.res_menu_layout);
        this.menuItem_save = createMenuItem(R.drawable.ic_save, R.drawable.ic_save_white, R.string.extract);
        this.menuItem_replace = createMenuItem(R.drawable.ic_replace, R.drawable.ic_replace_white, R.string.replace);
        this.menuItem_search = createMenuItem(R.drawable.ic_search, R.drawable.ic_search_white, R.string.search);
        this.menuItem_delete = createMenuItem(R.drawable.ic_delete, R.drawable.ic_delete_white, R.string.delete);
        this.menuItem_details = createMenuItem(R.drawable.ic_details, R.drawable.ic_details_white, R.string.detail);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(-1, -2, 1.0f);
        this.menuLayout.addView(this.menuItem_save, param);
        this.menuLayout.addView(createVerticalLine());
        this.menuLayout.addView(this.menuItem_replace, param);
        this.menuLayout.addView(createVerticalLine());
        this.menuLayout.addView(this.menuItem_search, param);
        this.menuLayout.addView(createVerticalLine());
        this.menuLayout.addView(this.menuItem_delete, param);
        this.menuLayout.addView(createVerticalLine());
        this.menuLayout.addView(this.menuItem_details, param);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private View createVerticalLine() {
        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(1, -1));
        line.setBackgroundColor(this.isDarkTheme ? -8355712 : -1842205);
        return line;
    }

    /* JADX WARN: Multi-variable type inference failed */
    private View createMenuItem(int drawable, int drawable2, int title) {
        int resId = R.layout.item_res_menu;
        View view = LayoutInflater.from(this).inflate(resId, (ViewGroup) null);
        ImageView icon = (ImageView) view.findViewById(R.id.menu_icon);
        icon.setImageResource(this.isDarkTheme ? drawable2 : drawable);
        TextView tv = (TextView) view.findViewById(R.id.menu_title);
        tv.setText(title);
        view.setId(drawable);
        view.setOnClickListener(this.clickListener);
        return view;
    }

    private void enableMenuItem(View view, boolean enabled) {
        ImageView icon = (ImageView) view.findViewById(R.id.menu_icon);
        TextView tv = (TextView) view.findViewById(R.id.menu_title);
        if (enabled) {
            icon.getDrawable().setAlpha(ParserRule.MAJOR_ACTIONS);
            tv.setEnabled(true);
        } else {
            icon.getDrawable().setAlpha(80);
            tv.setEnabled(false);
        }
        view.setClickable(enabled);
        view.setEnabled(enabled);
    }

    public void onPause() {
        super.onPause();
    }

    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void selectionChanged(Set<Integer> selected) {
        super.selectionChanged(selected);
        if (selected.size() == 1) {
            enableMenuItem(this.menuItem_replace, true);
            enableMenuItem(this.menuItem_details, true);
        } else {
            enableMenuItem(this.menuItem_replace, false);
            enableMenuItem(this.menuItem_details, false);
        }
    }

    class MenuClickListener implements View.OnClickListener {
        MenuClickListener() {
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.menu_home) {
                gotoRootDirectory();
                return;
            }
            if (id == R.id.menu_done) {
                ApkInfoExActivity.this.resListAdapter.checkAllItems(false);
                return;
            }
            if (id == R.id.menu_select) {
                selectAllOrNone();
                return;
            }
            if (id == R.id.menu_addfile) {
                ApkInfoExActivity.this.addFile(0);
                return;
            }
            if (id == R.id.menu_addfolder) {
                ApkInfoExActivity.this.createFolder(0);
                return;
            }
            if (id == R.id.menu_searchoptions) {
                ApkInfoExActivity.this.reverseSearchOption();
                return;
            }
            if (id == R.id.menu_caseinsensitive) {
                ApkInfoExActivity.this.reverseSearchCaseSensitive();
                return;
            }
            if (id == R.drawable.ic_save) {
                saveResourcesTo();
                return;
            }
            if (id == R.drawable.ic_replace) {
                replaceFileOrFolder();
                return;
            }
            if (id == R.drawable.ic_search) {
                inputKeywordAndSearch();
            } else if (id == R.drawable.ic_delete) {
                deleteSelectedResources();
            } else if (id == R.drawable.ic_details) {
                showResourceInformation();
            }
        }

        private void inputKeywordAndSearch() {
            AlertDialog.Builder builder = new AlertDialog.Builder(ApkInfoExActivity.this);
            builder.setTitle(R.string.search);
            builder.setMessage(R.string.pls_input_keyword);
            AutoCompleteAdapter adapter = new AutoCompleteAdapter(ApkInfoExActivity.this.getApplicationContext(), "res_keywords");
            LinearLayout linearLayout = new LinearLayout(ApkInfoExActivity.this);
            linearLayout.setOrientation(1);
            linearLayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
            final EditTextWithTip input = new EditTextWithTip(ApkInfoExActivity.this);
            input.setAdapter(adapter);
            linearLayout.addView(input, new ViewGroup.LayoutParams(-1, -2));
            final CheckBox caseInsstCb = new CheckBox(ApkInfoExActivity.this);
            caseInsstCb.setText(R.string.case_insensitive);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 8, 0, 0);
            caseInsstCb.setLayoutParams(params);
            linearLayout.addView(caseInsstCb);
            final CheckBox filenameCb = new CheckBox(ApkInfoExActivity.this);
            filenameCb.setText(R.string.search_file_names);
            LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(-1, -2);
            params2.setMargins(0, 8, 0, 32);
            filenameCb.setLayoutParams(params2);
            linearLayout.addView(filenameCb);
            builder.setView(linearLayout);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ApkInfoExActivity.MenuClickListener.this.m96x4383a907(input, filenameCb, caseInsstCb, dialogInterface, i);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ApkInfoExActivity.MenuClickListener.this.lambda$inputKeywordAndSearch$1(dialogInterface, i);
                }
            });
            builder.show();
        }

        void m96x4383a907(EditTextWithTip input, CheckBox filenameCb, CheckBox caseInsstCb, DialogInterface dialog, int whichButton) {
            String keyword = input.getText().toString().trim();
            if ("".equals(keyword)) {
                Toast.makeText((Context) ApkInfoExActivity.this, R.string.empty_input_tip, 1).show();
                return;
            }
            boolean bSearchName = filenameCb.isChecked();
            boolean bCaseIsst = caseInsstCb.isChecked();
            doSearchInSelectedItems(keyword, bSearchName, bCaseIsst);
        }

        void lambda$inputKeywordAndSearch$1(DialogInterface dialog, int whichButton) {
        }

        protected void doSearchInSelectedItems(String keyword, boolean bSearchName, boolean bCaseIsst) {
            Set<Integer> selected = ApkInfoExActivity.this.resListAdapter.getCheckedItems();
            if (selected.isEmpty()) {
                return;
            }
            List<FileRecord> records = new ArrayList<>();
            String baseFolder = ApkInfoExActivity.this.resListAdapter.getData(records);
            ArrayList<String> filenameList = new ArrayList<>();
            ArrayList<Integer> positions = new ArrayList<>(selected.size());
            positions.addAll(selected);
            Iterator<Integer> it = positions.iterator();
            while (it.hasNext()) {
                int index = it.next().intValue();
                filenameList.add(records.get(index).fileName);
            }
            ApkInfoExActivity.this.searchInResourceFiles(keyword, baseFolder, filenameList, bSearchName, !bCaseIsst);
        }

        private void showResourceInformation() {
            Set<Integer> selected = ApkInfoExActivity.this.resListAdapter.getCheckedItems();
            if (selected.isEmpty()) {
                return;
            }
            int position = selected.iterator().next().intValue();
            List<FileRecord> records = new ArrayList<>();
            String curDir = ApkInfoExActivity.this.resListAdapter.getData(records);
            FileRecord record = records.get(position);
            AlertDialog infoDlg = createInfoDialog(curDir, record, position);
            infoDlg.show();
        }

        private AlertDialog createInfoDialog(final String curDir, final FileRecord record, final int position) {
            String entryName;
            String entryName2;
            String entryName3;
            String fileName = record.fileName;
            String filepath = curDir + "/" + record.fileName;
            final String relativePath = filepath.substring(ApkInfoExActivity.this.decodeRootPath.length() + 1);
            String entryName4 = null;
            if (!filepath.startsWith(ApkInfoExActivity.this.decodeRootPath + "/")) {
                entryName = null;
            } else {
                String fileEntry = filepath.substring(ApkInfoExActivity.this.decodeRootPath.length() + 1);
                if (ApkInfoExActivity.this.fileEntry2ZipEntry != null) {
                    String entryName5 = ApkInfoExActivity.this.fileEntry2ZipEntry.get(fileEntry);
                    entryName4 = entryName5;
                }
                if (entryName4 != null) {
                    entryName2 = entryName4;
                } else {
                    entryName2 = fileEntry;
                }
                ZipFile zipFile = null;
                try {
                    try {
                        zipFile = new ZipFile(ApkInfoExActivity.this.apkPath);
                        if (zipFile.getEntry(entryName2) != null) {
                            entryName3 = entryName2;
                        } else {
                            entryName3 = null;
                        }
                        closeQuietly(zipFile);
                        entryName = entryName3;
                    } catch (Exception e) {
                        e.printStackTrace();
                        closeQuietly(zipFile);
                        entryName = null;
                    }
                } catch (Throwable th) {
                    closeQuietly(zipFile);
                    throw th;
                }
            }
            int resId = R.layout.dlg_resfile_more;
            View view = LayoutInflater.from(ApkInfoExActivity.this).inflate(resId, (ViewGroup) null);
            final EditText et = (EditText) view.findViewById(R.id.filename);
            et.setText(fileName);
            EditText et2 = (EditText) view.findViewById(R.id.filepath);
            et2.setText(relativePath);
            EditText et3 = (EditText) view.findViewById(R.id.fileentry);
            et3.setText(entryName != null ? entryName : ApkInfoExActivity.this.getString(R.string.not_available));
            final String _entry = entryName;
            Button extractBtn = (Button) view.findViewById(R.id.btn_extract);
            if (!record.isDir) {
                extractBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view2) {
                        ApkInfoExActivity.MenuClickListener.this.m92x7b549699(_entry, view2);
                    }
                });
            } else {
                extractBtn.setVisibility(8);
            }
            Button renameBtn = (Button) view.findViewById(R.id.btn_rename);
            if (!ApkInfoExActivity.this.isFullDecoding && record.isDir) {
                renameBtn.setVisibility(8);
            }
            renameBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view2) {
                    ApkInfoExActivity.MenuClickListener.this.m94x78169e57(et, record, curDir, _entry, position, view2);
                }
            });
            AlertDialog.Builder infoDlg = new AlertDialog.Builder(ApkInfoExActivity.this);
            infoDlg.setTitle(R.string.detail);
            infoDlg.setView(view);
            infoDlg.setNeutralButton(R.string.copy_file_path, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ApkInfoExActivity.MenuClickListener.this.m95xf677a236(relativePath, dialogInterface, i);
                }
            });
            infoDlg.setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
            return infoDlg.create();
        }

        void m92x7b549699(String _entry, View v) {
            try {
                ZipUtil.unzipFileTo(ApkInfoExActivity.this.apkPath, _entry, "/sdcard/axml");
            } catch (Exception e) {
            }
        }

        void m94x78169e57(EditText et, final FileRecord record, final String curDir, final String _entry, final int position, View v) {
            final String newName = et.getText().toString().trim();
            if (newName.equals("")) {
                Toast.makeText((Context) ApkInfoExActivity.this, R.string.empty_input_tip, 0).show();
                return;
            }
            if (newName.equals(record.fileName)) {
                Toast.makeText((Context) ApkInfoExActivity.this, R.string.no_change_detected, 0).show();
                return;
            }
            if (record.isDir || !isExtensionChanged(record.fileName, newName)) {
                doFileRename(curDir, record, _entry, newName, position);
                return;
            }
            AlertDialog.Builder dlg = new AlertDialog.Builder(ApkInfoExActivity.this);
            dlg.setMessage(R.string.extension_changed_tip);
            dlg.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ApkInfoExActivity.MenuClickListener.this.m93xf9b59a78(curDir, record, _entry, newName, position, dialogInterface, i);
                }
            });
            dlg.setNegativeButton(R.string.no, (DialogInterface.OnClickListener) null);
            dlg.show();
        }

        void m93xf9b59a78(String curDir, FileRecord record, String _entry, String newName, int position, DialogInterface dialog, int which) {
            doFileRename(curDir, record, _entry, newName, position);
        }

        void m95xf677a236(String relativePath, DialogInterface dialog, int which) {
            ApkInfoExActivity ctx = ApkInfoExActivity.this;
            ClipboardUtil.copyToClipboard(ctx, relativePath);
            String msg = ctx.getString(R.string.copied_to_clipboard);
            Toast.makeText((Context) ctx, (CharSequence) String.format(msg, relativePath), 0).show();
        }

        protected boolean isExtensionChanged(String fileName, String newName) {
            int pos = fileName.lastIndexOf(46);
            if (pos == -1) {
                return false;
            }
            String extension = fileName.substring(pos);
            int pos2 = newName.lastIndexOf(46);
            if (pos2 == -1) {
                return true;
            }
            String newExt = newName.substring(pos2);
            return !extension.equals(newExt);
        }

        protected void doFileRename(String curDir, FileRecord record, String entryName, String newName, int position) {
            if (ApkInfoExActivity.this.isFullDecoding) {
                File oldFile = new File(curDir + "/" + record.fileName);
                File newFile = new File(curDir + "/" + newName);
                oldFile.renameTo(newFile);
                return;
            }
            boolean useFileSource = true;
            if (record.isInZip) {
                useFileSource = false;
            } else if (ApkInfoExActivity.isCommonImage(record.fileName)) {
                useFileSource = false;
            }
            ZipFile zipFile = null;
            InputStream input = null;
            FileOutputStream out = null;
            try {
                try {
                    if (useFileSource) {
                        input = new FileInputStream(curDir + "/" + record.fileName);
                    } else {
                        zipFile = new ZipFile(ApkInfoExActivity.this.apkPath);
                        ZipEntry entry = zipFile.getEntry(entryName);
                        input = zipFile.getInputStream(entry);
                    }
                    String tmpFilePath = SDCard.makeWorkingDir(ApkInfoExActivity.this) + RandomUtil.getRandomString(6);
                    out = new FileOutputStream(tmpFilePath);
                    IOUtils.copy(input, out);
                    closeQuietly(out);
                    closeQuietly(input);
                    closeQuietly(zipFile);
                    ApkInfoExActivity.this.resListAdapter.deleteFile(curDir, record.fileName, record.isInZip);
                    List<Integer> positions = new ArrayList<>();
                    positions.add(Integer.valueOf(position));
                    ApkInfoExActivity.this.resListAdapter.listItemsDeleted(positions);
                    rename_addNewFile(curDir, curDir + "/" + newName, tmpFilePath);
                } catch (Exception e) {
                    Toast.makeText((Context) ApkInfoExActivity.this, R.string.str_rename_failed, 0).show();
                    closeQuietly(out);
                    closeQuietly(input);
                    closeQuietly(zipFile);
                }
            } catch (Throwable e2) {
                closeQuietly(out);
                closeQuietly(input);
                closeQuietly(zipFile);
                throw e2;
            }
        }

        private void rename_addNewFile(String dirPath, String targetPath, String filePath) {
            FileInputStream fis = null;
            try {
                try {
                    fis = new FileInputStream(filePath);
                    FileRecord rec = ApkInfoExActivity.this.resListAdapter.addFile(targetPath, fis);
                    if (rec != null) {
                        ApkInfoExActivity.this.resListAdapter.listItemAdded(dirPath, rec);
                        Toast.makeText((Context) ApkInfoExActivity.this, R.string.file_renamed, 0).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText((Context) ApkInfoExActivity.this, R.string.str_rename_failed, 0).show();
                }
            } finally {
                closeQuietly(fis);
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

        private void closeQuietly(ZipFile file) {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                }
            }
        }

        private void gotoRootDirectory() {
            ApkInfoExActivity.this.resListAdapter.openDirectory(ApkInfoExActivity.this.decodeRootPath);
            ApkInfoExActivity.this.navigationMgr.gotoDirectory(ApkInfoExActivity.this.decodeRootPath);
        }

        private void deleteSelectedResources() {
            ApkInfoExActivity.this.resListAdapter.dumpChangedFiles();
            Set<Integer> checked = ApkInfoExActivity.this.resListAdapter.getCheckedItems();
            if (checked.isEmpty()) {
                return;
            }
            List<Integer> selected = new ArrayList<>();
            selected.addAll(checked);
            Collections.sort(selected);
            ApkInfoExActivity.this.resListAdapter.deleteFile(selected);
        }

        private void saveResourcesTo() {
            Set<Integer> checked = ApkInfoExActivity.this.resListAdapter.getCheckedItems();
            if (checked.isEmpty()) {
                return;
            }
            List<Integer> selected = new ArrayList<>();
            selected.addAll(checked);
            Collections.sort(selected);
            ApkInfoExActivity.this.extractFileOrDir(selected);
        }

        private void selectAllOrNone() {
            Set<Integer> checked = ApkInfoExActivity.this.resListAdapter.getCheckedItems();
            int count = ApkInfoExActivity.this.resListAdapter.getCount();
            List<FileRecord> records = new ArrayList<>(count);
            ApkInfoExActivity.this.resListAdapter.getData(records);
            if ("..".equals(records.get(0).fileName)) {
                count--;
            }
            if (checked.size() == count) {
                ApkInfoExActivity.this.resListAdapter.checkAllItems(false);
            } else {
                ApkInfoExActivity.this.resListAdapter.checkAllItems(true);
            }
        }

        private void replaceFileOrFolder() {
            Set<Integer> selected = ApkInfoExActivity.this.resListAdapter.getCheckedItems();
            if (selected.isEmpty()) {
                return;
            }
            int position = selected.iterator().next().intValue();
            List<FileRecord> records = new ArrayList<>();
            ApkInfoExActivity.this.resListAdapter.getData(records);
            boolean isDir = records.get(position).isDir;
            if (isDir) {
                ApkInfoExActivity.this.replaceFolder(position);
            } else {
                ApkInfoExActivity.this.replaceFile(position);
            }
        }
    }
}
