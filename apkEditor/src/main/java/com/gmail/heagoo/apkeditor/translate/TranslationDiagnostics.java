package com.gmail.heagoo.apkeditor.translate;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.gmail.heagoo.apkeditor.base.R;
import java.util.Arrays;

/** Stores only the latest report; no source/translated text, filenames, paths or resource names. */
public final class TranslationDiagnostics {
    private static final String KEY = "LocalAiLastDiagnostics";
    private final Context context;
    private final TranslationTrace trace;
    private final long initialCpuMs;
    private final boolean googleMlKit;
    private final String sourceLanguage;
    private final String targetLanguage;
    public TranslationDiagnostics(Context context, TranslationTrace trace) { this(context, trace, false); }
    public TranslationDiagnostics(Context context, TranslationTrace trace, boolean googleMlKit) {
        this(context, trace, googleMlKit, null, null);
    }
    public TranslationDiagnostics(Context context, TranslationTrace trace, boolean googleMlKit, String sourceLanguage, String targetLanguage) {
        this.googleMlKit = googleMlKit;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.context = context.getApplicationContext();
        this.trace = trace;
        this.initialCpuMs = android.os.Process.getElapsedCpuTime();
    }
    public String report() {
        String version;
        try { version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; }
        catch (Exception e) { version = "unknown"; }
        String thermal = "unavailable";
        if (Build.VERSION.SDK_INT >= 29) {
            android.os.PowerManager power = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (power != null) thermal = Integer.toString(power.getCurrentThermalStatus());
        }
        return trace.report() + "app_version=" + version + (googleMlKit ? " engine=google-ml-kit-on-device\n" : " engine=unknown\n")
                + (googleMlKit ? "source_language=" + sourceLanguage + " target_language=" + targetLanguage + "\n" : "")
                + "device=" + Build.MANUFACTURER + " " + Build.MODEL + " android=" + Build.VERSION.RELEASE
                + " sdk=" + Build.VERSION.SDK_INT + " supported_64bit=" + Arrays.toString(Build.SUPPORTED_64_BIT_ABIS)
                + " process_64bit=" + android.os.Process.is64Bit() + " processors=" + Runtime.getRuntime().availableProcessors() + "\n"
                + "thermal_status=" + thermal + " (0=none, 1=light, 2=moderate, 3=severe, 4+=critical)\n"
                + "process_cpu_ms=" + (android.os.Process.getElapsedCpuTime() - initialCpuMs)
                + " native_heap_bytes=" + Debug.getNativeHeapAllocatedSize() + "\n"
                + (googleMlKit ? "ML Kit stages describe local model download and translation.\n" : "Native stages show token counts, but cannot inspect inside an individual decode call.\n");
    }
    public void save() { PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY, report()).apply(); }
    public void copy(Context ui) { String report = report(); save(); copyText(ui, report); }
    public static void copyLast(Context ui) {
        String value = PreferenceManager.getDefaultSharedPreferences(ui).getString(KEY, "");
        if (value.isEmpty()) { Toast.makeText(ui, R.string.ai_log_empty, Toast.LENGTH_SHORT).show(); return; }
        copyText(ui, value);
    }
    private static void copyText(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) { Toast.makeText(context, R.string.ai_log_copy_failed, Toast.LENGTH_SHORT).show(); return; }
        clipboard.setPrimaryClip(ClipData.newPlainText("Local AI diagnostic", text));
        Toast.makeText(context, R.string.ai_log_copied, Toast.LENGTH_LONG).show();
    }
}
