package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import com.gmail.heagoo.common.EditorTheme;

public class ApkEditorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity a, Bundle b) {
                if (Build.VERSION.SDK_INT >= 35) {
                    EdgeInsets.install(a);
                }
            }

            @Override
            public void onActivityStarted(Activity a) {
            }

            @Override
            public void onActivityResumed(Activity a) {
                if (!EditorTheme.needsRecreate(a) || a.isFinishing()) {
                    return;
                }
                a.recreate();
            }

            @Override
            public void onActivityPaused(Activity a) {
            }

            @Override
            public void onActivityStopped(Activity a) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity a, Bundle b) {
            }

            @Override
            public void onActivityDestroyed(Activity a) {
            }
        });
    }
}
