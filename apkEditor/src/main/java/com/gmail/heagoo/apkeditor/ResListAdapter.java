package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.se.ZipImageZoomer;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.ImageZoomer;
import com.gmail.heagoo.common.PathUtil;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.StringUtil;
import com.gmail.heagoo.folderlist.FileRecord;
import com.gmail.heagoo.folderlist.FileListSorter;
import com.gmail.heagoo.folderlist.FileSort;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResListAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener {
    private Map<String, String> allFileAdded;
    private Set<String> allFileDeleted;
    private Map<String, String> allFileReplaced;
    private String apkPath;
    private LruCache<String, BitmapInfo> bitmapCache;
    private Set<Integer> checkedItems;
    private WeakReference<Context> ctxRef;
    private String curPath;
    private Map<String, String> fileEntry2ZipEntry;
    List<FileRecord> fileList;
    private int sortKey = FileSort.NAME;
    private boolean sortDescending;
    private FilenameFilter filter;
    private WeakReference<ResSelectionChangeListener> listenerRef;
    private int resourceId;
    private ZipNode rootNode;
    private String rootPath;
    private String strResolution;
    private ZipImageZoomer zipImageZoomer;

    public ResListAdapter(Context ctx, String apkPath, String curPath, String rootPath, FilenameFilter filter) {
        this(ctx, apkPath, curPath, rootPath, null, filter, null);
    }

    public ResListAdapter(Context ctx, String apkPath, String curPath, String rootPath, Map<String, String> fileEntry2ZipEntry, FilenameFilter filter, ResSelectionChangeListener listener) {
        this.fileList = new ArrayList();
        this.checkedItems = new HashSet();
        this.bitmapCache = new LruCache<String, BitmapInfo>(64) {
            @Override
            public void entryRemoved(boolean evicted, String key, BitmapInfo oldValue, BitmapInfo newValue) {
                if (oldValue.bitmap != null) {
                    oldValue.bitmap.recycle();
                }
            }
        };
        this.rootNode = new ZipNode();
        this.allFileReplaced = new HashMap();
        this.allFileAdded = new HashMap();
        this.allFileDeleted = new HashSet();
        this.ctxRef = new WeakReference<>(ctx);
        if (listener != null) {
            this.listenerRef = new WeakReference<>(listener);
        }
        this.apkPath = apkPath;
        this.rootPath = rootPath;
        this.curPath = curPath;
        this.fileEntry2ZipEntry = fileEntry2ZipEntry;
        this.filter = filter;
        this.strResolution = ctx.getResources().getString(R.string.resolution);
        this.resourceId = listener != null ? R.layout.item_file_selectable : R.layout.item_file;
        if (apkPath != null) {
            initZipData(apkPath);
        }
        initListData(curPath);
    }

    public void refresh() {
        openDirectory(this.curPath);
    }

    private void initZipData(String apkFilePath) {
        try {
            ZipFile zipFile = new ZipFile(apkFilePath);
            this.zipImageZoomer = new ZipImageZoomer(zipFile);
            Enumeration<? extends ZipEntry> entryEnum = zipFile.entries();
            while (entryEnum.hasMoreElements()) {
                ZipEntry entry = entryEnum.nextElement();
                String entryName = entry.getName();
                if (!entryName.startsWith("res/") && !entryName.startsWith("r/") && !entryName.startsWith("META-INF/")) {
                    String[] paths = entryName.split("/");
                    if (paths.length != 1 || (!entryName.equals("AndroidManifest.xml") && !entryName.endsWith(".dex") && !entryName.endsWith(".arsc"))) {
                        this.rootNode.addChildByPath(paths, true);
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public String getData(List<FileRecord> records) {
        String str;
        synchronized (this.fileList) {
            if (records != null) {
                records.addAll(this.fileList);
            }
            str = this.curPath;
        }
        return str;
    }

    private void initListData(String path) {
        synchronized (this.fileList) {
            this.checkedItems.clear();
            File dir = new File(path);
            if (!dir.exists()) {
                listSubNodes(path);
            } else {
                listSubFiles(path);
            }
            sortFileList();
        }
    }

    private void sortFileList() {
        Set<FileRecord> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Integer position : this.checkedItems) {
            if (position != null && position >= 0 && position < this.fileList.size()) {
                selected.add(this.fileList.get(position));
            }
        }
        FileListSorter.sort(this.fileList, sortKey, sortDescending, this.curPath);
        this.checkedItems.clear();
        if (!selected.isEmpty()) {
            for (int i = 0; i < this.fileList.size(); i++) {
                if (selected.contains(this.fileList.get(i))) this.checkedItems.add(i);
            }
        }
    }

    public void setSort(int key, boolean descending) {
        synchronized (this.fileList) {
            this.sortKey = FileSort.normalizeKey(key);
            this.sortDescending = descending;
            sortFileList();
        }
        notifyDataSetChanged();
    }

    private void listSubNodes(String path) {
        String relativePath = path.substring(this.rootPath.length() + 1);
        String[] paths = relativePath.split("/");
        ZipNode parent = this.rootNode.findFolderByPath(paths);
        if (parent != null) {
            this.fileList.clear();
            List<ZipNode> children = parent.getChildren();
            if (children != null) {
                for (ZipNode node : children) {
                    FileRecord fr = new FileRecord();
                    fr.fileName = node.name;
                    fr.isDir = !node.isFile;
                    fr.isInZip = true;
                    this.fileList.add(fr);
                }
            }
            FileRecord fr2 = new FileRecord();
            fr2.fileName = "..";
            fr2.isDir = true;
            this.fileList.add(0, fr2);
            this.curPath = path;
        }
    }

    private void listSubFiles(String path) {
        File[] subFiles;
        File dir = new File(path);
        boolean isRootDir = path.equals(this.rootPath);
        if (isRootDir && this.filter != null) {
            subFiles = dir.listFiles(this.filter);
        } else {
            subFiles = dir.listFiles();
        }
        if (subFiles != null) {
            this.fileList.clear();
            for (File f : subFiles) {
                FileRecord fr = new FileRecord();
                fr.fileName = f.getName();
                fr.isDir = f.isDirectory();
                this.fileList.add(fr);
            }
            if (!isRootDir) {
                FileRecord fr2 = new FileRecord();
                fr2.fileName = "..";
                fr2.isDir = true;
                this.fileList.add(0, fr2);
            } else {
                List<FileRecord> extraRecords = new ArrayList<>();
                List<ZipNode> children = this.rootNode.children;
                if (children != null && !children.isEmpty()) {
                    for (ZipNode node : children) {
                        FileRecord fr3 = new FileRecord();
                        fr3.fileName = node.name;
                        fr3.isDir = !node.isFile;
                        fr3.isInZip = true;
                        extraRecords.add(fr3);
                    }
                    this.fileList.addAll(extraRecords);
                }
            }
            this.curPath = path;
            return;
        }
        if (PathUtil.isParentFolderOf(path, SDCard.getRootDirectory())) {
            this.fileList.clear();
            SDCard.getRootDirectory();
            FileRecord fr4 = new FileRecord();
            fr4.fileName = PathUtil.getSubFolder(path, SDCard.getRootDirectory());
            fr4.isDir = true;
            this.fileList.add(fr4);
            if (!path.equals(this.rootPath)) {
                FileRecord fr5 = new FileRecord();
                fr5.fileName = "..";
                fr5.isDir = true;
                fr5.totalSize = -1L;
                this.fileList.add(0, fr5);
            }
            this.curPath = path;
        }
    }

    @Override
    public int getCount() {
        return this.fileList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public ImageThumbnailInfo getImageInfo(String filepath) {
        String entryName = filepath.substring(this.rootPath.length() + 1);
        return getImageInfo(entryName, false);
    }

    private ImageThumbnailInfo getImageInfo(String entryName, boolean bInZip) {
        String t;
        Bitmap bitmap = null;
        String detailInfo = null;
        BitmapInfo bmpInfo = getModifiedImageInfo(entryName);
        if (bmpInfo != null) {
            bitmap = bmpInfo.bitmap;
            detailInfo = this.strResolution + ": " + bmpInfo.width + " X " + bmpInfo.height;
        } else if (this.apkPath == null) {
            ImageZoomer zoomer = new ImageZoomer();
            bitmap = zoomer.getImageThumbnail(this.rootPath + "/" + entryName, 32, 32);
            detailInfo = this.strResolution + ": " + zoomer.getOriginWidth() + " X " + zoomer.getOriginHeight();
        } else if (!bInZip && entryName.endsWith(".9.png")) {
            ImageZoomer zoomer2 = new ImageZoomer();
            bitmap = zoomer2.getImageThumbnail(this.rootPath + "/" + entryName, 32, 32);
            detailInfo = this.strResolution + ": " + zoomer2.getOriginWidth() + " X " + zoomer2.getOriginHeight();
        } else {
            if (this.fileEntry2ZipEntry != null && (t = this.fileEntry2ZipEntry.get(entryName)) != null) {
                entryName = t;
            }
            BitmapInfo bmpInfo2 = getImageInfoFromZip(entryName);
            if (bmpInfo2 != null) {
                bitmap = bmpInfo2.bitmap;
                detailInfo = this.strResolution + ": " + bmpInfo2.width + " X " + bmpInfo2.height;
            }
        }
        return new ImageThumbnailInfo(bitmap, detailInfo);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        FileRecord rec = this.fileList.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(this.ctxRef.get()).inflate(this.resourceId, (ViewGroup) null);
            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.file_icon);
            viewHolder.filename = (TextView) convertView.findViewById(R.id.filename);
            viewHolder.desc1 = (TextView) convertView.findViewById(R.id.detail1);
            viewHolder.checkbox = (CheckBox) convertView.findViewById(R.id.checkBox);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String detailInfo = null;
        viewHolder.filename.setText(rec.fileName);
        if (rec.fileName.equals("..")) {
            viewHolder.icon.setImageResource(R.drawable.folderutil_up);
        } else if (rec.isDir) {
            viewHolder.icon.setImageResource(R.drawable.folderutil_folder);
        } else {
            if (rec.fileName.endsWith(".xml")) {
                viewHolder.icon.setImageResource(R.drawable.util_xml_file);
            }
            if (isImageFile(rec.fileName)) {
                String entryName = getEntryName(this.curPath, rec.fileName);
                ImageThumbnailInfo info = getImageInfo(entryName, rec.isInZip);
                viewHolder.icon.setImageBitmap(info.thumbnail);
                detailInfo = info.detailInfo;
            } else if (rec.fileName.endsWith(".xml")) {
                viewHolder.icon.setImageResource(R.drawable.util_xml_file);
            } else {
                viewHolder.icon.setImageResource(R.drawable.folderutil_file);
            }
        }
        if (detailInfo != null) {
            viewHolder.desc1.setText(detailInfo);
            viewHolder.desc1.setVisibility(0);
        } else {
            viewHolder.desc1.setVisibility(8);
        }
        if (viewHolder.checkbox != null) {
            if (position != 0 || !rec.fileName.equals("..")) {
                viewHolder.checkbox.setVisibility(0);
                viewHolder.checkbox.setId(position);
                viewHolder.checkbox.setOnCheckedChangeListener(null);
                viewHolder.checkbox.setChecked(this.checkedItems.contains(Integer.valueOf(position)));
                viewHolder.checkbox.setOnCheckedChangeListener(this);
            } else {
                viewHolder.checkbox.setVisibility(4);
            }
        }
        return convertView;
    }

    private String getEntryName(String curPath, String fileName) {
        if (curPath.equals(this.rootPath)) {
            return fileName;
        }
        int position = this.rootPath.endsWith("/") ? this.rootPath.length() : this.rootPath.length() + 1;
        String entryName = curPath.substring(position) + "/" + fileName;
        return entryName;
    }

    private BitmapInfo getModifiedImageInfo(String entryName) {
        ImageZoomer zoomer;
        Bitmap bitmap;
        String path = this.allFileAdded.get(entryName);
        if (path == null) {
            path = this.allFileReplaced.get(entryName);
        }
        if (path != null && (bitmap = (zoomer = new ImageZoomer()).getImageThumbnail(path, 32, 32)) != null) {
            return new BitmapInfo(bitmap, zoomer.getOriginWidth(), zoomer.getOriginHeight());
        }
        return null;
    }

    private BitmapInfo getImageInfoFromZip(String entryName) {
        BitmapInfo bmpInfo = this.bitmapCache.get(entryName);
        if (bmpInfo == null) {
            Bitmap bitmap = this.zipImageZoomer.getImageThumbnail(entryName, 32, 32);
            if (bitmap != null) {
                int width = this.zipImageZoomer.getOriginWidth();
                int height = this.zipImageZoomer.getOriginHeight();
                bmpInfo = new BitmapInfo(bitmap, width, height);
            }
            if (bmpInfo != null) {
                this.bitmapCache.put(entryName, bmpInfo);
            }
        }
        return bmpInfo;
    }

    public boolean isImageFile(String fileName) {
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
            return true;
        }
        return false;
    }

    public void openDirectory(String targetPath) {
        if (this.rootPath.startsWith(targetPath) && !targetPath.equals(this.rootPath)) {
            return;
        }
        synchronized (this.fileList) {
            this.checkedItems.clear();
            initListData(targetPath);
        }
        notifyDataSetChanged();
    }

    protected void listItemsDeleted(List<Integer> positions) {
        Iterator<FileRecord> it = this.fileList.iterator();
        int index = 0;
        while (it.hasNext()) {
            it.next();
            if (positions.contains(Integer.valueOf(index))) {
                it.remove();
            }
            index++;
        }
        this.checkedItems.clear();
        notifyDataSetChanged();
        if (this.listenerRef.get() != null) {
            this.listenerRef.get().selectionChanged(this.checkedItems);
        }
    }

    public void listItemAdded(String dirPath, FileRecord rec) {
        synchronized (this.fileList) {
            if (this.curPath.equals(dirPath)) {
                this.fileList.add(rec);
                sortFileList();
            }
        }
        notifyDataSetChanged();
    }

    public void addFile(String targetPath, String filePath) {
        int pos = targetPath.lastIndexOf("/");
        String filename = targetPath.substring(pos + 1);
        String dirPath = targetPath.substring(0, pos);
        FileInputStream fis = null;
        try {
            try {
                fis = new FileInputStream(filePath);
                FileRecord rec = addFile(targetPath, fis);
                if (rec != null) {
                    listItemAdded(dirPath, rec);
                    String msg = String.format(this.ctxRef.get().getString(R.string.file_added), filename);
                    Toast.makeText(this.ctxRef.get(), msg, 0).show();
                }
            } catch (Exception e) {
                String msg2 = String.format(this.ctxRef.get().getString(R.string.failed_1), e.getMessage());
                Toast.makeText(this.ctxRef.get(), msg2, 1).show();
                if (fis == null) {
                    return;
                }
            }
            try {
                fis.close();
            } catch (IOException e2) {
            }
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e3) {
                }
            }
            throw th;
        }
    }

    public FileRecord addFile(String targetPath, InputStream input) throws Exception {
        String entryName;
        boolean bInZip;
        int position = targetPath.lastIndexOf(47);
        String dirPath = targetPath.substring(0, position);
        String filename = targetPath.substring(position + 1);
        if (dirPath.equals(this.rootPath)) {
            entryName = filename;
        } else {
            String entryName2 = this.rootPath;
            entryName = targetPath.substring(entryName2.length() + 1);
        }
        if (dirPath.equals(this.rootPath) && this.apkPath != null) {
            checkCanPutInRootPath(filename);
        }
        File dir = new File(dirPath);
        if (insideResOrSmali(dirPath) || (dir.exists() && !dirPath.equals(this.rootPath))) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            bInZip = false;
            if (new File(targetPath).exists()) {
                throwExistException(entryName);
            } else {
                FileOutputStream out = new FileOutputStream(targetPath);
                IOUtils.copy(input, out);
                out.close();
            }
        } else {
            bInZip = true;
            String[] paths = entryName.split("/");
            ZipNode zipNode = this.rootNode.findNodeByPath(paths);
            if (zipNode != null) {
                throwExistException(entryName);
            } else {
                targetPath = SDCard.makeWorkingDir(this.ctxRef.get()) + RandomUtil.getRandomString(8);
                FileOutputStream out2 = new FileOutputStream(targetPath);
                IOUtils.copy(input, out2);
                out2.close();
                this.rootNode.addChildByPath(paths, true);
            }
        }
        recordFileAdd(entryName, targetPath);
        FileRecord rec = new FileRecord(filename, false, bInZip);
        return rec;
    }

    private boolean insideResOrSmali(String dirPath) {
        String resDir = this.rootPath + "/res";
        if (dirPath.equals(resDir) || dirPath.startsWith(resDir + "/")) {
            return true;
        }
        String smaliDir = this.rootPath + "/smali";
        return dirPath.equals(smaliDir) || dirPath.startsWith(new StringBuilder().append(smaliDir).append("/").toString()) || dirPath.startsWith(new StringBuilder().append(smaliDir).append("_").toString());
    }

    private void checkCanPutInRootPath(String filename) throws Exception {
        if (filename.equals("META-INF")) {
            throwExistException(filename);
        }
        ZipFile zfile = new ZipFile(this.apkPath);
        ZipEntry entry = zfile.getEntry(filename);
        if (entry != null) {
            zfile.close();
            throwExistException(filename);
        } else {
            zfile.close();
        }
    }

    public void addFolder(String dirPath, String folderName) {
        try {
            addFolderReportError(dirPath, folderName, true);
        } catch (Exception e) {
            Toast.makeText(this.ctxRef.get(), e.getMessage(), 1).show();
        }
    }

    public void addFolderReportError(String dirPath, String folderName, boolean bUiThread) throws Exception {
        String entryName;
        if (dirPath.equals(this.rootPath) && this.apkPath != null) {
            checkCanPutInRootPath(folderName);
        }
        File dir = new File(dirPath);
        if (dir.exists() && !dirPath.equals(this.rootPath)) {
            File f = new File(dirPath, folderName);
            if (f.exists()) {
                throwExistException(folderName);
            }
            boolean ret = f.mkdir();
            if (bUiThread) {
                if (!ret && this.ctxRef.get() != null) {
                    Toast.makeText(this.ctxRef.get(), R.string.failed, 1).show();
                    return;
                } else {
                    openDirectory(dirPath);
                    return;
                }
            }
            return;
        }
        if (dirPath.equals(this.rootPath)) {
            entryName = folderName;
        } else {
            entryName = dirPath.substring(this.rootPath.length() + 1) + "/" + folderName;
        }
        String[] paths = entryName.split("/");
        ZipNode node = this.rootNode.findNodeByPath(paths);
        if (node != null) {
            throwExistException(folderName);
            return;
        }
        this.rootNode.addChildByPath(paths, false);
        if (bUiThread) {
            openDirectory(dirPath);
        }
    }

    private void throwExistException(String filename) throws Exception {
        throw new Exception(String.format(this.ctxRef.get().getString(R.string.file_already_exist), filename));
    }

    public void fileModified(String entryName, String newFilePath) {
        recordFileReplace(entryName, newFilePath);
    }

    public void replaceFile(String decodedPath, String newPath) {
        String entryPath = decodedPath.substring(this.rootPath.length() + 1);
        if (new File(decodedPath).exists()) {
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                try {
                    in = new FileInputStream(newPath);
                    out = new FileOutputStream(decodedPath);
                    IOUtils.copy(in, out);
                    recordFileReplace(entryPath, decodedPath);
                    Toast.makeText(this.ctxRef.get(), R.string.file_replaced, 0).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } finally {
                closeQuietly(in);
                closeQuietly(out);
            }
        } else {
            try {
                String targetPath = SDCard.makeWorkingDir(this.ctxRef.get()) + RandomUtil.getRandomString(8);
                FileUtil.copyFile(newPath, targetPath);
                recordFileReplace(entryPath, targetPath);
                Toast.makeText(this.ctxRef.get(), R.string.file_replaced, 0).show();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        notifyDataSetChanged();
    }

    public void replaceFolder(final String decodedPath, final String newPath) {
        ProcessingDialog.ProcessingInterface processing = new ProcessingDialog.ProcessingInterface() {
            @Override
            public void process() throws Exception {
                File dstFolder = new File(decodedPath);
                if (dstFolder.exists()) {
                    List<String> deletedFiles = ResListAdapter.this.doDeleteDirectory(dstFolder);
                    if (deletedFiles != null) {
                        for (String delPath : deletedFiles) {
                            ResListAdapter.this.recordFileDelete(delPath.substring(ResListAdapter.this.rootPath.length() + 1));
                        }
                    }
                    File srcFolder = new File(newPath);
                    Map<String, String> added = ResListAdapter.this.copyAllFiles(srcFolder, dstFolder, decodedPath.substring(ResListAdapter.this.rootPath.length() + 1));
                    for (Map.Entry<String, String> entry : added.entrySet()) {
                        ResListAdapter.this.recordFileAdd(entry.getKey(), entry.getValue());
                    }
                    return;
                }
                String entry2 = decodedPath.substring(ResListAdapter.this.rootPath.length() + 1);
                List<String> delEntries = ResListAdapter.this.rootNode.deleteByPath(entry2.split("/"));
                if (delEntries != null) {
                    Iterator<String> it = delEntries.iterator();
                    while (it.hasNext()) {
                        ResListAdapter.this.recordFileDelete(it.next());
                    }
                }
                String targetFolder = SDCard.makeWorkingDir((Context) ResListAdapter.this.ctxRef.get()) + RandomUtil.getRandomString(6);
                Map<String, String> added2 = ResListAdapter.this.copyAllFiles(new File(newPath), new File(targetFolder), entry2);
                for (Map.Entry<String, String> e : added2.entrySet()) {
                    ResListAdapter.this.rootNode.addChildByPath(e.getKey().split("/"), true);
                    ResListAdapter.this.recordFileAdd(e.getKey(), e.getValue());
                }
            }

            @Override
            public void afterProcess() {
            }
        };
        new ProcessingDialog((Activity) this.ctxRef.get(), processing, R.string.folder_replaced).show();
    }

    public Map<String, String> copyAllFiles(File srcFolder, File dstFolder, String entryName) throws IOException {
        if (!dstFolder.exists()) {
            dstFolder.mkdirs();
        }
        Map<String, String> addedEntries = new HashMap<>();
        File[] files = srcFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if (f.isFile()) {
                    File dstFile = new File(dstFolder, f.getName());
                    FileUtil.copyFile(f, dstFile);
                    String filepath = dstFile.getPath();
                    addedEntries.put(entryName + "/" + name, filepath);
                } else {
                    Map<String, String> added = copyAllFiles(f, new File(dstFolder, f.getName()), entryName + "/" + name);
                    addedEntries.putAll(added);
                }
            }
        }
        return addedEntries;
    }

    protected void dumpChangedFiles() {
    }

    public void recordFileAdd(String entryName, String filepath) {
        if (this.allFileDeleted.contains(entryName)) {
            this.allFileDeleted.remove(entryName);
            this.allFileReplaced.put(entryName, filepath);
        } else if (this.allFileReplaced.containsKey(entryName)) {
            Log.d("DEBUG", "error: " + entryName + " already exist?");
        } else {
            this.allFileAdded.put(entryName, filepath);
        }
    }

    public void recordFileDelete(String entryName) {
        if (this.allFileAdded.containsKey(entryName)) {
            this.allFileAdded.remove(entryName);
        } else if (this.allFileReplaced.containsKey(entryName)) {
            this.allFileReplaced.remove(entryName);
            this.allFileDeleted.add(entryName);
        } else {
            this.allFileDeleted.add(entryName);
        }
    }

    private void recordFileReplace(String entryName, String filepath) {
        if (this.allFileAdded.containsKey(entryName)) {
            this.allFileAdded.put(entryName, filepath);
        } else if (this.allFileDeleted.contains(entryName)) {
            Log.d("DEBUG", "error: " + entryName + " is already deleted?");
        } else {
            this.allFileReplaced.put(entryName, filepath);
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

    public List<String> doDeleteDirectory(File dir) {
        List<String> deletedFiles = new ArrayList<>();
        File[] subFiles = dir.listFiles();
        if (subFiles != null) {
            for (File subF : subFiles) {
                if (subF.isFile()) {
                    subF.delete();
                    deletedFiles.add(subF.getPath());
                } else {
                    deletedFiles.addAll(doDeleteDirectory(subF));
                }
            }
        }
        dir.delete();
        return deletedFiles;
    }

    private List<String> doDeleteFileOrDir(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
                List<String> deletedFiles = new ArrayList<>();
                deletedFiles.add(path);
                return deletedFiles;
            }
            return doDeleteDirectory(file);
        }
        return null;
    }

    protected void deleteFile(List<Integer> positions) {
        List<FileRecord> records = new ArrayList<>();
        String dirPath = getData(records);
        List<Integer> deletedPositions = new ArrayList<>();
        Iterator<Integer> it = positions.iterator();
        while (it.hasNext()) {
            int position = it.next().intValue();
            FileRecord fileRec = records.get(position);
            if (fileRec != null) {
                deleteFile(dirPath, fileRec);
                deletedPositions.add(Integer.valueOf(position));
            }
        }
        dumpChangedFiles();
        listItemsDeleted(deletedPositions);
    }

    private boolean deleteFile(String dirPath, FileRecord fileRec) {
        return deleteFile(dirPath, fileRec.fileName, fileRec.isInZip);
    }

    public boolean deleteFile(String dirPath, String fileName, boolean bInZip) {
        boolean ret;
        String entryPath;
        List<String> deletedEntries = null;
        if (bInZip) {
            if (!dirPath.equals(this.rootPath)) {
                entryPath = dirPath.substring(this.rootPath.length() + 1) + "/" + fileName;
            } else {
                entryPath = fileName;
            }
            deletedEntries = this.rootNode.deleteByPath(entryPath.split("/"));
            ret = true;
        } else {
            String path = dirPath + "/" + fileName;
            List<String> deletedFiles = doDeleteFileOrDir(path);
            if (deletedFiles != null) {
                deletedEntries = new ArrayList();
                for (String delPath : deletedFiles) {
                    deletedEntries.add(delPath.substring(this.rootPath.length() + 1));
                }
            }
            ret = deletedFiles != null;
        }
        if (deletedEntries != null) {
            for (String delEntry : deletedEntries) {
                recordFileDelete(delEntry);
            }
        }
        return ret;
    }

    public String getReplacedFilePath(String entryName) {
        String filepath = this.allFileReplaced.get(entryName);
        if (filepath == null) {
            return this.allFileAdded.get(entryName);
        }
        return filepath;
    }

    public Map<String, String> getAddedFiles() {
        return this.allFileAdded;
    }

    public Map<String, String> getReplacedFiles() {
        return this.allFileReplaced;
    }

    public Set<String> getDeletedFiles() {
        return this.allFileDeleted;
    }

    public void setModification(Map<String, String> res_addedFiles, Set<String> res_deletedFiles, Map<String, String> res_replacedFiles) {
        if (res_addedFiles != null) {
            this.allFileAdded = res_addedFiles;
        }
        if (res_deletedFiles != null) {
            this.allFileDeleted = res_deletedFiles;
        }
        if (res_replacedFiles != null) {
            this.allFileReplaced = res_replacedFiles;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        int id = view.getId();
        if (isChecked) {
            this.checkedItems.add(Integer.valueOf(id));
        } else {
            this.checkedItems.remove(Integer.valueOf(id));
        }
        if (this.listenerRef != null) {
            this.listenerRef.get().selectionChanged(this.checkedItems);
        }
    }

    public Set<Integer> getCheckedItems() {
        return this.checkedItems;
    }

    public void reverseCheckStatus(int position) {
        if (position == 0 && !this.fileList.isEmpty() && "..".equals(this.fileList.get(0).fileName)) {
            return;
        }
        if (this.checkedItems.contains(Integer.valueOf(position))) {
            this.checkedItems.remove(Integer.valueOf(position));
        } else {
            this.checkedItems.add(Integer.valueOf(position));
        }
        notifyDataSetChanged();
    }

    public void checkAllItems(boolean checkAll) {
        if (checkAll) {
            FileRecord rec = this.fileList.get(0);
            if (!rec.fileName.equals("..")) {
                this.checkedItems.add(0);
            }
            for (int i = 1; i < this.fileList.size(); i++) {
                this.checkedItems.add(Integer.valueOf(i));
            }
        } else {
            this.checkedItems.clear();
            if (this.listenerRef.get() != null) {
                this.listenerRef.get().selectionChanged(this.checkedItems);
            }
        }
        notifyDataSetChanged();
    }

    public boolean isFolderExist(String path) {
        File f = new File(path);
        if (f.exists() || this.rootPath.equals(path)) {
            return true;
        }
        String relativePath = path.substring(this.rootPath.length() + 1);
        ZipNode node = this.rootNode.findFolderByPath(relativePath.split("/"));
        return node != null;
    }

    public boolean isInRootDir() {
        return this.curPath.equals(this.rootPath);
    }

    public void gotoRootDir() {
        openDirectory(this.rootPath);
    }

    public void renamePathAsFolderRename(String fromPath, String toDir) {
        if (this.allFileAdded != null) {
            for (Map.Entry<String, String> entry : this.allFileAdded.entrySet()) {
                if (entry.getValue().startsWith(fromPath)) {
                    entry.setValue(toDir + entry.getValue().substring(fromPath.length()));
                }
            }
        }
        if (this.allFileReplaced != null) {
            for (Map.Entry<String, String> entry2 : this.allFileReplaced.entrySet()) {
                if (entry2.getValue().startsWith(fromPath)) {
                    entry2.setValue(toDir + entry2.getValue().substring(fromPath.length()));
                }
            }
        }
    }

    static class BitmapInfo {
        Bitmap bitmap;
        int height;
        int width;

        public BitmapInfo(Bitmap bitmap2, int width2, int height2) {
            this.bitmap = bitmap2;
            this.width = width2;
            this.height = height2;
        }
    }

    public static class ZipNode {
        List<ZipNode> children;
        boolean isFile;
        String name;

        public ZipNode() {
            this.name = "";
            this.isFile = false;
        }

        public ZipNode(String name, boolean isFile) {
            this.name = name;
            this.isFile = isFile;
        }

        public void addChildByPath(String[] paths, boolean isFile) {
            ZipNode parentNode = this;
            for (int i = 0; i < paths.length - 1; i++) {
                ZipNode childNode = parentNode.findFolder(paths[i], true);
                if (childNode == null) {
                    childNode = new ZipNode(paths[i], false);
                    parentNode.addChild(childNode);
                }
                parentNode = childNode;
            }
            ZipNode leaf = new ZipNode(paths[paths.length - 1], isFile);
            parentNode.addChild(leaf);
        }

        public ZipNode findFolderByPath(String[] paths) {
            ZipNode parentNode = this;
            for (String str : paths) {
                ZipNode childNode = parentNode.findFolder(str, false);
                if (childNode == null) {
                    return null;
                }
                parentNode = childNode;
            }
            return parentNode;
        }

        public ZipNode findNodeByPath(String[] paths) {
            ZipNode parentNode = this;
            for (int i = 0; i < paths.length - 1; i++) {
                ZipNode childNode = parentNode.findFolder(paths[i], false);
                if (childNode == null) {
                    return null;
                }
                parentNode = childNode;
            }
            int i2 = paths.length;
            return parentNode.findChildByName(paths[i2 - 1]);
        }

        private void addChild(ZipNode childNode) {
            if (this.children == null) {
                this.children = new ArrayList();
            }
            this.children.add(childNode);
        }

        private ZipNode findFolder(String name, boolean bCreate) {
            if (this.children == null) {
                if (!bCreate) {
                    return null;
                }
                this.children = new ArrayList();
            }
            for (ZipNode node : this.children) {
                if (!node.isFile && name.equals(node.name)) {
                    return node;
                }
            }
            return null;
        }

        private ZipNode findChildByName(String name) {
            if (this.children != null) {
                for (ZipNode node : this.children) {
                    if (name.equals(node.name)) {
                        return node;
                    }
                }
                return null;
            }
            return null;
        }

        public List<ZipNode> getChildren() {
            return this.children;
        }

        public List<String> deleteByPath(String[] paths) {
            ZipNode parentNode;
            ZipNode target;
            if (paths.length > 1) {
                String[] newPaths = new String[paths.length - 1];
                for (int i = 0; i < paths.length - 1; i++) {
                    newPaths[i] = paths[i];
                }
                parentNode = findFolderByPath(newPaths);
            } else {
                parentNode = this;
            }
            if (parentNode != null && (target = parentNode.findChildByName(paths[paths.length - 1])) != null) {
                if (target.isFile) {
                    parentNode.deleteChild(target);
                    List<String> deletedEntries = new ArrayList<>();
                    String delPath = StringUtil.join("/", paths);
                    deletedEntries.add(delPath);
                    return deletedEntries;
                }
                List<String> deletedEntries2 = target.enumFiles();
                if (deletedEntries2 != null) {
                    String delRootPath = StringUtil.join("/", paths);
                    for (int i2 = 0; i2 < deletedEntries2.size(); i2++) {
                        deletedEntries2.set(i2, delRootPath + "/" + deletedEntries2.get(i2));
                    }
                    parentNode.deleteChild(target);
                    return deletedEntries2;
                }
                parentNode.deleteChild(target);
                return null;
            }
            return null;
        }

        private boolean deleteChild(ZipNode _child) {
            if (this.children != null) {
                return this.children.remove(_child);
            }
            return false;
        }

        private List<String> enumFiles() {
            if (this.children != null) {
                List<String> entries = new ArrayList<>();
                for (ZipNode child : this.children) {
                    if (child.isFile) {
                        entries.add(child.name);
                    } else {
                        List<String> subEntries = child.enumFiles();
                        if (subEntries != null) {
                            for (String subEntry : subEntries) {
                                entries.add(child.name + "/" + subEntry);
                            }
                        }
                    }
                }
                return entries;
            }
            return null;
        }
    }

    private static class ViewHolder {
        CheckBox checkbox;
        TextView desc1;
        TextView filename;
        ImageView icon;

        private ViewHolder() {
        }
    }
}
