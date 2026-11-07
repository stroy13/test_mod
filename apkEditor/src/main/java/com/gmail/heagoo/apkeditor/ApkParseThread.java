package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.decoder.ARSCDecoder;
import brut.androlib.res.util.ExtFile;
import brut.directory.Directory;
import com.gmail.heagoo.apkeditor.ApkDecoderMine;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.smali.ResSmaliIdProvider;
import com.gmail.heagoo.apklib.ManifestInfoCollector;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.neweditor.Document;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkParseThread extends Thread {
    private WeakReference<Activity> activityRef;
    private ResPackage apkPackage;
    private String apkPath;
    private volatile boolean cancelled;
    private WeakReference<ApkParseConsumer> consumerRef;
    private String decodeRootPath;
    private volatile ApkDecoderMine decoder;
    private String errMessage;
    private ResSmaliIdProvider idProvider;
    private boolean isFullDecoding;
    private boolean mApkProtected;
    private ResTable resTable;
    private long stageStartNanos;
    public static boolean sKeepBroken = false;
    public static boolean isProAndNoModification = true;
    private static final Object DECODE_LOCK = new Object();
    private final long[] stageNanos = new long[11];
    private int timingStage = -1;
    // Watchdog: if the decode makes no progress at all for this long, the UI is
    // told about the stall instead of waiting forever on "Processing manifest
    // and resources..." (the last label that was posted before the hang).
    private static final long STALL_TIMEOUT_MS = 120000L;
    private volatile long lastProgressNanos = System.nanoTime();
    private volatile int lastPhase = -1;
    private volatile boolean finished;
    private volatile boolean failureReported;

    private synchronized void recordStage(int phase) {
        long now = System.nanoTime();
        if (this.timingStage >= 0 && this.timingStage < this.stageNanos.length) {
            long[] jArr = this.stageNanos;
            int i = this.timingStage;
            jArr[i] = jArr[i] + (now - this.stageStartNanos);
        }
        this.timingStage = phase;
        this.stageStartNanos = now;
    }

    public synchronized String getStageTimings() {
        StringBuilder result;
        String[] names = {"waiting", "app resource table", "manifest", "resource values", "other files", "Android framework", "resource indexing", "resource files", "old project cleanup", "bundled tools", "string indexing"};
        long now = System.nanoTime();
        result = new StringBuilder("Stage time (ms):");
        int i = 0;
        while (i < names.length) {
            long n = this.stageNanos[i] + (i == this.timingStage ? now - this.stageStartNanos : 0L);
            if (n > 0) {
                result.append("\n  ").append(names[i]).append(": ").append(n / 1000000);
            }
            i++;
        }
        ApkDecoderMine current = this.decoder;
        if (current != null) {
            result.append(Document.LINE_SEPARATOR_UNIX).append(current.getWorkerStats());
        }
        ResTable table = this.resTable;
        if (table != null) {
            result.append(Document.LINE_SEPARATOR_UNIX).append(table.getFrameworkLoadInfo());
        }
        return result.toString();
    }

    void indexStringsStarted() {
        progress(10);
    }

    public void progress(int phase) {
        recordStage(phase);
        this.lastPhase = phase;
        this.lastProgressNanos = System.nanoTime();
        ApkParseConsumer consumer = this.consumerRef.get();
        if (this.cancelled || consumer == null) {
            return;
        }
        consumer.decodeProgress(phase);
    }

    public void progressCount(int done, int total) {
        this.lastProgressNanos = System.nanoTime();
        ApkParseConsumer consumer = this.consumerRef.get();
        if (this.cancelled || consumer == null) {
            return;
        }
        consumer.decodeProgressCount(done, total);
    }

    /**
     * Reports a decode failure to the UI exactly once. Without this a thread that
     * dies with an {@link Error} (or an early return) left the loading screen
     * spinning on the last progress label forever.
     */
    private void reportFailure(String message) {
        if (this.failureReported || this.cancelled) {
            return;
        }
        this.failureReported = true;
        if (message == null || message.length() == 0) {
            message = "Unknown error";
        }
        this.errMessage = message;
        Log.e("ApkParse", "APK decoding failed: " + message);
        ApkParseConsumer consumer = this.consumerRef.get();
        if (consumer != null) {
            consumer.decodeFailed(message);
        }
    }

    private void startWatchdog() {
        Thread watchdog = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!ApkParseThread.this.finished) {
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (ApkParseThread.this.finished || ApkParseThread.this.cancelled) {
                        return;
                    }
                    long idleMs = (System.nanoTime() - ApkParseThread.this.lastProgressNanos) / 1000000L;
                    // Phase 0 = "waiting for previous operation" (the decode lock may
                    // legitimately be busy with a big project), so be more patient there.
                    long timeoutMs = ApkParseThread.this.lastPhase == 0 ? 10 * STALL_TIMEOUT_MS : STALL_TIMEOUT_MS;
                    if (idleMs < timeoutMs) {
                        continue;
                    }
                    String message = buildStallMessage(idleMs);
                    // Notify first (reportFailure is ignored once cancelled), then
                    // ask the decoder to stop so the retry button starts clean.
                    ApkParseThread.this.reportFailure(message);
                    ApkParseThread.this.stopParse();
                    return;
                }
            }
        }, "apk-decode-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private String buildStallMessage(long idleMs) {
        long seconds = idleMs / 1000L;
        int phase = this.lastPhase;
        String message;
        Activity activity = this.activityRef.get();
        if (activity != null) {
            try {
                message = activity.getString(R.string.lab_load_stalled, (int) seconds, phase);
            } catch (Throwable t) {
                message = "APK decoding stalled: no progress for " + seconds + " s (stage " + phase + ")";
            }
        } else {
            message = "APK decoding stalled: no progress for " + seconds + " s (stage " + phase + ")";
        }
        return message + Document.LINE_SEPARATOR_UNIX + getStageTimings();
    }

    public ApkParseThread(Activity ctx, ApkParseConsumer consumer, String apkPath, String decodeRootPath, boolean isFullDecoding) {
        this.activityRef = new WeakReference<>(ctx);
        this.consumerRef = new WeakReference<>(consumer);
        this.apkPath = apkPath;
        this.decodeRootPath = decodeRootPath;
        this.isFullDecoding = isFullDecoding;
    }

    @Override
    public void run() {
        boolean success = false;
        progress(0);
        startWatchdog();
        try {
            success = decodeApk();
        } catch (Throwable t) {
            // Errors (NoClassDefFoundError, OutOfMemoryError, StackOverflowError,
            // ...) used to escape and kill this thread silently, leaving the
            // loading screen hanging on the last progress label.
            Log.e("ApkParse", "APK decoding failed", t);
            if (this.errMessage == null) {
                this.errMessage = t instanceof OutOfMemoryError
                        ? "OutOfMemoryError: insufficient memory while opening the APK"
                        : t.toString();
            }
        } finally {
            this.finished = true;
        }
        if (!success) {
            reportFailure(this.errMessage);
        }
    }

    private boolean decodeApk() {
        synchronized (DECODE_LOCK) {
            if (this.cancelled) {
                return true;
            }
            progress(9);
            Activity activity = this.activityRef.get();
            if (activity == null) {
                this.errMessage = "Activity is no longer available";
                return false;
            }
            ApkComposeThread tmp = new ApkComposeThread(activity, null, null, null);
            if (!tmp.prepare()) {
                this.errMessage = tmp.getErrMessage();
                return false;
            }
            return parse();
        }
    }

    private void readAssetFile(Context ctx, String filename, byte[] data) throws IOException {
        int curRead;
        AssetManager am = ctx.getAssets();
        InputStream input = am.open(filename);
        int readLen = input.read(data);
        while (readLen < data.length && (curRead = input.read(data, readLen, data.length - readLen)) != -1) {
            readLen += curRead;
        }
        input.close();
    }

    private boolean parse() {
        boolean z;
        boolean z2;
        try {
            Activity activity = this.activityRef.get();
            byte[] bArr = null;
            byte[] bArr2 = null;
            byte[] bArr3 = null;
            if (!this.isFullDecoding) {
                bArr = new byte[68];
                readAssetFile(activity, "dummy_png.png", bArr);
                bArr2 = new byte[667];
                readAssetFile(activity, "dummy_jpg.jpg", bArr2);
                bArr3 = new byte[97];
                readAssetFile(activity, "dummy.9.png", bArr3);
            }
            ExtFile extFile = new ExtFile(new File(this.apkPath));
            Directory directory = extFile.getDirectory();
            this.mApkProtected = !directory.containsDir("res");
            if (this.mApkProtected) {
                if (!directory.containsDir("r")) {
                    if (!directory.containsDir("R")) {
                        z = false;
                        z2 = true;
                    }
                }
                try {
                    InputStream fileInput = directory.getFileInput("AndroidManifest.xml");
                    ManifestInfoCollector manifestInfoCollector = new ManifestInfoCollector();
                    manifestInfoCollector.parse(fileInput, "manifest", "package");
                    String value = manifestInfoCollector.getValue();
                    fileInput.close();
                    InputStream fileInput2 = directory.getFileInput("classes.dex");
                    String makeWorkingDir = SDCard.makeWorkingDir(activity);
                    String str = makeWorkingDir + "classes.dex";
                    FileOutputStream fileOutputStream = new FileOutputStream(str);
                    IOUtils.copy(fileInput2, fileOutputStream);
                    fileInput2.close();
                    fileOutputStream.close();
                    z = false;
                    z2 = true;
                    try {
                        try {
                            RefInvoke.invokeStaticMethod("com.gmail.heagoo.apkeditor.pro.ResourceDecoder", "decodeResources", new Class[]{String.class, String.class}, new Object[]{str, makeWorkingDir});
                            this.idProvider = new ResSmaliIdProvider(makeWorkingDir, value);
                        } catch (Exception e) {
                        }
                    } catch (Exception e2) {
                    }
                } catch (Exception e3) {
                    z = false;
                    z2 = true;
                }
            } else {
                z = false;
                z2 = true;
            }
            boolean z3 = z2;
            try {
                progress(z3 ? 1 : 0);
                this.resTable = getResTable(extFile, z3);
                if (this.cancelled) {
                    return z;
                }
                progress(10);
                ApkParseConsumer apkParseConsumer = this.consumerRef.get();
                if (apkParseConsumer != null) {
                    apkParseConsumer.resTableDecoded(true);
                }
                if (this.cancelled) {
                    return z;
                }
                progress(5);
                this.resTable.getPackage(1);
                if (this.cancelled) {
                    return z;
                }
                this.decoder = new ApkDecoderMine(this.resTable, bArr, bArr2, bArr3);
                this.decoder.setProgressListener(new ApkDecoderMine.ProgressListener() {
                    @Override
                    public void onProgress(int phase) {
                        ApkParseThread.this.progress(phase);
                    }

                    @Override
                    public void onCount(int done, int total) {
                        ApkParseThread.this.progressCount(done, total);
                    }
                });
                if (this.cancelled) {
                    return z;
                }
                progress(8);
                if (!ProjectCleanup.deleteAll(new File(this.decodeRootPath), new BooleanSupplier() {
                    @Override
                    public final boolean getAsBoolean() {
                        return ApkParseThread.this.m109lambda$parse$0$comgmailheagooapkeditorApkParseThread();
                    }
                })) {
                    return z;
                }
                isProAndNoModification = ApkComposeThread.isProAndNoModification(this.activityRef.get());
                File file = new File(this.decodeRootPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                this.decoder.decode(extFile, file);
                if (!this.cancelled && this.consumerRef.get() != null) {
                    this.consumerRef.get().resourceDecoded(this.decoder.getFileEntry2ZipEntry());
                    return true;
                }
                return true;
            } catch (Exception e4) {
                this.errMessage = e4.toString();
                e4.printStackTrace();
                return z;
            }
        } catch (Exception e5) {
            z = false;
        }
        return z;
    }

    boolean m109lambda$parse$0$comgmailheagooapkeditorApkParseThread() {
        return this.cancelled;
    }

    private ResTable getResTable(ExtFile apkFile, boolean loadMainPkg) throws AndrolibException {
        Activity activity = this.activityRef.get();
        ResTable resTable = new ResTable(activity.getApplicationContext(), this.mApkProtected);
        if (loadMainPkg) {
            loadMainPkg(resTable, apkFile);
        }
        return resTable;
    }

    private ResPackage loadMainPkg(ResTable resTable, ExtFile apkFile) throws AndrolibException {
        LOGGER.info("Loading resource table of apk file...");
        ResPackage[] pkgs = getResPackagesFromApk(apkFile, resTable, sKeepBroken);
        if (pkgs == null) {
            return null;
        }
        switch (pkgs.length) {
            case 1:
                this.apkPackage = pkgs[0];
                break;
            case 2:
                if (pkgs[0].getName().equals("android")) {
                    LOGGER.warning("Skipping \"android\" package group");
                    this.apkPackage = pkgs[1];
                    break;
                } else if (pkgs[0].getName().equals("com.htc")) {
                    LOGGER.warning("Skipping \"htc\" package group");
                    this.apkPackage = pkgs[1];
                    break;
                }
                break;
        }
        if (this.apkPackage == null) {
            throw new AndrolibException("Arsc files with zero or multiple packages");
        }
        resTable.addPackage(this.apkPackage, true);
        LOGGER.info("Loaded.");
        return this.apkPackage;
    }

    private ResPackage[] getResPackagesFromApk(ExtFile apkFile, ResTable resTable, boolean keepBroken) throws AndrolibException {
        Exception e = null;
        ZipFile zipFile = null;
        Throwable th;
        Throwable th2;
        try {
            zipFile = new ZipFile((File) apkFile);
        } catch (IOException e2) {
            e = e2;
        }
        if (zipFile == null) {
            throw new AndrolibException("Could not read resources.arsc from file: " + apkFile, e);
        }
        try {
            try {
                ZipEntry entry = zipFile.getEntry("resources.arsc");
                if (entry != null) {
                    InputStream in = new BufferedInputStream(zipFile.getInputStream(entry), 65536);
                    try {
                        try {
                            try {
                                ResPackage[] packages = ARSCDecoder.decode(in, false, keepBroken, resTable, this.idProvider, this.mApkProtected).getPackages();
                                in.close();
                                zipFile.close();
                                return packages;
                            } catch (Throwable th3) {
                                th2 = th3;
                                try {
                                    in.close();
                                    throw th2;
                                } catch (Throwable th4) {
                                    th2.addSuppressed(th4);
                                    throw th2;
                                }
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            try {
                                zipFile.close();
                                throw th;
                            } catch (Throwable th6) {
                                th.addSuppressed(th6);
                                throw th;
                            }
                        }
                    } catch (Throwable th7) {
                        th2 = th7;
                    }
                } else {
                    zipFile.close();
                    return null;
                }
            } catch (Throwable th8) {
                th = th8;
            }
        } catch (Throwable e3) {
            throw new AndrolibException("Could not read resources.arsc from file: " + apkFile, e3);
        }
        return null;
    }

    public ResTable getResTable() {
        return this.resTable;
    }

    public String getErrMessage() {
        return this.errMessage;
    }

    public String getResourceRootPath() {
        return this.decodeRootPath + "/res";
    }

    public ResPackage getApkPackage() {
        return this.apkPackage;
    }

    public void stopParse() {
        this.cancelled = true;
        interrupt();
        if (this.decoder != null) {
            this.decoder.stopDecode();
        }
    }
}
