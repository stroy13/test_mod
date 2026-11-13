package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.gmail.heagoo.apkeditor.base.R;

/** Notification permission is optional and is requested only once automatically. */
final class NotificationAccess {
    static final String PERMISSION = "android.permission.POST_NOTIFICATIONS";
    static final String ASKED = "notification_permission_requested_v1";
    static final String STAGE_CHANNEL = "build_stages_alert";
    static final String CHANNEL = "default"; // Preserve the existing build channel and user settings.
    private NotificationAccess() {}

    static boolean shouldRequest(int sdk, boolean asked, boolean granted) {
        return sdk >= 33 && !asked && !granted;
    }

    static void createBuildChannel(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL, context.getString(R.string.notification_build_channel), NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
        NotificationChannel stages = new NotificationChannel(STAGE_CHANNEL,
                context.getString(R.string.notification_stage_channel), NotificationManager.IMPORTANCE_HIGH);
        stages.enableVibration(false);
        stages.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
        manager.createNotificationChannel(stages);
    }

    static boolean requestOnFirstLaunch(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < 33) return false;
        android.content.SharedPreferences prefs = activity.getSharedPreferences("notification_access", Context.MODE_PRIVATE);
        boolean asked = prefs.getBoolean(ASKED, false);
        boolean granted = ContextCompat.checkSelfPermission(activity, PERMISSION) == PackageManager.PERMISSION_GRANTED;
        boolean request = shouldRequest(Build.VERSION.SDK_INT, asked, granted);
        if (!asked) prefs.edit().putBoolean(ASKED, true).apply();
        if (request) ActivityCompat.requestPermissions(activity, new String[]{PERMISSION}, requestCode);
        return request;
    }

    static void openSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
        try { activity.startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) {
            try { activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + activity.getPackageName()))); }
            catch (ActivityNotFoundException | SecurityException fallback) {
                Toast.makeText(activity, R.string.notification_settings_unavailable, Toast.LENGTH_LONG).show();
            }
        }
    }
}
