package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.ac.AutoCompleteAdapter;
import com.gmail.heagoo.apkeditor.ac.EditTextWithTip;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ActivityUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: /tmp/apkx/classes5.dex */
public class SearchTextDialog extends Dialog implements ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener, View.OnClickListener, AdapterView.OnItemLongClickListener {
    private WeakReference<ApkInfoActivity> activityRef;
    private AutoCompleteAdapter adapter;
    private boolean caseSensitive;
    private EditTextWithTip etReplaceAll;
    private List<String> filenameList;
    private String keyword;
    private MatchedTextListAdapter listAdapter;
    private ExpandableListView listView;
    private ArrayList<String> matchedFiles;
    private String searchFolder;
    private LinearLayout searchingLayout;
    private TextView titleTv;

    /* JADX WARN: Multi-variable type inference failed */
    public SearchTextDialog(ApkInfoActivity apkInfoActivity, String searchFolder, List<String> filenameList, String keyword, boolean caseSensitive) {
        super(apkInfoActivity);
        this.matchedFiles = new ArrayList<>();
        requestWindowFeature(1);
        if (GlobalConfig.instance(apkInfoActivity).isFullScreen()) {
            getWindow().setFlags(1024, 1024);
        }
        this.activityRef = new WeakReference<>(apkInfoActivity);
        this.searchFolder = searchFolder;
        this.filenameList = filenameList;
        this.keyword = keyword;
        this.caseSensitive = caseSensitive;
        if (!this.searchFolder.endsWith("/")) {
            String str = searchFolder + "/";
        }
        init(apkInfoActivity);
    }

    private void init(Activity activity) {
        int resId = R.layout.dialog_txt_searchresult;
        View view = LayoutInflater.from(activity).inflate(resId, (ViewGroup) null);
        this.titleTv = (TextView) view.findViewById(R.id.title);
        this.etReplaceAll = view.findViewById(R.id.et_replaceall);
        this.listView = (ExpandableListView) view.findViewById(R.id.lv_matchedfiles);
        this.searchingLayout = (LinearLayout) view.findViewById(R.id.searching_layout);
        this.listView.setVisibility(4);
        new AsyncFolderSearchTask(this.searchFolder, this.filenameList, this.keyword, this.caseSensitive).execute(new Object[0]);
        view.findViewById(R.id.btn_replaceall).setOnClickListener(this);
        this.adapter = new AutoCompleteAdapter(activity, "search_replace_with");
        EditTextWithTip etReplaceAll = view.findViewById(R.id.et_replaceall);
        etReplaceAll.setAdapter(this.adapter);
        setContentView(view);
    }

    @Override // android.widget.ExpandableListView.OnGroupClickListener
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        boolean expanded = parent.isGroupExpanded(groupPosition);
        if (expanded || this.listAdapter.groupChildExist(groupPosition)) {
            return false;
        }
        String filePath = (String) this.listAdapter.getGroup(groupPosition);
        String keyword = this.listAdapter.getKeyword();
        new AsyncFileSearchTask(filePath, keyword, groupPosition).execute(new Object[0]);
        return true;
    }

    /* JADX WARN: Type inference failed for: r3v3, types: [android.content.Context, com.gmail.heagoo.apkeditor.ApkInfoActivity] */
    /* JADX WARN: Type inference failed for: r4v8, types: [android.content.Context, com.gmail.heagoo.apkeditor.ApkInfoActivity] */
    @Override // android.widget.ExpandableListView.OnChildClickListener
    public boolean onChildClick(ExpandableListView parent, View v, int groupPos, int childPos, long id) {
        Intent intent;
        ArrayList<String> filePathList = this.listAdapter.getFileList();
        MatchedLineItem item = (MatchedLineItem) this.listAdapter.getChild(groupPos, childPos);
        if (item == null) {
            return false;
        }
        if (filePathList.size() > 100) {
            String filePath = filePathList.get(groupPos);
            ApkInfoActivity apkInfoActivity = this.activityRef.get();
            intent = TextEditor.getEditorIntent(apkInfoActivity, filePath, apkInfoActivity.getApkPath());
            ActivityUtil.attachParam(intent, "startLine", "" + item.lineIndex);
        } else {
            ApkInfoActivity apkInfoActivity2 = this.activityRef.get();
            intent = TextEditor.getEditorIntent(apkInfoActivity2, filePathList, groupPos, apkInfoActivity2.getApkPath());
            ActivityUtil.attachParam(intent, "fileList", filePathList);
            ActivityUtil.attachParam(intent, "curFileIndex", groupPos);
            ArrayList<Integer> startLineList = new ArrayList<>(filePathList.size());
            for (int i = 0; i < groupPos; i++) {
                startLineList.add(-1);
            }
            int i2 = item.lineIndex;
            startLineList.add(Integer.valueOf(i2));
            for (int i3 = groupPos + 1; i3 < filePathList.size(); i3++) {
                startLineList.add(-1);
            }
            ActivityUtil.attachParam2(intent, "startLineList", startLineList);
        }
        ActivityUtil.attachParam(intent, "searchString", this.keyword);
        this.activityRef.get().startActivityForResult(intent, 0);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showMatchedFiles() {
        String format = this.activityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, Integer.valueOf(this.matchedFiles.size()), this.keyword);
        this.titleTv.setText(text);
        this.listAdapter = new MatchedTextListAdapter(this.activityRef, this.listView, this.searchFolder, this.matchedFiles, this.keyword);
        this.listView.setAdapter(this.listAdapter);
        this.listView.setOnGroupClickListener(this);
        this.listView.setOnChildClickListener(this);
        this.listView.setOnItemLongClickListener(this);
        this.listView.setVisibility(0);
        this.searchingLayout.setVisibility(4);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_replaceall) {
            showConfirmDialog();
        }
    }

    private void showConfirmDialog() {
        final String strReplace = this.etReplaceAll.getText().toString();
        AlertDialog.Builder comfirmDlg = new AlertDialog.Builder((Context) this.activityRef.get());
        String msg = String.format(this.activityRef.get().getString(R.string.sure_to_replace_all), this.keyword, strReplace);
        comfirmDlg.setMessage(msg);
        comfirmDlg.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { // from class: com.gmail.heagoo.apkeditor.SearchTextDialog.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int whichButton) {
                if (!"".equals(strReplace.trim())) {
                    SearchTextDialog.this.adapter.addInputHistory(strReplace);
                }
                SearchTextDialog.this.doReplaceAll(strReplace);
            }
        });
        comfirmDlg.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.gmail.heagoo.apkeditor.SearchTextDialog.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        comfirmDlg.show();
    }

    protected void doReplaceAll(final String strReplace) {
        new ProcessingDialog((Activity) this.activityRef.get(), new ProcessingDialog.ProcessingInterface() { // from class: com.gmail.heagoo.apkeditor.SearchTextDialog.3
            int failedNum = 0;
            String failMessage = "";

            @Override // com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface
            public void process() throws Exception {
                Iterator it = SearchTextDialog.this.matchedFiles.iterator();
                while (it.hasNext()) {
                    String f = (String) it.next();
                    try {
                        SearchTextDialog.this.listAdapter.replaceWith(f, strReplace);
                        SearchTextDialog.this.addModification(f);
                    } catch (Exception e) {
                        this.failMessage += "\n" + String.format(((ApkInfoActivity) SearchTextDialog.this.activityRef.get()).getString(R.string.failed_to_modify), f);
                        this.failedNum++;
                    }
                }
            }

            @Override // com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface
            public void afterProcess() {
                for (int i = 0; i < SearchTextDialog.this.listAdapter.getGroupCount(); i++) {
                    SearchTextDialog.this.listView.collapseGroup(i);
                    SearchTextDialog.this.listAdapter.removeSearchResult(i);
                }
                String msg = String.format(((ApkInfoActivity) SearchTextDialog.this.activityRef.get()).getString(R.string.str_num_modified_file), Integer.valueOf(SearchTextDialog.this.matchedFiles.size() - this.failedNum));
                if (this.failedNum > 0) {
                    Toast.makeText((Context) SearchTextDialog.this.activityRef.get(), msg + this.failMessage, 1).show();
                } else {
                    Toast.makeText((Context) SearchTextDialog.this.activityRef.get(), msg, 0).show();
                }
            }
        }, -1);
    }

    public void addModification(String filePath) {
        this.activityRef.get().dealWithModifiedFile(filePath, null);
    }

    @Override // android.widget.AdapterView.OnItemLongClickListener
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        int itemType = ExpandableListView.getPackedPositionType(id);
        if (itemType != 0) {
            return true;
        }
        final int groupIdx = ExpandableListView.getPackedPositionGroup(id);
        parent.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() { // from class: com.gmail.heagoo.apkeditor.SearchTextDialog.4
            @Override // android.view.View.OnCreateContextMenuListener
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                MenuItem item1 = menu.add(0, 1, 0, R.string.delete);
                item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { // from class: com.gmail.heagoo.apkeditor.SearchTextDialog.4.1
                    @Override // android.view.MenuItem.OnMenuItemClickListener
                    public boolean onMenuItemClick(MenuItem item) {
                        SearchTextDialog.this.deleteItem(groupIdx);
                        return true;
                    }
                });
                MenuItem item2 = menu.add(0, 2, 0, R.string.extract);
                item2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { // from class: com.gmail.heagoo.apkeditor.SearchTextDialog.4.2
                    @Override // android.view.MenuItem.OnMenuItemClickListener
                    public boolean onMenuItemClick(MenuItem item) {
                        SearchTextDialog.this.extractItem(groupIdx);
                        return true;
                    }
                });
                MenuItem item3 = menu.add(0, 3, 0, R.string.replace);
                MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() { // from class: com.gmail.heagoo.apkeditor.SearchTextDialog.4.3
                    @Override // android.view.MenuItem.OnMenuItemClickListener
                    public boolean onMenuItemClick(MenuItem item) {
                        SearchTextDialog.this.replaceItem(groupIdx);
                        return true;
                    }
                };
                item3.setOnMenuItemClickListener(listener);
            }
        });
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteItem(int position) {
        ResListAdapter resManager = this.activityRef.get().getResListAdapter();
        String filepath = this.matchedFiles.get(position);
        int pos = filepath.lastIndexOf(47);
        String dirPath = pos != -1 ? filepath.substring(0, pos) : "";
        String fileName = filepath.substring(pos + 1);
        resManager.deleteFile(dirPath, fileName, false);
        this.listAdapter.removeItem(position);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void extractItem(int position) {
        if (position < this.matchedFiles.size()) {
            String filepath = this.matchedFiles.get(position);
            this.activityRef.get().extractFileOrDir(filepath);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void replaceItem(final int position) {
        if (position < this.matchedFiles.size()) {
            String filepath = this.matchedFiles.get(position);
            this.activityRef.get().replaceFile(filepath, new SomethingChangedListener() { // from class: com.gmail.heagoo.apkeditor.SearchTextDialog.5
                @Override // com.gmail.heagoo.apkeditor.SomethingChangedListener
                public void somethingChanged() {
                    SearchTextDialog.this.listView.collapseGroup(position);
                    SearchTextDialog.this.listAdapter.removeSearchResult(position);
                }
            });
        }
    }

    private class AsyncFolderSearchTask extends AsyncTask<Object, Void, List<String>> {
        private String baseFolder;
        private boolean caseSensitive;
        private List<String> filenameList;
        private String keyword;
        private String lcKeyword;

        public AsyncFolderSearchTask(String folderPath, List<String> filenameList, String keyword, boolean caseSensitive) {
            this.baseFolder = folderPath;
            this.filenameList = filenameList;
            this.keyword = keyword;
            this.lcKeyword = keyword.toLowerCase();
            this.caseSensitive = caseSensitive;
        }

        /* JADX WARN: Code restructure failed: missing block: B:25:0x004d, code lost:
        
            if (0 == 0) goto L29;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private boolean fileContainsKeyword(File file) {
            boolean ret = false;
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line = br.readLine();
                if (this.caseSensitive) {
                    while (line != null) {
                        if (line.contains(this.keyword)) {
                            ret = true;
                            break;
                        }
                        line = br.readLine();
                    }
                } else {
                    while (line != null) {
                        if (line.toLowerCase().contains(this.lcKeyword)) {
                            ret = true;
                            break;
                        }
                        line = br.readLine();
                    }
                }
            } catch (Exception e) {
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        br.close();
                    } catch (IOException e2) {
                    }
                }
                throw th;
            }
            try {
                br.close();
            } catch (IOException e3) {
            }
            return ret;
        }

        private void searchFolder(File folderFile) {
            File[] files = folderFile.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        searchFolder(f);
                    } else if (isTxtFile(f) && fileContainsKeyword(f)) {
                        SearchTextDialog.this.matchedFiles.add(f.getPath());
                    }
                }
            }
        }

        private boolean isTxtFile(File f) {
            String name = f.getName();
            return name.endsWith(".xml") || name.endsWith(".smali") || name.endsWith(".txt");
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public List<String> doInBackground(Object... params) {
            File root = new File(this.baseFolder);
            for (String filename : this.filenameList) {
                File f = new File(root, filename);
                if (f.exists()) {
                    if (f.isDirectory()) {
                        searchFolder(f);
                    } else if (isTxtFile(f) && fileContainsKeyword(f)) {
                        SearchTextDialog.this.matchedFiles.add(f.getPath());
                    }
                }
            }
            return SearchTextDialog.this.matchedFiles;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(List<String> result) {
            SearchTextDialog.this.showMatchedFiles();
        }
    }

    private class AsyncFileSearchTask extends AsyncTask<Object, Void, TxtSearchResult> {
        private String filePath;
        private int groupPosition;
        private String keyword;

        public AsyncFileSearchTask(String filePath, String keyword, int groupPosition) {
            this.filePath = filePath;
            this.keyword = keyword;
            this.groupPosition = groupPosition;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        /* JADX WARN: Code restructure failed: missing block: B:25:0x006a, code lost:
        
            if (r2 == null) goto L27;
         */
        @Override // android.os.AsyncTask
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public TxtSearchResult doInBackground(Object... params) {
            int position;
            TxtSearchResult result = new TxtSearchResult();
            result.filePath = this.filePath;
            result.keyword = this.keyword;
            String lcKeyword = this.keyword.toLowerCase();
            BufferedReader br = null;
            try {
                List<MatchedLineItem> matchedItems = new ArrayList<>();
                br = new BufferedReader(new InputStreamReader(new FileInputStream(this.filePath)));
                int lineIndex = 1;
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    if (SearchTextDialog.this.caseSensitive) {
                        position = line.indexOf(this.keyword);
                    } else {
                        position = line.toLowerCase().indexOf(lcKeyword);
                    }
                    if (position != -1) {
                        matchedItems.add(new MatchedLineItem(lineIndex, position, line));
                    }
                    lineIndex++;
                }
                result.matchList = matchedItems;
            } catch (Exception e) {
            } catch (Throwable th) {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e2) {
                    }
                }
                throw th;
            }
            try {
                br.close();
            } catch (IOException e3) {
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(TxtSearchResult result) {
            if (result.matchList != null) {
                SearchTextDialog.this.listAdapter.addSearchResult(result.filePath, result.matchList);
            }
            SearchTextDialog.this.listView.expandGroup(this.groupPosition);
        }
    }
}
