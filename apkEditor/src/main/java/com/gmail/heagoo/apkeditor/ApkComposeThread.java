package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.ce.IDescriptionUpdate;
import com.gmail.heagoo.apkeditor.smali.ISmaliAssembleCallback;
import com.gmail.heagoo.apkeditor.util.Aapt2Pipeline;
import com.gmail.heagoo.apkeditor.util.Aapt2ResourceTypes;
import com.gmail.heagoo.apkeditor.util.BundledFrameworkFiles;
import com.gmail.heagoo.apkeditor.util.SignHelper;
import com.gmail.heagoo.apkeditor.util.ToolFramework;
import com.gmail.heagoo.apkeditor.util.ZipEngine;
import com.gmail.heagoo.common.CommandRunner;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.ITaskCallback;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.TextFileReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/* loaded from: /tmp/apkx/classes5.dex */
public class ApkComposeThread extends ComposeThread implements ISmaliAssembleCallback {
    public static String sysArch;
    private String aaptPath;
    private Map<String, String> addedFiles;
    private String androidJarPath;
    private boolean bSignApk;
    private String binRootPath;
    private Context ctx;
    private String decodedFilePath;
    private Set<String> deletedFiles;
    private String errMessage;
    private IApkMaking extraMaker;
    private Map<String, String> fileEntry2ZipEntry;
    private boolean manifestModified;
    private List<String> modifiedSmaliFolders;
    private Map<String, String> replacedFiles;
    private boolean resFileModified;
    private String srcApkPath;
    private ITaskCallback.TaskStepInfo stepInfo;
    private boolean stringModified;
    private boolean succeed;
    private String targetApkPath;
    private ITaskCallback taskCallback;
    private String tempApkPath;
    private Map<String, String> dexReplaces = new HashMap();
    private long lastUpdateAssembleTime = 0;
    private boolean stopFlag = false;

    public ApkComposeThread(Context ctx, String decodedFilePath, String srcApkPath, String apkPath) {
        this.ctx = ctx;
        File fileDir = ctx.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        this.binRootPath = rootDirectory + "/bin";
        this.aaptPath = getNativeAaptPath(ctx);
        this.androidJarPath = this.binRootPath + "/android.jar";
        this.decodedFilePath = decodedFilePath;
        this.srcApkPath = srcApkPath;
        this.targetApkPath = apkPath;
        this.tempApkPath = srcApkPath;
        LOGGER.info("aaptPath: " + this.aaptPath);
        LOGGER.info("androidJarPath: " + this.androidJarPath);
        LOGGER.info("decodedFilePath: " + this.decodedFilePath);
        this.stepInfo = new ITaskCallback.TaskStepInfo();
    }

    public static String getNativeAaptPath(Context ctx) {
        return new File(ctx.getApplicationInfo().nativeLibraryDir, "libaapt_exec.so").getAbsolutePath();
    }

    public static String getInstaller(Context ctx, String pkgName) {
        String installer = null;
        try {
            StringBuffer sb2 = new StringBuffer();
            sb2.append('g');
            sb2.append('e');
            sb2.append('t');
            sb2.append("Package");
            sb2.append('M');
            sb2.append('a');
            sb2.append('n');
            sb2.append('a');
            sb2.append('g');
            sb2.append('e');
            sb2.append('r');
            Method method2 = Context.class.getMethod(sb2.toString(), new Class[0]);
            PackageManager pm = (PackageManager) method2.invoke(ctx, new Object[0]);
            StringBuffer sb = new StringBuffer();
            sb.append('g');
            sb.append('e');
            sb.append('t');
            sb.append("In");
            sb.append("stall");
            sb.append("er");
            sb.append("Package");
            sb.append('N');
            sb.append('a');
            sb.append('m');
            sb.append('e');
            Method method3 = PackageManager.class.getMethod(sb.toString(), String.class);
            installer = (String) method3.invoke(pm, pkgName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (installer == null) {
            return "";
        }
        return installer;
    }

    public static boolean isProAndNoModification(Context ctx) {
        if (!ctx.getPackageName().endsWith("pro")) {
            return false;
        }
        try {
            String packageName = ctx.getPackageName();
            String installer = getInstaller(ctx, packageName);
            StringBuffer sb = new StringBuffer();
            sb.append('i');
            sb.append('n');
            sb.append('g');
            if (!installer.endsWith(sb.toString())) {
                return false;
            }
            if (!packageName.endsWith("pro")) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected static boolean getNoVersionVectorOption(Context ctx, String aaptPath) {
        boolean option;
        String configKey = aaptPath.endsWith("libaaptz_exec.so") ? "aaptz-no-version-vectors" : "aapt-no-version-vectors";
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        int intVal = sp.getInt(configKey, -1);
        int i = 1;
        if (intVal == 1) {
            return true;
        }
        if (intVal == 0) {
            return false;
        }
        String[] command = {aaptPath};
        CommandRunner cr = new CommandRunner();
        cr.runCommand(command, (String[]) null, (String) null, 5000, false);
        String strOut = cr.getStdOut();
        String strError = cr.getStdError();
        if ((strOut != null && strOut.contains("--no-version-vectors")) || (strError != null && strError.contains("--no-version-vectors"))) {
            option = true;
        } else {
            option = false;
        }
        SharedPreferences.Editor editor = sp.edit();
        if (!option) {
            i = 0;
        }
        editor.putInt(configKey, i);
        editor.apply();
        return option;
    }

    public static boolean prepare(Context ctx, String aaptPath, String binRootPath) throws Exception {
        String curVersion = null;
        try {
            PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            curVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        SharedPreferences sp = ctx.getSharedPreferences("info", 0);
        boolean inited = sp.getBoolean("initialized", false);
        String lastVersion = sp.getString("version", "");
        if (inited && lastVersion.equals(curVersion) && new File(binRootPath, "android.jar").isFile() && new File(binRootPath, "resources.arsc").isFile() && new File(binRootPath, "AndroidManifest.xml").isFile()) {
            return true;
        }
        if (!copyFiles(ctx, aaptPath, binRootPath)) {
            return false;
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("initialized", true);
        editor.putString("version", curVersion);
        editor.commit();
        return true;
    }

    private static boolean copyFiles(Context ctx, String aaptPath, String binRootPath) throws Exception {
        try {
            File parentDir = new File(binRootPath);
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            ctx.getAssets();
            sysArch = detArchName();
            File nativeAapt = new File(aaptPath);
            if (!nativeAapt.isFile()) {
                throw new IOException("Упакованный aapt не найден: " + aaptPath);
            }
            copyAndroidjar(ctx, binRootPath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Can not copy file: " + e.getMessage());
        }
    }

    private static void copy_aapt(AssetManager assets, File outDir) throws IOException {
        File aapt = new File(outDir, "aapt");
        InputStream aapt_in = assets.open(sysArch + "/aapt");
        OutputStream aapu_out = new FileOutputStream(aapt);
        IOUtils.copy(aapt_in, aapu_out);
        aapt_in.close();
        aapu_out.close();
        aapt.setExecutable(true);
    }

    private static void copy_aapt2(AssetManager assets, File outDir) throws IOException {
        File aapt2 = new File(outDir, "aapt");
        InputStream aapt2_in = assets.open(sysArch + "/aapt");
        OutputStream aapt2_out = new FileOutputStream(aapt2);
        IOUtils.copy(aapt2_in, aapt2_out);
        aapt2_in.close();
        aapt2_out.close();
        aapt2.setExecutable(true);
    }

    private static String detArchName() {
        String arch = Build.CPU_ABI;
        if (arch.equals("arm64-v8a")) {
            return "arm64-v8a";
        }
        if (arch.equals("armeabi-v7a")) {
            return "armeabi-v7a";
        }
        if (arch.equals("x86_64")) {
            return "x86_64";
        }
        if (!arch.equals("x86")) {
            return "armeabi-v7a";
        }
        return "x86";
    }

    private static void copyAndroidjar(final Context ctx, String binRootPath) throws IOException {
        BundledFrameworkFiles.ensure(new File(binRootPath), new BundledFrameworkFiles.AssetSource() {
            public final InputStream open() throws IOException {
                return ctx.getAssets().open("android.zip");
            }
        });
    }

    @Override // com.gmail.heagoo.apkeditor.ComposeThread
    public void setModification(boolean strModified, boolean manifestModified, boolean resFileModified, List<String> modifiedSmaliFolders, Map<String, String> addedFiles, Map<String, String> replacedFiles, Set<String> deletedFiles, Map<String, String> fileEntry2ZipEntry, boolean bSignApk) {
        this.stringModified = strModified;
        this.manifestModified = manifestModified;
        this.resFileModified = resFileModified;
        this.modifiedSmaliFolders = modifiedSmaliFolders;
        this.addedFiles = addedFiles;
        this.deletedFiles = deletedFiles;
        this.replacedFiles = replacedFiles;
        this.fileEntry2ZipEntry = fileEntry2ZipEntry;
        this.bSignApk = bSignApk;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        try {
            boolean rebuildResNeeded = isResourceModified();
            this.stepInfo.stepTotal = this.bSignApk ? 2 : 1;
            if (rebuildResNeeded) {
                this.stepInfo.stepTotal += 2;
            }
            if (this.modifiedSmaliFolders != null && !this.modifiedSmaliFolders.isEmpty()) {
                this.stepInfo.stepTotal += this.modifiedSmaliFolders.size();
            }

            if (rebuildResNeeded) {
                if (isStopped()) {
                    return;
                }
                setNextStep(this.ctx.getString(R.string.compose));
                if (!prepare() || !composeResource()) {
                    return;
                }
                if (isStopped()) {
                    return;
                }
            }

            if (this.modifiedSmaliFolders != null && !this.modifiedSmaliFolders.isEmpty()) {
                for (String smaliFolder : this.modifiedSmaliFolders) {
                    if (isStopped()) {
                        return;
                    }
                    String smaliPath = this.decodedFilePath + "/" + smaliFolder;
                    String dexName = getDexNameBySmaliFolder(smaliFolder);
                    setNextStep(this.ctx.getString(R.string.assemble_dex_file) + ": " + dexName);
                    try {
                        assembleSmali(smaliPath, dexName);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        this.errMessage = this.ctx.getString(R.string.assemble_dex_file)
                                + ": " + describe(e);
                        return;
                    }
                    if (isStopped()) {
                        return;
                    }
                }
            }

            if (rebuildResNeeded) {
                if (isStopped()) {
                    return;
                }
                setNextStep(this.ctx.getString(R.string.merge));
                mergeApk();
                removeResChanges();
            } else {
                translate2OriginEntry();
            }
            if (isStopped()) {
                return;
            }
            if (this.bSignApk) {
                setNextStep(this.ctx.getString(R.string.sign));
                if (!signApk()) {
                    return;
                }
            } else if (!notSignApk()) {
                this.errMessage = this.ctx.getString(R.string.merge);
                return;
            }
            if (isStopped()) {
                return;
            }
            setNextStep(this.ctx.getString(R.string.cleanup));
            cleanup();
            this.succeed = true;
        } catch (Throwable t) {
            t.printStackTrace();
            this.succeed = false;
            this.errMessage = describe(t);
        } finally {
            finishTask();
        }
    }

    /** True when the user asked to stop; records the reason for the dialog. */
    private boolean isStopped() {
        if (!this.stopFlag) {
            return false;
        }
        this.errMessage = "User request to stop";
        return true;
    }

    /**
     * Reports the outcome exactly once. The decompiled method ended in an empty
     * {@code if (!stopFlag) {}} block, so the build screen never left its first
     * step. See the matching comment in ApkComposeThreadNew.
     */
    private void finishTask() {
        ITaskCallback callback = this.taskCallback;
        if (callback == null) {
            return;
        }
        if (this.succeed) {
            callback.taskSucceed();
            return;
        }
        String message = this.errMessage;
        if (message == null || message.trim().length() == 0) {
            message = this.ctx.getString(R.string.lab_build_no_reason);
        }
        callback.taskFailed(message);
    }

    /** getMessage() is null for many runtime exceptions; never report a bare null. */
    private static String describe(Throwable t) {
        if (t == null) {
            return "unknown error";
        }
        String message = t.getMessage();
        return message == null || message.length() == 0
                ? t.getClass().getSimpleName() : t.getClass().getSimpleName() + ": " + message;
    }

    private void translate2OriginEntry() {
        if (this.fileEntry2ZipEntry == null || this.fileEntry2ZipEntry.isEmpty()) {
            return;
        }
        if (!this.addedFiles.isEmpty()) {
            Map<String, String> newAdded = new HashMap<>();
            for (Map.Entry<String, String> entry : this.addedFiles.entrySet()) {
                String newKey = this.fileEntry2ZipEntry.get(entry.getKey());
                if (newKey != null) {
                    newAdded.put(newKey, entry.getValue());
                } else {
                    newAdded.put(entry.getKey(), entry.getValue());
                }
            }
            this.addedFiles = newAdded;
        }
        if (!this.replacedFiles.isEmpty()) {
            Map<String, String> newReplace = new HashMap<>();
            for (Map.Entry<String, String> entry2 : this.replacedFiles.entrySet()) {
                String newKey2 = this.fileEntry2ZipEntry.get(entry2.getKey());
                if (newKey2 != null) {
                    newReplace.put(newKey2, entry2.getValue());
                } else {
                    newReplace.put(entry2.getKey(), entry2.getValue());
                }
            }
            this.replacedFiles = newReplace;
        }
        if (!this.deletedFiles.isEmpty()) {
            Set<String> newDelete = new HashSet<>();
            for (String entry3 : this.deletedFiles) {
                String newEntry = this.fileEntry2ZipEntry.get(entry3);
                if (newEntry != null) {
                    newDelete.add(newEntry);
                } else {
                    newDelete.add(entry3);
                }
            }
            this.deletedFiles = newDelete;
        }
    }

    private void removeResChanges() {
        removeResInMap(this.addedFiles);
        removeResInMap(this.replacedFiles);
        Iterator<String> it = this.deletedFiles.iterator();
        while (it.hasNext()) {
            if (it.next().startsWith("res/")) {
                it.remove();
            }
        }
    }

    private void removeResInMap(Map<String, String> data) {
        Iterator<Map.Entry<String, String>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (entry.getKey().startsWith("res/")) {
                it.remove();
            }
        }
    }

    private String getDexNameBySmaliFolder(String smaliFolder) {
        if ("smali".equals(smaliFolder)) {
            return "classes.dex";
        }
        if (smaliFolder.startsWith("smali_")) {
            return smaliFolder.substring("smali_".length()) + ".dex";
        }
        return smaliFolder + ".dex";
    }

    private void setNextStep(String description) {
        this.stepInfo.stepIndex++;
        this.stepInfo.stepDescription = description;
        this.taskCallback.setTaskStepInfo(this.stepInfo);
    }

    private void assembleSmali(String smaliFilePath, String dexFileName) throws Throwable {
        String dexFilePath = getPathInSameDirectory(this.targetApkPath, dexFileName);
        try {
            long start = System.currentTimeMillis();
            Class<?> obj_class = Class.forName("com.gmail.heagoo.apkeditor.plus.DexEncoder");
            Method method = obj_class.getMethod("smali2Dex", String.class, String.class, ISmaliAssembleCallback.class);
            method.invoke(null, smaliFilePath, dexFilePath, this);
            Log.i("DEBUG", "Encode time=" + (System.currentTimeMillis() - start));
            this.dexReplaces.put(dexFileName, dexFilePath);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private String getPathInSameDirectory(String path, String name) {
        int position = path.lastIndexOf(47);
        return path.substring(0, position + 1) + name;
    }

    private void cleanup() {
        if (!this.tempApkPath.equals(this.srcApkPath)) {
            File f = new File(this.tempApkPath);
            f.delete();
        }
        try {
            ProjectCleanup.deleteAll(new File(SDCard.getEditorDirectory(), "tmp"), new BooleanSupplier() {
                @Override // java.util.function.BooleanSupplier
                public final boolean getAsBoolean() {
                    return ApkComposeThread.lambda$cleanup$1();
                }
            });
        } catch (Exception e) {
        }
    }

    static boolean lambda$cleanup$1() {
        return false;
    }

    protected void mergeApk() throws IOException {
        Map<String, String> extraReplaces = new HashMap<>();
        if (this.extraMaker != null) {
            try {
                this.extraMaker.prepareReplaces(this.ctx, this.tempApkPath, extraReplaces, new IDescriptionUpdate() {
                    public void updateDescription(String strDesc) {
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        StringBuilder sb1 = new StringBuilder();
        int mappingLen = 0;
        for (Map.Entry<String, String> entry : this.fileEntry2ZipEntry.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb1.append(key);
            sb1.append('\n');
            sb1.append(value);
            sb1.append('\n');
            mappingLen += key.getBytes().length + value.getBytes().length + 2;
        }
        int replaceLen = 0;
        StringBuilder sb2 = new StringBuilder();
        for (Map.Entry<String, String> entry2 : this.addedFiles.entrySet()) {
            String key2 = entry2.getKey();
            String value2 = entry2.getValue();
            if (!key2.startsWith("res/") || !key2.endsWith(".xml")) {
                sb2.append(key2);
                sb2.append('\n');
                sb2.append(value2);
                sb2.append('\n');
                replaceLen += key2.getBytes().length + value2.getBytes().length + 2;
            }
        }
        for (Map.Entry<String, String> entry3 : this.replacedFiles.entrySet()) {
            String key3 = entry3.getKey();
            String value3 = entry3.getValue();
            if (!key3.startsWith("res/") || (!key3.endsWith(".xml") && !key3.endsWith(".9.png"))) {
                sb2.append(key3);
                sb2.append('\n');
                sb2.append(value3);
                sb2.append('\n');
                replaceLen += key3.getBytes().length + value3.getBytes().length + 2;
            }
        }
        int replaceLen2 = replaceLen;
        for (Map.Entry<String, String> entry4 : extraReplaces.entrySet()) {
            String key4 = entry4.getKey();
            String value4 = entry4.getValue();
            sb2.append(entry4.getKey());
            sb2.append('\n');
            sb2.append(entry4.getValue());
            sb2.append('\n');
            replaceLen2 += key4.getBytes().length + value4.getBytes().length + 2;
        }
        ZipEngine.merge(this.ctx, this.tempApkPath, this.srcApkPath, sb2.toString(), replaceLen2, sb1.toString(), mappingLen);
    }

    private boolean isResourceModified() {
        return this.stringModified || this.manifestModified || this.resFileModified;
    }

    private boolean signApk() {
        this.replacedFiles.putAll(this.dexReplaces);
        try {
            SignHelper.sign(this.ctx, this.tempApkPath, this.targetApkPath, this.replacedFiles, this.addedFiles, this.deletedFiles);
            return true;
        } catch (Exception e) {
            String strHeader = this.ctx.getResources().getString(R.string.sign_error);
            this.errMessage = strHeader + e.getMessage();
            return false;
        }
    }

    private boolean notSignApk() throws IOException {
        this.replacedFiles.putAll(this.dexReplaces);
        StringBuilder sb1 = new StringBuilder();
        int addLen = 0;
        for (Map.Entry<String, String> entry : this.addedFiles.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb1.append(key);
            sb1.append('\n');
            sb1.append(value);
            sb1.append('\n');
            addLen += key.getBytes().length + value.getBytes().length + 2;
        }
        StringBuilder sb2 = new StringBuilder();
        int deleteLen = 0;
        for (String entry2 : this.deletedFiles) {
            sb2.append(entry2);
            sb2.append('\n');
            deleteLen += entry2.getBytes().length + 1;
        }
        StringBuilder sb3 = new StringBuilder();
        int replaceLen = 0;
        for (Map.Entry<String, String> entry3 : this.replacedFiles.entrySet()) {
            String key2 = entry3.getKey();
            String value2 = entry3.getValue();
            sb3.append(key2);
            sb3.append('\n');
            sb3.append(value2);
            sb3.append('\n');
            replaceLen += key2.getBytes().length + value2.getBytes().length + 2;
        }
        ZipEngine.modify(this.ctx, this.targetApkPath, this.tempApkPath, sb1.toString(), addLen, sb2.toString(), deleteLen, sb3.toString(), replaceLen);
        return true;
    }

    private boolean composeResource() {
        this.tempApkPath = this.targetApkPath + ".in";
        try {
            String selectedFramework = ToolFramework.selected(this.ctx, this.androidJarPath);
            String aaptChoice = PreferenceManager.getDefaultSharedPreferences(this.ctx).getString("AaptVersion", "legacy");
            boolean wantsAapt2 = "aapt2".equals(aaptChoice) || "aapt2-37".equals(aaptChoice);
            if (wantsAapt2) {
                Set<String> unsupported = Aapt2ResourceTypes.unsupportedIn(
                        new File(this.decodedFilePath + "/res"));
                if (!unsupported.isEmpty()) {
                    warnAapt2Fallback(Aapt2ResourceTypes.describe(unsupported));
                    wantsAapt2 = false;
                }
            }
            if (wantsAapt2) {
                try {
                    boolean newest = "aapt2-37".equals(aaptChoice);
                    if (Build.VERSION.SDK_INT < (newest ? 30 : 28)) {
                        throw new IOException(newest ? "AAPT2 37 requires Android 11 or newer" : "AAPT2 requires Android 9 or newer; select the original AAPT on Android 8.");
                    }
                    String aapt2 = new File(this.ctx.getApplicationInfo().nativeLibraryDir, newest ? "libaapt2_37.so" : "libaapt2_exec.so").getAbsolutePath();
                    Aapt2Pipeline.build(aapt2, selectedFramework, this.decodedFilePath + "/res", this.decodedFilePath + "/AndroidManifest.xml", this.tempApkPath);
                    return true;
                } catch (Exception e) {
                    // A renamed type that only ever held values has no directory
                    // to spot, so the compiler is the one that finds it. Fall
                    // back to the original AAPT rather than fail the build.
                    if (!Aapt2ResourceTypes.isUnknownTypeFailure(e.getMessage())) {
                        this.errMessage = e.getMessage();
                        return false;
                    }
                    warnAapt2Fallback(null);
                }
            }
            String selectedAapt = this.aaptPath;
            if ("aaptz-2022".equals(aaptChoice)) {
                File z = new File(this.ctx.getApplicationInfo().nativeLibraryDir, "libaaptz_exec.so");
                if (!z.isFile()) {
                    this.errMessage = "AAPTz доступен только в ARM32-сборке приложения. Выберите другой AAPT.";
                    return false;
                }
                selectedAapt = z.getAbsolutePath();
            }
            boolean noVersionVectorOption = getNoVersionVectorOption(this.ctx, selectedAapt);
            String[] command1 = {selectedAapt, "package", "-f", "-I", selectedFramework, "-S", this.decodedFilePath + "/res", "-M", this.decodedFilePath + "/AndroidManifest.xml", "-F", this.tempApkPath};
            String[] command2 = {selectedAapt, "package", "-f", "-I", selectedFramework, "-S", this.decodedFilePath + "/res", "-M", this.decodedFilePath + "/AndroidManifest.xml", "-F", this.tempApkPath, "--no-version-vectors"};
            CommandRunner cr = new CommandRunner();
            boolean ret = cr.runCommand(noVersionVectorOption ? command2 : command1, (String[]) null, (String) null, 300000, true);
            LOGGER.info("stdout: " + cr.getStdOut() + ", ret=" + ret);
            if (ret) {
                return true;
            }
            LOGGER.info("stderr: " + cr.getStdError());
            this.errMessage = cr.getStdError();
            return false;
        } catch (Exception e) {
            this.errMessage = e.getMessage();
            return false;
        }
    }

    /**
     * Tells the user the build is continuing with the original AAPT.
     *
     * @param types offending type names, or null when the compiler found them
     */
    private void warnAapt2Fallback(String types) {
        String message = types == null
                ? this.ctx.getString(R.string.aapt2_fallback_generic)
                : this.ctx.getString(R.string.aapt2_fallback_types, types);
        LOGGER.info(message, true);
        if (this.taskCallback != null) {
            this.taskCallback.taskWarning(message);
        }
    }

    public boolean prepare() {
        try {
            return prepare(this.ctx, this.aaptPath, this.binRootPath);
        } catch (Exception e) {
            this.errMessage = e.getMessage();
            return false;
        }
    }

    private boolean isArm64() {
        int i = Build.VERSION.SDK_INT;
        try {
            TextFileReader reader = new TextFileReader("/proc/cpuinfo");
            String content = reader.getContents();
            if (!content.contains("AArch64")) {
                if (!content.contains("aarch64")) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override // com.gmail.heagoo.apkeditor.ComposeThread
    public void setTaskCallback(ITaskCallback taskCallback) {
        this.taskCallback = taskCallback;
    }

    public void updateAssembledFiles(int assembledFiles, int totalFiles) {
        long curTime = System.currentTimeMillis();
        if (curTime > this.lastUpdateAssembleTime + 500) {
            String fmt = this.ctx.getString(R.string.assemble_dex_detail);
            this.stepInfo.stepDescription = String.format(fmt, Integer.valueOf(assembledFiles), Integer.valueOf(totalFiles));
            this.taskCallback.setTaskStepInfo(this.stepInfo);
        }
    }

    @Override // com.gmail.heagoo.apkeditor.ComposeThread
    public void stopRunning() {
        this.stopFlag = true;
        interrupt();
    }

    @Override // com.gmail.heagoo.apkeditor.ComposeThread
    public void setExtraMaker(IApkMaking extraMaker) {
        this.extraMaker = extraMaker;
    }

    public String getErrMessage() {
        return this.errMessage;
    }
}
