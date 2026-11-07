package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.os.AsyncTask;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.neweditor.ParserRule;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SearchFilenameDialog extends Dialog implements View.OnClickListener, ResSelectionChangeListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    WeakReference<ApkInfoActivity> activityRef;
    private boolean caseSensitive;
    private Button closeBtn;
    private Button deleteBtn;
    private View doneMenu;
    private List<String> filenameList;
    private String keyword;
    private MatchedFilenameAdapter listAdapter;
    private ListView listView;
    private ArrayList<String> matchedFiles;
    private final Set<String> modifiedFiles;
    private String searchFolder;
    private LinearLayout searchingLayout;
    private View selectMenu;
    private View selectionHeaderView;
    private TextView selectionTipTv;
    private TextView titleTv;

    /* JADX WARN: Multi-variable type inference failed */
    public SearchFilenameDialog(ApkInfoActivity apkInfoActivity, String searchFolder, List<String> filenameList, String keyword, boolean caseSensitive) {
        super(apkInfoActivity);
        this.modifiedFiles = new HashSet();
        this.matchedFiles = new ArrayList<>();
        requestWindowFeature(1);
        if (GlobalConfig.instance(apkInfoActivity).isFullScreen()) {
            getWindow().setFlags(ParserRule.NO_WORD_BREAK, ParserRule.NO_WORD_BREAK);
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

    /* JADX WARN: Multi-variable type inference failed */
    private void init(ApkInfoActivity apkInfoActivity) {
        int resId = R.layout.dlg_filename_searchret;
        View view = LayoutInflater.from(apkInfoActivity).inflate(resId, (ViewGroup) null);
        this.titleTv = (TextView) view.findViewById(R.id.title);
        this.selectionHeaderView = view.findViewById(R.id.res_header_selection);
        this.selectionTipTv = (TextView) view.findViewById(R.id.selection_tip);
        this.listView = (ListView) view.findViewById(R.id.file_list);
        this.searchingLayout = (LinearLayout) view.findViewById(R.id.searching_layout);
        this.doneMenu = view.findViewById(R.id.menu_done);
        this.selectMenu = view.findViewById(R.id.menu_select);
        this.doneMenu.setOnClickListener(this);
        this.selectMenu.setOnClickListener(this);
        this.closeBtn = (Button) view.findViewById(R.id.btn_close);
        this.deleteBtn = (Button) view.findViewById(R.id.btn_delete);
        this.closeBtn.setOnClickListener(this);
        this.deleteBtn.setOnClickListener(this);
        this.listView.setVisibility(4);
        new AsyncFolderSearchTask(this.searchFolder, this.filenameList, this.keyword).execute(new Object[0]);
        setContentView(view);
    }

    public void showMatchedFiles() {
        String format = this.activityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, Integer.valueOf(this.matchedFiles.size()), this.keyword);
        this.titleTv.setText(text);
        this.listAdapter = new MatchedFilenameAdapter(this.activityRef.get(), this, this.searchFolder, this.matchedFiles);
        this.listView.setAdapter((ListAdapter) this.listAdapter);
        this.listView.setOnItemClickListener(this);
        this.listView.setOnItemLongClickListener(this);
        this.listView.setVisibility(0);
        this.searchingLayout.setVisibility(4);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            dismiss();
            return;
        }
        if (id == R.id.btn_delete) {
            deleteSelectedFiles();
            return;
        }
        if (id == R.id.menu_done) {
            this.listAdapter.selectNone();
            showNonSelectView();
        } else if (id == R.id.menu_select) {
            if (this.listAdapter.isAllSelected()) {
                this.listAdapter.selectNone();
                showNonSelectView();
            } else {
                this.listAdapter.selectAll();
            }
        }
    }

    private void deleteSelectedFiles() {
        List<Integer> selected = this.listAdapter.getSeletedItems();
        deleteFilesByIndex(selected);
    }

    private void deleteFilesByIndex(List<Integer> indexes) {
        ResListAdapter resManager = this.activityRef.get().getResListAdapter();
        Iterator<Integer> it = indexes.iterator();
        while (it.hasNext()) {
            int index = it.next().intValue();
            String filepath = this.matchedFiles.get(index);
            int pos = filepath.lastIndexOf(47);
            String dirPath = pos != -1 ? filepath.substring(0, pos) : "";
            String fileName = filepath.substring(pos + 1);
            resManager.deleteFile(dirPath, fileName, false);
        }
        ArrayList<String> fileList = new ArrayList<>();
        for (int i = 0; i < this.matchedFiles.size(); i++) {
            if (!indexes.contains(Integer.valueOf(i))) {
                fileList.add(this.matchedFiles.get(i));
            }
        }
        this.matchedFiles = fileList;
        this.listAdapter.resetFileList(this.matchedFiles, indexes);
        if (this.listAdapter.isNonSelected()) {
            showNonSelectView();
        }
    }

    private void showNonSelectView() {
        this.titleTv.setVisibility(0);
        this.selectionHeaderView.setVisibility(8);
        this.deleteBtn.setVisibility(8);
        String format = this.activityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, Integer.valueOf(this.matchedFiles.size()), this.keyword);
        this.titleTv.setText(text);
    }

    @Override
    public void selectionChanged(Set<Integer> selected) {
        if (selected.isEmpty()) {
            showNonSelectView();
            return;
        }
        String text = String.format(this.activityRef.get().getString(R.string.num_items_selected), Integer.valueOf(selected.size()));
        this.selectionTipTv.setText(text);
        this.titleTv.setVisibility(8);
        this.selectionHeaderView.setVisibility(0);
        this.deleteBtn.setVisibility(0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String filepath;
        int pos;
        if (position < this.matchedFiles.size() && (pos = (filepath = this.matchedFiles.get(position)).lastIndexOf("/")) != -1) {
            String directory = filepath.substring(0, pos);
            String fileName = filepath.substring(pos + 1);
            this.activityRef.get().openFile(directory, fileName, false);
        }
    }

    public void deleteItem(int position) {
        List<Integer> indexes = new ArrayList<>();
        indexes.add(Integer.valueOf(position));
        deleteFilesByIndex(indexes);
    }

    public void extractItem(int position) {
        if (position < this.matchedFiles.size()) {
            String filepath = this.matchedFiles.get(position);
            this.activityRef.get().extractFileOrDir(filepath);
        }
    }

    public void replaceItem(int position) {
        if (position < this.matchedFiles.size()) {
            String filepath = this.matchedFiles.get(position);
            this.activityRef.get().replaceFile(filepath, new SomethingChangedListener() {
                @Override
                public void somethingChanged() {
                    SearchFilenameDialog.this.listAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        parent.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                MenuItem item1 = menu.add(0, 1, 0, R.string.delete);
                item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        SearchFilenameDialog.this.deleteItem(position);
                        return true;
                    }
                });
                MenuItem item2 = menu.add(0, 2, 0, R.string.extract);
                item2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        SearchFilenameDialog.this.extractItem(position);
                        return true;
                    }
                });
                MenuItem item3 = menu.add(0, 3, 0, R.string.replace);
                MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        SearchFilenameDialog.this.replaceItem(position);
                        return true;
                    }
                };
                item3.setOnMenuItemClickListener(listener);
            }
        });
        return false;
    }

    private class AsyncFolderSearchTask extends AsyncTask<Object, Void, List<String>> {
        private String baseFolder;
        private List<String> filenameList;
        private String keyword;
        private String lowerCaseKeyword;

        public AsyncFolderSearchTask(String folderPath, List<String> filenameList, String keyword) {
            this.baseFolder = folderPath;
            this.filenameList = filenameList;
            this.keyword = keyword;
            this.lowerCaseKeyword = keyword.toLowerCase();
        }

        private boolean fileMatches(String filename) {
            if (SearchFilenameDialog.this.caseSensitive) {
                boolean bContain = filename.contains(this.keyword);
                return bContain;
            }
            boolean bContain2 = filename.toLowerCase().contains(this.lowerCaseKeyword);
            return bContain2;
        }

        private void searchFolder(File folderFile) {
            File[] files = folderFile.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        searchFolder(f);
                    } else if (fileMatches(f.getName())) {
                        SearchFilenameDialog.this.matchedFiles.add(f.getPath());
                    }
                }
            }
        }

        @Override
        public List<String> doInBackground(Object... params) {
            File root = new File(this.baseFolder);
            for (String filename : this.filenameList) {
                File f = new File(root, filename);
                if (f.exists()) {
                    if (f.isDirectory()) {
                        searchFolder(f);
                    } else if (fileMatches(filename)) {
                        SearchFilenameDialog.this.matchedFiles.add(f.getPath());
                    }
                }
            }
            return SearchFilenameDialog.this.matchedFiles;
        }

        @Override
        public void onPostExecute(List<String> result) {
            SearchFilenameDialog.this.showMatchedFiles();
        }
    }
}
