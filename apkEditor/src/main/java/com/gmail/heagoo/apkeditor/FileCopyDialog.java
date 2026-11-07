package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.ZipUtil;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileCopyDialog extends Dialog implements View.OnClickListener {
    private String apkPath;
    private List<CopySource> copySources;
    private String decodeRootPath;
    private Map<String, String> entryMapping;
    private String failedStr;
    private int fileRenameOption;
    private MyHandler handler;
    private String savedFilePath;
    private String succeedStr;
    private String targetFolder;
    private View view;

    public static class CopySource {
        public boolean isDir;
        public boolean isInApk;
        public String path;
    }

    public FileCopyDialog(Context context, String filePath, String targetFolder, String apkPath, String decodeRootPath, Map<String, String> entryMapping, int unused) {
        super(context);
        this.handler = new MyHandler(this);
        requestWindowFeature(1);
        CopySource source = new CopySource();
        source.path = filePath;
        source.isInApk = false;
        source.isDir = new File(filePath).isDirectory();
        this.copySources = new ArrayList();
        this.copySources.add(source);
        this.apkPath = apkPath;
        this.decodeRootPath = decodeRootPath;
        this.entryMapping = entryMapping;
        if (targetFolder != null) {
            this.targetFolder = targetFolder;
        } else {
            this.targetFolder = SDCard.getEditorDirectory();
        }
        init(context);
    }

    public FileCopyDialog(Context context, String apkPath, String decodeRootPath, Map<String, String> fileEntry2ZipEntry, List<CopySource> copySources, String targetFolder) {
        super(context);
        this.handler = new MyHandler(this);
        requestWindowFeature(1);
        this.apkPath = apkPath;
        this.decodeRootPath = decodeRootPath;
        this.entryMapping = fileEntry2ZipEntry;
        this.copySources = copySources;
        this.targetFolder = targetFolder;
        init(context);
    }

    public static String getName(String path) {
        int pos = path.lastIndexOf(47);
        return path.substring(pos + 1);
    }

    public static File getTargetNonExistFile(String path, boolean isDir) {
        int index = 1;
        String folder = null;
        String name = null;
        String fileType = null;
        if (!isDir) {
            int slashPos = path.lastIndexOf(47);
            folder = path.substring(0, slashPos + 1);
            String filename = path.substring(slashPos + 1);
            name = filename;
            fileType = "";
            int dotPos = filename.lastIndexOf(46);
            if (dotPos != -1) {
                name = filename.substring(0, dotPos);
                fileType = filename.substring(dotPos);
            }
        }
        while (true) {
            String testingPath = isDir ? path + "(" + index + ")" : folder + name + "(" + index + ")" + fileType;
            File node = new File(testingPath);
            if (!node.exists()) {
                return node;
            }
            index++;
        }
    }

    private void init(Context context) {
        this.fileRenameOption = SettingActivity.getFileRenameOption(context);
        Resources res = context.getResources();
        this.succeedStr = res.getString(R.string.save_succeed_1);
        this.failedStr = res.getString(R.string.failed_1);
        int layoutId = R.layout.dlg_extractres;
        this.view = LayoutInflater.from(context).inflate(layoutId, (ViewGroup) null);
        setContentView(this.view);
        Button closeBtn = (Button) this.view.findViewById(R.id.close_button);
        closeBtn.setOnClickListener(this);
    }

    public void succeed() {
        TextView resultTv = (TextView) this.view.findViewById(R.id.result_tv);
        if (this.copySources.size() == 1) {
            resultTv.setText(String.format(this.succeedStr, this.savedFilePath));
        } else {
            resultTv.setText(String.format(this.succeedStr, this.targetFolder));
        }
        this.view.findViewById(R.id.layout_done).setVisibility(0);
    }

    public void failed(String msg) {
        TextView resultTv = (TextView) this.view.findViewById(R.id.result_tv);
        resultTv.setText(String.format(this.failedStr, msg));
        this.view.findViewById(R.id.layout_done).setVisibility(0);
    }

    @Override
    public void show() {
        new Thread() {
            @Override
            public void run() {
                try {
                    for (CopySource source : FileCopyDialog.this.copySources) {
                        if (!source.isInApk) {
                            FileCopyDialog.this.copyFiles(source);
                        } else {
                            FileCopyDialog.this.extractFiles(source);
                        }
                    }
                    FileCopyDialog.this.handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    FileCopyDialog.this.handler.setErrorMessage(e.getMessage());
                    FileCopyDialog.this.handler.sendEmptyMessage(1);
                }
            }
        }.start();
        super.show();
    }

    protected void extractFiles(CopySource source) throws Exception {
        String targetPath = this.targetFolder + "/" + getName(source.path);
        boolean bExist = new File(targetPath).exists();
        if (bExist && this.fileRenameOption == 0) {
            targetPath = getTargetNonExistPath(targetPath, source.isDir);
        }
        if (source.isDir) {
            ZipUtil.unzipDirectory(this.apkPath, source.path, targetPath);
        } else {
            ZipUtil.unzipFileTo(this.apkPath, source.path, targetPath);
        }
        this.savedFilePath = targetPath;
    }

    protected void copyFiles(CopySource source) throws Exception {
        String targetPath = this.targetFolder + "/" + getName(source.path);
        File srcFile = new File(source.path);
        if (srcFile.isDirectory()) {
            File outputDirFile = new File(targetPath);
            if (outputDirFile.exists()) {
                if (this.fileRenameOption == 0) {
                    outputDirFile = createDirByAddSuffix(targetPath);
                }
            } else {
                outputDirFile.mkdirs();
            }
            copyDirectory(srcFile, outputDirFile);
            this.savedFilePath = outputDirFile.getPath();
            return;
        }
        File dst = new File(targetPath);
        if (dst.exists() && this.fileRenameOption == 0) {
            dst = getTargetNonExistFile(targetPath, false);
        }
        doFileCopy(new File(source.path), dst);
        this.savedFilePath = dst.getPath();
    }

    private String getTargetNonExistPath(String path, boolean isDir) {
        return getTargetNonExistFile(path, isDir).getPath();
    }

    private File createDirByAddSuffix(String path) {
        File f = getTargetNonExistFile(path, true);
        f.mkdirs();
        return f;
    }

    private void copyDirectory(File srcDir, File dstDir) throws Exception {
        File[] files = srcDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    File t = new File(dstDir, f.getName());
                    doFileCopy(f, t);
                } else {
                    File dir = new File(dstDir, f.getName());
                    dir.mkdir();
                    copyDirectory(f, dir);
                }
            }
        }
    }

    private void doFileCopy(File from, File to) throws Exception {
        String filename = from.getName();
        if (this.apkPath != null && (filename.endsWith(".jpg") || (filename.endsWith(".png") && !filename.endsWith(".9.png")))) {
            String entryName = from.getPath().substring(this.decodeRootPath.length() + 1);
            String realEntry = this.entryMapping.get(entryName);
            if (realEntry != null) {
                entryName = realEntry;
            }
            ZipUtil.unzipFileTo(this.apkPath, entryName, to.getPath());
            return;
        }
        FileUtil.copyFile(from, to);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.close_button) {
            dismiss();
        }
    }

    private static class MyHandler extends Handler {
        private WeakReference<FileCopyDialog> dlgRef;
        private String errMsg;

        public MyHandler(FileCopyDialog dlg) {
            this.dlgRef = new WeakReference<>(dlg);
        }

        public void setErrorMessage(String msg) {
            this.errMsg = msg;
        }

        @Override
        public void handleMessage(Message msg) {
            FileCopyDialog dlg = this.dlgRef.get();
            if (dlg == null) {
            }
            switch (msg.what) {
                case 0:
                    dlg.succeed();
                    break;
                case 1:
                    dlg.failed(this.errMsg);
                    break;
            }
        }
    }
}
