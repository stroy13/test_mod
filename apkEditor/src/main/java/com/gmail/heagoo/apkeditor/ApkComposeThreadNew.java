package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.ce.IDescriptionUpdate;
import com.gmail.heagoo.apkeditor.smali.ISmaliAssembleCallback;
import com.gmail.heagoo.apkeditor.util.Aapt2Pipeline;
import com.gmail.heagoo.apkeditor.util.Aapt2ResourceTypes;
import com.gmail.heagoo.apkeditor.util.SignHelper;
import com.gmail.heagoo.apkeditor.util.ComposeBuildTrace;
import com.gmail.heagoo.apkeditor.util.ModifiedTree;
import com.gmail.heagoo.apkeditor.util.ToolFramework;
import com.gmail.heagoo.apkeditor.util.ZipEngine;
import com.gmail.heagoo.common.CommandRunner;
import com.gmail.heagoo.common.ITaskCallback;
import com.gmail.heagoo.common.SDCard;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/* loaded from: /tmp/apkx/classes5.dex */
public class ApkComposeThreadNew extends ComposeThread implements ISmaliAssembleCallback {
    private String aaptPath;
    private String androidJarPath;
    private boolean bSignApk;
    private boolean manifestModified;
    private String binRootPath;
    private Context ctx;
    private String decodedFilePath;
    private String errMessage;
    private IApkMaking extraMaker;
    private String resourceApkPath;
    private ITaskCallback.TaskStepInfo stepInfo;
    private boolean succeed;
    private String targetApkPath;
    private ITaskCallback taskCallback;
    private Map<String, String> builtDexFiles = new HashMap();
    private List<String> modifiedSmaliFolders = new ArrayList();
    private final com.gmail.heagoo.apkeditor.util.ProgressThrottle assembleProgress = new com.gmail.heagoo.apkeditor.util.ProgressThrottle(100);
    private boolean stopFlag = false;
    private ComposeBuildTrace buildTrace;

    public ApkComposeThreadNew(Context ctx, String decodedFilePath, String apkPath) {
        this.ctx = ctx;
        File fileDir = ctx.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        this.binRootPath = rootDirectory + "/bin";
        this.aaptPath = ApkComposeThread.getNativeAaptPath(ctx);
        this.androidJarPath = this.binRootPath + "/android.jar";
        this.decodedFilePath = decodedFilePath;
        this.targetApkPath = apkPath;
        this.resourceApkPath = decodedFilePath + "/build/resource.apk";
        LOGGER.info("aaptPath: " + this.aaptPath);
        LOGGER.info("androidJarPath: " + this.androidJarPath);
        LOGGER.info("decodedFilePath: " + this.decodedFilePath);
        this.stepInfo = new ITaskCallback.TaskStepInfo();
    }

    @Override // com.gmail.heagoo.apkeditor.ComposeThread
    public void setModification(boolean strModified, boolean manifestModified, boolean resFileModified, List<String> modifiedSmaliFolders, Map<String, String> addedFiles, Map<String, String> replacedFiles, Set<String> deletedFiles, Map<String, String> fileEntry2ZipEntry, boolean bSignApk) {
        this.bSignApk = bSignApk;
        this.manifestModified = manifestModified;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        File buildDir = new File(this.decodedFilePath + "/build");
        if (!buildDir.exists()) {
            buildDir.mkdir();
        }
        this.buildTrace = new ComposeBuildTrace(new File(this.targetApkPath), "full");
        try {
            this.buildTrace.begin("resource change scan");
            boolean rebuildResource = this.manifestModified || needRebuildResource();
            this.buildTrace.begin("smali change scan");
            checkSmaliModification();
            this.buildTrace.end();
            this.stepInfo.stepTotal = 0;
            if (rebuildResource) {
                this.stepInfo.stepTotal++;
            }
            this.stepInfo.stepTotal++;
            if (this.modifiedSmaliFolders != null && !this.modifiedSmaliFolders.isEmpty()) {
                this.stepInfo.stepTotal += this.modifiedSmaliFolders.size();
            }
            if (this.bSignApk) {
                this.stepInfo.stepTotal++;
            }
            this.stepInfo.stepTotal++;

            if (rebuildResource) {
                if (isStopped()) {
                    return;
                }
                this.buildTrace.begin("prepare and compile resources");
                setNextStep(this.ctx.getString(R.string.compose));
                long prepareStarted = System.nanoTime();
                boolean prepared = prepare();
                if (this.buildTrace.logger() != null) this.buildTrace.logger().log("Resource tool preparation: " + (System.nanoTime() - prepareStarted) / 1000000L + " ms");
                if (!prepared) return;
                long compileStarted = System.nanoTime();
                boolean compiled = composeResource();
                if (this.buildTrace.logger() != null) this.buildTrace.logger().log("Resource compose total: " + (System.nanoTime() - compileStarted) / 1000000L + " ms");
                if (!compiled) return;
                if (isStopped()) {
                    return;
                }
            }

            this.builtDexFiles.clear();
            if (this.modifiedSmaliFolders != null && !this.modifiedSmaliFolders.isEmpty()) {
                for (String smaliFolder : this.modifiedSmaliFolders) {
                    if (isStopped()) {
                        return;
                    }
                    String smaliPath = this.decodedFilePath + "/" + smaliFolder;
                    String dexName = getDexNameBySmaliFolder(smaliFolder);
                    this.buildTrace.begin("assemble " + dexName);
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

            if (isStopped()) {
                return;
            }
            this.buildTrace.begin("merge APK");
            setNextStep(this.ctx.getString(R.string.merge));
            mergeApk();
            if (isStopped()) {
                return;
            }
            if (this.bSignApk) {
                this.buildTrace.begin("sign and verify");
                setNextStep(this.ctx.getString(R.string.sign));
                if (!signApk()) {
                    return;
                }
                if (isStopped()) {
                    return;
                }
            } else {
                this.buildTrace.begin("normalize unsigned APK");
                com.gmail.heagoo.apkeditor.util.ApkBuildPipeline.normalizeUnsigned(new File(this.targetApkPath));
            }
            this.buildTrace.begin("cleanup");
            setNextStep(this.ctx.getString(R.string.cleanup));
            cleanup();
            this.succeed = true;
        } catch (Throwable t) {
            t.printStackTrace();
            this.succeed = false;
            this.errMessage = describe(t);
        } finally {
            this.buildTrace.result(this.succeed, this.stopFlag, this.errMessage);
            this.buildTrace.close();
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
     * Reports the outcome exactly once.
     *
     * <p>The decompiled method ended in an empty {@code if (!stopFlag) {}} block:
     * the completion callback had been lost, so the build screen sat on
     * "Step 1/6" forever even though the worker had already returned. Running
     * this from {@code finally} means every exit path -- success, failure, stop,
     * or an unexpected throw -- releases the UI.
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

    private long getLastModifyTime(File file) {
        if (file.exists()) {
            return file.lastModified();
        }
        return 0L;
    }

    private void checkSmaliModification() throws IOException {
        File decodeDir;
        File decodeDir2 = new File(this.decodedFilePath);
        File[] files = decodeDir2.listFiles();
        int length = files.length;
        int i = 0;
        while (i < length) {
            File f = files[i];
            if (!f.isDirectory()) {
                decodeDir = decodeDir2;
            } else {
                String dirName = f.getName();
                if (dirName.equals("smali") || dirName.startsWith("smali_")) {
                    String dexName = getDexNameBySmaliFolder(dirName);
                    long dexTime1 = getLastModifyTime(new File(this.decodedFilePath + "/" + dexName));
                    long dexTime2 = getLastModifyTime(new File(this.decodedFilePath + "/build/" + dexName));
                    long dexTime = dexTime2 > dexTime1 ? dexTime2 : dexTime1;
                    File changed = ModifiedTree.firstNewer(f, dexTime);
                    if (this.buildTrace != null && this.buildTrace.logger() != null) {
                        this.buildTrace.logger().log("DEX change check: " + dirName
                                + "; original timestamp=" + dexTime1 + "; built timestamp=" + dexTime2
                                + (changed == null ? "; unchanged" : "; newer=" + changed.getPath()
                                + "; timestamp=" + changed.lastModified()));
                    }
                    if (changed == null) {
                        decodeDir = decodeDir2;
                    } else {
                        decodeDir = decodeDir2;
                        this.modifiedSmaliFolders.add(dirName);
                    }
                } else {
                    decodeDir = decodeDir2;
                }
            }
            i++;
            decodeDir2 = decodeDir;
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
        File decodeDir = new File(this.decodedFilePath);
        File buildDir = new File(decodeDir, "build");
        if (!buildDir.exists()) {
            buildDir.mkdir();
        }
        File dexFile = new File(buildDir, dexFileName);
        String dexFilePath = dexFile.getPath();
        try {
            long start = System.currentTimeMillis();
            Class<?> obj_class = Class.forName(com.gmail.heagoo.apkeditor.smali.SmaliToolChoice.encoderClass(this.ctx));
            if (this.buildTrace != null && this.buildTrace.logger() != null) {
                this.buildTrace.logger().log("Smali assembler: " + obj_class.getName());
            }
            Method method = obj_class.getMethod("smali2Dex", String.class, String.class, ISmaliAssembleCallback.class);
            method.invoke(null, smaliFilePath, dexFilePath, this);
            Log.i("DEBUG", "Encode time=" + (System.currentTimeMillis() - start));
            this.builtDexFiles.put(dexFileName, dexFilePath);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private void cleanup() {
        try {
            ProjectCleanup.deleteAll(new File(SDCard.getEditorDirectory(), "tmp"), new BooleanSupplier() {
                @Override // java.util.function.BooleanSupplier
                public final boolean getAsBoolean() {
                    return ApkComposeThreadNew.lambda$cleanup$0();
                }
            });
        } catch (Exception e) {
        }
    }

    static boolean lambda$cleanup$0() {
        return false;
    }

    private void mergeApk() throws IOException {
        char c;
        Map<String, String> extraReplaces = new HashMap<>();
        if (this.extraMaker != null) {
            try {
                this.extraMaker.prepareReplaces(this.ctx, this.resourceApkPath, extraReplaces, new IDescriptionUpdate() {
                    public void updateDescription(String strDesc) {
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long collectStarted = System.nanoTime();
        Map<String, String> filesToAdd = new HashMap<>();
        File assetsDir = new File(this.decodedFilePath + "/assets");
        if (assetsDir.exists()) {
            collectFiles(filesToAdd, assetsDir, "assets/");
        }
        File libDir = new File(this.decodedFilePath + "/lib");
        if (libDir.exists()) {
            collectFiles(filesToAdd, libDir, "lib/");
        }
        File libsDir = new File(this.decodedFilePath + "/libs");
        if (libsDir.exists()) {
            collectFiles(filesToAdd, libsDir, "libs/");
        }
        File kotlinDir = new File(this.decodedFilePath + "/kotlin");
        if (kotlinDir.exists()) {
            collectFiles(filesToAdd, kotlinDir, "kotlin/");
        }
        File unknownDir = new File(this.decodedFilePath + "/unknown");
        if (unknownDir.exists()) {
            collectFiles(filesToAdd, unknownDir, "");
        }
        collectDexFiles(filesToAdd);
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, String>> it = filesToAdd.entrySet().iterator();
        int addLen = 0;
        while (true) {
            c = '\n';
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<String, String> entry = it.next();
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key);
            sb.append('\n');
            sb.append(value);
            sb.append('\n');
            addLen += key.getBytes().length + value.getBytes().length + 2;
        }
        StringBuilder sb2 = new StringBuilder();
        int replaceLen = 0;
        for (Map.Entry<String, String> entry2 : extraReplaces.entrySet()) {
            String key2 = entry2.getKey();
            String value2 = entry2.getValue();
            sb2.append(key2);
            sb2.append(c);
            sb2.append(value2);
            sb2.append(c);
            replaceLen += key2.getBytes().length + value2.getBytes().length + 2;
            filesToAdd = filesToAdd;
            c = '\n';
        }
        if (this.buildTrace != null && this.buildTrace.logger() != null) {
            this.buildTrace.logger().log("Merge file collection and mapping: " + filesToAdd.size()
                    + " files; " + (System.nanoTime() - collectStarted) / 1000000L + " ms");
        }
        long zipStarted = System.nanoTime();
        ZipEngine.modify(this.ctx, this.targetApkPath, this.resourceApkPath, sb.toString(), addLen, "", 0, sb2.toString(), replaceLen);
        if (this.buildTrace != null && this.buildTrace.logger() != null) {
            this.buildTrace.logger().log("Merge ZIP engine: " + (System.nanoTime() - zipStarted) / 1000000L + " ms");
        }
    }

    private void collectDexFiles(Map<String, String> filesToAdd) {
        filesToAdd.putAll(this.builtDexFiles);
        File decodeDir = new File(this.decodedFilePath);
        File[] files = decodeDir.listFiles();
        for (File f : files) {
            String name = f.getName();
            if (f.isFile() && name.endsWith(".dex") && !this.builtDexFiles.containsKey(name)) {
                File dex = new File(this.decodedFilePath + "/build/" + name);
                if (dex.isFile() && dex.exists()) {
                    filesToAdd.put(name, dex.getPath());
                } else {
                    filesToAdd.put(name, f.getPath());
                }
            }
        }
    }

    private void collectFiles(Map<String, String> relPath2AbsPath, File dir, String relPath) {
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                relPath2AbsPath.put(relPath + f.getName(), f.getPath());
            } else if (f.isDirectory()) {
                collectFiles(relPath2AbsPath, f, relPath + f.getName() + "/");
            }
        }
    }

    private boolean needRebuildResource() throws IOException {
        File apkFile = new File(this.resourceApkPath);
        if (!apkFile.isFile() || !apkFile.exists()) {
            return true;
        }
        long apkTime = apkFile.lastModified();
        return ModifiedTree.atLeast(new File(this.decodedFilePath + "/AndroidManifest.xml"), apkTime)
                || ModifiedTree.atLeast(new File(this.decodedFilePath + "/res"), apkTime);
    }

    private boolean signApk() {
        String signedPath = this.targetApkPath + ".signed";
        try {
            Map<String, String> emptyMap = new HashMap<>();
            Set<String> emptySet = new HashSet<>();
            SignHelper.sign(this.ctx, this.targetApkPath, signedPath, emptyMap, emptyMap, emptySet,
                    null, null, this.buildTrace == null ? null : this.buildTrace.logger());
            File targetFile = new File(this.targetApkPath);
            targetFile.delete();
            if (!new File(signedPath).renameTo(targetFile)) {
                throw new IOException("Cannot move signed APK");
            }
            File signedSide = new File(signedPath + ".idsig");
            File finalSide = new File(this.targetApkPath + ".idsig");
            if (finalSide.exists() && !finalSide.delete()) {
                throw new IOException("Cannot remove old signature");
            }
            if (!signedSide.exists() || signedSide.renameTo(finalSide)) {
                return true;
            }
            throw new IOException("Cannot move v4 signature");
        } catch (Exception e) {
            String strHeader = this.ctx.getResources().getString(R.string.sign_error);
            this.errMessage = strHeader + e.getMessage();
            return false;
        }
    }

    private boolean composeResource() {
        try {
            String selectedFramework = ToolFramework.selected(this.ctx, this.androidJarPath);
            String aaptChoice = PreferenceManager.getDefaultSharedPreferences(this.ctx).getString("AaptVersion", "legacy");
            if ("aapt2".equals(aaptChoice) || "aapt2-37".equals(aaptChoice)) {
                Set<String> unsupported = Aapt2ResourceTypes.unsupportedIn(
                        new File(this.decodedFilePath + "/res"));
                if (!unsupported.isEmpty()) {
                    warnAapt2Fallback(Aapt2ResourceTypes.describe(unsupported));
                } else {
                    try {
                        boolean newest = "aapt2-37".equals(aaptChoice);
                        if (Build.VERSION.SDK_INT < (newest ? 30 : 28)) {
                            throw new IOException(newest ? "AAPT2 37 requires Android 11 or newer" : "AAPT2 requires Android 9 or newer; select the original AAPT on Android 8.");
                        }
                        String aapt2 = new File(this.ctx.getApplicationInfo().nativeLibraryDir, newest ? "libaapt2_37.so" : "libaapt2_exec.so").getAbsolutePath();
                        Aapt2Pipeline.build(aapt2, selectedFramework, this.decodedFilePath + "/res", this.decodedFilePath + "/AndroidManifest.xml", this.resourceApkPath, this.buildTrace == null ? null : this.buildTrace.logger());
                        return true;
                    } catch (Exception e) {
                        // A renamed type that only ever held values has no
                        // directory to spot, so the compiler is the one that
                        // finds it. Fall back rather than fail the build.
                        if (Aapt2ResourceTypes.isUnknownTypeFailure(e.getMessage())) {
                            warnAapt2Fallback(null);
                        } else {
                            this.errMessage = e.getMessage();
                            return false;
                        }
                    }
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
            boolean noVersionVectorOption = ApkComposeThread.getNoVersionVectorOption(this.ctx, selectedAapt);
            List<String> paramList = new ArrayList<>();
            paramList.add(selectedAapt);
            paramList.add("package");
            paramList.add("-f");
            paramList.add("-I");
            paramList.add(selectedFramework);
            paramList.add("-S");
            paramList.add(this.decodedFilePath + "/res");
            paramList.add("-M");
            paramList.add(this.decodedFilePath + "/AndroidManifest.xml");
            paramList.add("-F");
            paramList.add(this.resourceApkPath);
            if (noVersionVectorOption) {
                paramList.add("--no-version-vectors");
            }
            System.currentTimeMillis();
            CommandRunner cr = new CommandRunner();
            boolean ret = cr.runCommand(paramList.toArray(new String[paramList.size()]), (String[]) null, (String) null, 300000, true);
            LOGGER.info("stdout: " + cr.getStdOut() + ", ret=" + ret);
            if (ret) {
                return true;
            }
            LOGGER.info("stderr: " + cr.getStdError());
            this.errMessage = cr.getStdError();
            return false;
        } catch (Exception e2) {
            this.errMessage = e2.getMessage();
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

    private boolean prepare() {
        try {
            return ApkComposeThread.prepare(this.ctx, this.aaptPath, this.binRootPath);
        } catch (Exception e) {
            this.errMessage = e.getMessage();
            return false;
        }
    }

    @Override // com.gmail.heagoo.apkeditor.ComposeThread
    public void setTaskCallback(ITaskCallback taskCallback) {
        this.taskCallback = taskCallback;
    }

    public void updateAssembledFiles(int assembledFiles, int totalFiles) {
        if (this.assembleProgress.shouldUpdate(System.nanoTime() / 1000000L, assembledFiles == totalFiles)) {
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
