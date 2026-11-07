package com.gmail.heagoo.apkeditor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.gmail.heagoo.apkeditor.Constants;
import com.gmail.heagoo.apkeditor.base.R;

class ForegroundService extends Service {
    private static final String LOG_TAG = "ForegroundService";

    ForegroundService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Start Foreground Intent ");
            Intent notificationIntent = new Intent(this, (Class<?>) ApkComposeActivity.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(268468224);
            int i = Build.VERSION.SDK_INT;
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 67108864);
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.apkeditor);
            Notification notification = new NotificationCompat.Builder(this).setContentTitle("Truiton Music Player").setTicker("Truiton Music Player").setContentText("My Music").setLargeIcon(Bitmap.createScaledBitmap(icon, BinaryDiffIndex.PAGE_ROWS, BinaryDiffIndex.PAGE_ROWS, false)).setContentIntent(pendingIntent).setOngoing(true).build();
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return 1;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "In onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
