package com.gmail.heagoo.apkeditor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.gmail.heagoo.apkeditor.Constants;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ITaskCallback;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* loaded from: /tmp/apkx/classes5.dex */
public class ApkComposeService extends Service implements ITaskCallback {
    private Map<String, String> addedFiles;
    private ComposeThread composeThread;
    private String decodeRootPath;
    private Set<String> deletedFiles;
    private Map<String, String> fileEntry2ZipEntry;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotifyBuilder;
    private Boolean manifestModified;
    private ArrayList<String> modifiedSmaliFolders;
    private WeakReference<ITaskCallback> observer;
    private Map<String, String> replacedFiles;
    private Boolean resFileModified;
    private boolean signAPK;
    private String srcApkPath;
    private Boolean stringModified;
    private String targetApkPath;
    private ComposeResult composeResult = new ComposeResult();
    private IApkMaking extraMaker = null;
    private boolean foregroundStarted = false;
    private MyHandler handler = new MyHandler();
    private ComposeServiceBinder binder = new ComposeServiceBinder();
    private long lastUpdateTime = 0;

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            taskFailed(getString(R.string.lab_compose_service_lost));
            return 2;
        }
        resetStatus();
        synchronized (this.composeResult) {
            this.composeResult.started = true;
        }
        try {
            this.decodeRootPath = ActivityUtil.getParam(intent, "decodeRootPath");
            this.srcApkPath = ActivityUtil.getParam(intent, "srcApkPath");
            this.targetApkPath = ActivityUtil.getParam(intent, "targetApkPath");
            String str = ActivityUtil.getParam(intent, "stringModified");
            this.stringModified = Boolean.valueOf(str);
            String str2 = ActivityUtil.getParam(intent, "manifestModified");
            this.manifestModified = Boolean.valueOf(str2);
            String str3 = ActivityUtil.getParam(intent, "resFileModified");
            this.resFileModified = Boolean.valueOf(str3);
            this.modifiedSmaliFolders = ActivityUtil.getStringArray(intent, "modifiedSmaliFolders");
            this.signAPK = ActivityUtil.getBoolParam(intent, "signAPK");
            this.addedFiles = ActivityUtil.getMapParam(intent, "addedFiles");
            this.replacedFiles = ActivityUtil.getMapParam(intent, "replacedFiles");
            this.deletedFiles = new HashSet();
            List<String> delEntries = ActivityUtil.getStringArray(intent, "deletedFiles");
            for (String name : delEntries) {
                this.deletedFiles.add(name);
            }
            String passedFile = ActivityUtil.getParam(intent, "fileEntry2ZipEntry");
            if (passedFile != null) {
                this.fileEntry2ZipEntry = getMapFromFile(passedFile);
            }
            dumpModification();
            showNotification();
            startComposeThread();
        } catch (LinkageError | RuntimeException error) {
            Log.e("ApkComposeService", "Unable to start build", error);
            taskFailed(getString(R.string.lab_compose_start_failed, new Object[]{error.toString()}));
        }
        return 2;
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showNotification() {
        Intent composeIntent = new Intent(this, (Class<?>) ApkComposeActivity.class);
        composeIntent.setAction(Constants.ACTION.MAIN_ACTION);
        composeIntent.setFlags(131072);
        int i = Build.VERSION.SDK_INT;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, composeIntent, 67108864);
        String appName = getString(R.string.app_name);
        this.mNotificationManager = (NotificationManager) getSystemService("notification");
        int i2 = Build.VERSION.SDK_INT;
        NotificationChannel channel = new NotificationChannel(ApkComposeActivity.PRIMARY_NOTIF_CHANNEL, getString(R.string.app_name), 2);
        this.mNotificationManager.createNotificationChannel(channel);
        this.mNotifyBuilder = new NotificationCompat.Builder(this, ApkComposeActivity.PRIMARY_NOTIF_CHANNEL);
        this.mNotifyBuilder.setContentTitle(appName).setTicker(appName).setContentText(getString(R.string.build_ongoing)).setSmallIcon(R.drawable.ic_compose_notification).setContentIntent(pendingIntent).setOngoing(true);
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, this.mNotifyBuilder.build());
        this.foregroundStarted = true;
    }

    private void updateNotification(boolean forceShow, String title, String desc) {
        if (this.mNotificationManager != null && this.mNotifyBuilder != null) {
            this.handler.removeMessages(0);
            this.handler.setInfo(title, desc);
            if (forceShow || System.currentTimeMillis() - this.lastUpdateTime > 1000) {
                this.handler.sendEmptyMessage(0);
            } else {
                this.handler.sendEmptyMessageDelayed(0, 500L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetStatus() {
        if (this.composeThread != null && this.composeThread.isAlive()) {
            this.composeThread.stopRunning();
        }
        this.composeResult.clear();
    }

    private void dumpModification() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startComposeThread() {
        if (this.srcApkPath != null) {
            this.composeThread = new ApkComposeThread(this, this.decodeRootPath, this.srcApkPath, this.targetApkPath);
        } else {
            this.composeThread = new ApkComposeThreadNew(this, this.decodeRootPath, this.targetApkPath);
        }
        if (this.extraMaker != null) {
            this.composeThread.setExtraMaker(this.extraMaker);
        }
        this.composeThread.setModification(this.stringModified.booleanValue(), this.manifestModified.booleanValue(), this.resFileModified.booleanValue(), this.modifiedSmaliFolders, this.addedFiles, this.replacedFiles, this.deletedFiles, this.fileEntry2ZipEntry, this.signAPK);
        this.composeThread.setTaskCallback(this);
        this.composeThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() { // from class: com.gmail.heagoo.apkeditor.ApkComposeService$$ExternalSyntheticLambda0
            @Override // java.lang.Thread.UncaughtExceptionHandler
            public final void uncaughtException(Thread thread, Throwable th) {
                ApkComposeService.this.m35x7541076d(thread, th);
            }
        });
        this.composeThread.start();
    }

    /* renamed from: lambda$startComposeThread$0$com-gmail-heagoo-apkeditor-ApkComposeService, reason: not valid java name */
    /* synthetic */ void m35x7541076d(Thread thread, Throwable error) {
        Log.e("ApkComposeService", "Build worker failed", error);
        synchronized (this.composeResult) {
            if (this.composeResult.finished) {
                return;
            }
            taskFailed(getString(R.string.lab_compose_worker_failed, new Object[]{error.toString()}));
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x0037, code lost:
    
        if (r1 == null) goto L22;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private Map<String, String> getMapFromFile(String filepath) {
        Map<String, String> result = new HashMap<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filepath)));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String key = line;
                String value = br.readLine();
                if (value != null) {
                    result.put(key, value);
                }
            }
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

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public void setTaskStepInfo(ITaskCallback.TaskStepInfo stepInfo) {
        ITaskCallback ob;
        synchronized (this.composeResult) {
            this.composeResult.curStep = stepInfo;
        }
        if (this.observer != null && (ob = this.observer.get()) != null) {
            ob.setTaskStepInfo(stepInfo);
        }
        String desc = String.format(getResources().getString(R.string.step) + " %d/%d: %s", Integer.valueOf(stepInfo.stepIndex), Integer.valueOf(stepInfo.stepTotal), stepInfo.stepDescription);
        boolean forceShow = stepInfo.stepIndex == stepInfo.stepTotal;
        updateNotification(forceShow, getString(R.string.build_ongoing), desc);
    }

    public void setTaskProgress(float progress) {
        ITaskCallback ob;
        if (this.observer != null && (ob = this.observer.get()) != null) {
            ob.setTaskProgress(progress);
        }
    }

    public void taskSucceed() {
        ITaskCallback ob;
        synchronized (this.composeResult) {
            this.composeResult.finished = true;
            this.composeResult.ret = true;
            this.composeResult.failMessage = null;
        }
        updateNotification(true, getString(R.string.build_finished), getString(R.string.succeed));
        if (this.observer != null && (ob = this.observer.get()) != null) {
            ob.taskSucceed();
        }
    }

    public void taskFailed(String errMessage) {
        ITaskCallback ob;
        if (errMessage == null || errMessage.trim().isEmpty()) {
            errMessage = getString(R.string.lab_compose_unknown_error);
        }
        synchronized (this.composeResult) {
            this.composeResult.finished = true;
            this.composeResult.ret = false;
            this.composeResult.failMessage = errMessage;
        }
        updateNotification(true, getString(R.string.build_finished), getString(R.string.failed));
        if (this.observer != null && (ob = this.observer.get()) != null) {
            ob.taskFailed(errMessage);
        }
    }

    public void taskWarning(String message) {
        ITaskCallback ob;
        if (this.observer != null && (ob = this.observer.get()) != null) {
            ob.taskWarning(message);
        }
    }

    private class MyHandler extends Handler {
        private String desc;
        private String title;

        private MyHandler() {
        }

        public void setInfo(String title, String desc) {
            this.title = title;
            this.desc = desc;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (ApkComposeService.this.mNotifyBuilder != null && ApkComposeService.this.mNotificationManager != null) {
                        try {
                            if (this.title != null) {
                                ApkComposeService.this.mNotifyBuilder.setContentTitle(this.title);
                            }
                            if (this.desc != null) {
                                ApkComposeService.this.mNotifyBuilder.setContentText(this.desc);
                            }
                            if (ApkComposeService.this.foregroundStarted) {
                                ApkComposeService.this.mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, ApkComposeService.this.mNotifyBuilder.build());
                            } else {
                                ApkComposeService.this.startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, ApkComposeService.this.mNotifyBuilder.build());
                                ApkComposeService.this.foregroundStarted = true;
                            }
                            ApkComposeService.this.lastUpdateTime = System.currentTimeMillis();
                            break;
                        } catch (RuntimeException error) {
                            Log.e("ApkComposeService", "Notification update failed", error);
                        }
                    }
                    break;
            }
        }
    }

    public class ComposeResult {
        public ITaskCallback.TaskStepInfo curStep;
        public String failMessage;
        public boolean ret;
        public boolean started = false;
        public boolean finished = false;

        public ComposeResult() {
        }

        public void clear() {
            this.started = false;
            this.finished = false;
            this.failMessage = null;
            this.curStep = null;
        }
    }

    public class ComposeServiceBinder extends Binder {
        public ComposeServiceBinder() {
        }

        public void setObserver(ITaskCallback _observer) {
            ApkComposeService.this.observer = new WeakReference(_observer);
            synchronized (ApkComposeService.this.composeResult) {
                if (!ApkComposeService.this.composeResult.started && !ApkComposeService.this.composeResult.finished) {
                    _observer.taskFailed(ApkComposeService.this.getString(R.string.lab_compose_service_lost));
                } else if (ApkComposeService.this.composeResult.finished) {
                    if (ApkComposeService.this.composeResult.ret) {
                        _observer.taskSucceed();
                    } else {
                        _observer.taskFailed(ApkComposeService.this.composeResult.failMessage);
                    }
                } else if (ApkComposeService.this.composeResult.curStep != null) {
                    _observer.setTaskStepInfo(ApkComposeService.this.composeResult.curStep);
                }
            }
        }

        public void buildAgain() {
            ApkComposeService.this.resetStatus();
            synchronized (ApkComposeService.this.composeResult) {
                ApkComposeService.this.composeResult.started = true;
            }
            try {
                showNotification();
                ApkComposeService.this.startComposeThread();
            } catch (LinkageError | RuntimeException error) {
                Log.e("ApkComposeService", "Unable to restart build", error);
                ApkComposeService.this.taskFailed(ApkComposeService.this.getString(R.string.lab_compose_start_failed, new Object[]{error.toString()}));
            }
        }

        public void stopBuilding() {
            if (ApkComposeService.this.composeThread != null && ApkComposeService.this.composeThread.isAlive()) {
                ApkComposeService.this.composeThread.stopRunning();
            }
            hideNotification();
        }

        public void setBuildHooker(IApkMaking extraMaker) {
            ApkComposeService.this.extraMaker = extraMaker;
        }

        public Map<String, Object> getValues() {
            Map<String, Object> ret = new HashMap<>();
            ret.put("srcApkPath", ApkComposeService.this.srcApkPath);
            ret.put("targetApkPath", ApkComposeService.this.targetApkPath);
            ret.put("decodeRootPath", ApkComposeService.this.decodeRootPath);
            boolean codeModified = (ApkComposeService.this.modifiedSmaliFolders == null || ApkComposeService.this.modifiedSmaliFolders.isEmpty()) ? false : true;
            ret.put("codeModified", Boolean.valueOf(codeModified));
            ret.put("signAPK", Boolean.valueOf(ApkComposeService.this.signAPK));
            return ret;
        }

        public void hideNotification() {
            if (ApkComposeService.this.mNotificationManager != null) {
                ApkComposeService.this.mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
                if (ApkComposeService.this.foregroundStarted) {
                    ApkComposeService.this.stopForeground(true);
                    ApkComposeService.this.foregroundStarted = false;
                }
                ApkComposeService.this.mNotificationManager = null;
                Log.e("DEBUG", "notification hided.");
            }
        }

        public void showNotification() {
            ApkComposeService.this.showNotification();
        }

        public boolean isRunning() {
            return ApkComposeService.this.composeThread != null && ApkComposeService.this.composeThread.isAlive();
        }
    }
}
