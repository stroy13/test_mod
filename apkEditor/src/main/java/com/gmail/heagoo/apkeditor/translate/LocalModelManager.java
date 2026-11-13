package com.gmail.heagoo.apkeditor.translate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import com.gmail.heagoo.apkeditor.base.R;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

/** Settings and document-picker entry point for experimental local models. */
public final class LocalModelManager {
    public static final int PICK_MODEL = 19613;
    private static final String SELECTED = "LocalAiModel";
    private static final String[] NAMES = {
            "Qwen3 0.6B Q4_K_M (~0.48 GB)", "Qwen3 1.7B Q4_K_M (~1.28 GB)",
            "Qwen3 4B Q4_K_M (~2.50 GB)", "Qwen2.5 3B Instruct Q4_K_M (~1.93 GB)",
            "Qwen3 4B Q5_K_M (~2.89 GB)"};
    private static final String[] LINKS = {
            "https://huggingface.co/bartowski/Qwen_Qwen3-0.6B-GGUF/resolve/7bcae0bc7b0606f1e948f8cdb31b98a2c10635db/Qwen_Qwen3-0.6B-Q4_K_M.gguf?download=true",
            "https://huggingface.co/bartowski/Qwen_Qwen3-1.7B-GGUF/resolve/822702a8257cf1b05a27d73f775d8606cf2e33a4/Qwen_Qwen3-1.7B-Q4_K_M.gguf?download=true",
            "https://huggingface.co/bartowski/Qwen_Qwen3-4B-GGUF/resolve/a8512ea1cb744e4e9537847a993f49229b77d1ce/Qwen_Qwen3-4B-Q4_K_M.gguf?download=true",
            "https://huggingface.co/bartowski/Qwen2.5-3B-Instruct-GGUF/resolve/254600e47ea694216f6108325376ce5b35d456a1/Qwen2.5-3B-Instruct-Q4_K_M.gguf?download=true",
            "https://huggingface.co/bartowski/Qwen_Qwen3-4B-GGUF/resolve/81efb03138d70e248fdd8908ca8969a9ec9e7bfc/Qwen_Qwen3-4B-Q5_K_M.gguf?download=true"};
    private static final String[] HASHES = {
            "9acfc1e001311f34b4252001b626f2e466d592a42065f66571bff3790d4e1b14",
            "72c5c3cb38fa32d5256e2fe30d03e7a64c6c79e668ad84057e3bd66e250b24fb",
            "fbe1d5edd4ce802ae3ae7c7e4ab7d09789d697fdac1fc7929f8df4ca3c41bae3",
            "9c9f56a391a3abbd5b89d0245bf6106081bcc3173119d4229235dd9d23253f94",
            "3e6002bee4457c8061c04bfb742c3b23aca976606dfe0e0963cbfd434a370eac"};
    private static final java.util.Map<Activity, java.util.concurrent.atomic.AtomicBoolean> ACTIVE = new java.util.WeakHashMap<>();
    private LocalModelManager() {}
    public static void cancelFor(Activity activity) {
        synchronized (ACTIVE) {
            java.util.concurrent.atomic.AtomicBoolean flag = ACTIVE.remove(activity);
            if (flag != null) flag.set(true);
        }
    }
    private static File directory(Activity a) { return new File(a.getFilesDir(), "local-ai-models"); }
    public static File selected(Activity a) {
        String name = PreferenceManager.getDefaultSharedPreferences(a).getString(SELECTED, "");
        if (!name.matches("[0-9a-f-]{36}\\.gguf")) return null;
        File f = new File(directory(a), name);
        return f.isFile() ? f : null;
    }
    public static void show(Activity a) {
        File selected = selected(a);
        String label = selected == null ? a.getString(R.string.ai_none) : label(a, selected);
        new AlertDialog.Builder(a).setTitle(a.getString(R.string.ai_models) + "\n" + a.getString(R.string.ai_selected, label))
                .setItems(new String[]{a.getString(R.string.ai_choose), a.getString(R.string.ai_import),
                        a.getString(R.string.ai_download), a.getString(R.string.ai_delete), a.getString(R.string.ai_copy_last_log)}, (dialog, which) -> {
                    if (which == 0) choose(a, false);
                    else if (which == 1) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                        a.startActivityForResult(intent, PICK_MODEL);
                    } else if (which == 2) catalog(a);
                    else if (which == 3) choose(a, true);
                    else TranslationDiagnostics.copyLast(a);
                }).setNegativeButton(android.R.string.cancel, null).show();
    }
    private static String label(Activity a, File f) {
        return PreferenceManager.getDefaultSharedPreferences(a).getString("LocalAiName." + f.getName(), f.getName());
    }
    private static void choose(Activity a, boolean delete) {
        File[] files = directory(a).listFiles((dir, name) -> name.matches("[0-9a-f-]{36}\\.gguf"));
        if (files == null || files.length == 0) {
            new AlertDialog.Builder(a).setMessage(R.string.ai_none).setPositiveButton(android.R.string.ok, null).show(); return;
        }
        Arrays.sort(files, (x, y) -> label(a, x).compareToIgnoreCase(label(a, y)));
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) names[i] = label(a, files[i]);
        new AlertDialog.Builder(a).setTitle(delete ? R.string.ai_delete : R.string.ai_choose)
                .setItems(names, (d, i) -> {
                    if (!delete) {
                        PreferenceManager.getDefaultSharedPreferences(a).edit().putString(SELECTED, files[i].getName()).apply();
                        show(a);
                    } else new AlertDialog.Builder(a).setMessage(a.getString(R.string.ai_delete_confirm, names[i]))
                            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, (dd, w) -> {
                                if (!files[i].delete()) { android.widget.Toast.makeText(a, R.string.ai_delete_failed, 1).show(); return; }
                                android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
                                android.content.SharedPreferences.Editor edit = prefs.edit().remove("LocalAiName." + files[i].getName());
                                if (files[i].getName().equals(prefs.getString(SELECTED, ""))) edit.remove(SELECTED);
                                edit.apply(); show(a);
                            }).show();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }
    private static void catalog(Activity a) {
        new AlertDialog.Builder(a).setTitle(R.string.ai_download).setItems(NAMES, (d, i) ->
                new AlertDialog.Builder(a).setMessage(a.getString(R.string.ai_download_confirm, NAMES[i]))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dd, w) -> transfer(a, null, i, NAMES[i], -1)).show())
                .setNegativeButton(android.R.string.cancel, null).show();
    }
    public static void importUri(Activity a, Uri uri) {
        if (uri == null) return;
        String display = "model.gguf";
        long size = -1;
        try (Cursor c = a.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                if (!c.isNull(0)) display = c.getString(0);
                if (!c.isNull(1)) size = c.getLong(1);
            }
        } catch (Exception ignored) {}
        transfer(a, uri, -1, display, size);
    }

    private static void transfer(Activity a, Uri uri, int index, String name, long size) {
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        synchronized (ACTIVE) {
            if (ACTIVE.containsKey(a)) return;
            ACTIVE.put(a, cancelled);
        }
        ProgressDialog dialog = new ProgressDialog(a);
        dialog.setTitle(index >= 0 ? R.string.ai_download : R.string.ai_import);
        dialog.setMessage(a.getString(R.string.ai_preparing));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setButton(android.content.DialogInterface.BUTTON_NEGATIVE, a.getString(android.R.string.cancel), (d, w) -> {});
        dialog.setOnShowListener(d -> dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
            cancelled.set(true);
            v.setEnabled(false);
            dialog.setMessage(a.getString(R.string.ai_cancelling));
        }));
        dialog.show();
        new Thread(() -> {
            final long[] total = {size};
            LocalModelStore.Progress progress = new LocalModelStore.Progress() {
                private long lastUpdate;
                public boolean isCancelled() { return cancelled.get(); }
                public void onTotal(long bytes) { total[0] = bytes; }
                public void onBytes(long bytes) {
                    long now = android.os.SystemClock.elapsedRealtime();
                    if (now - lastUpdate < 250) return;
                    lastUpdate = now;
                    String msg = total[0] > 0
                            ? a.getString(R.string.ai_progress_bytes_total, bytes / 1048576L, total[0] / 1048576L)
                            : a.getString(R.string.ai_progress_bytes, bytes / 1048576L);
                    a.runOnUiThread(() -> { if (!a.isFinishing() && !a.isDestroyed() && !cancelled.get()) dialog.setMessage(msg); });
                }
            };
            File file = null;
            String error = null;
            try {
                if (index >= 0) file = LocalModelDownload.download(LINKS[index], HASHES[index], directory(a), progress);
                else try (InputStream in = a.getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new java.io.IOException("Cannot open file");
                    file = LocalModelStore.importModel(in, directory(a), size, null, progress);
                }
                if (cancelled.get()) { file.delete(); file = null; }
                else PreferenceManager.getDefaultSharedPreferences(a).edit().putString(SELECTED, file.getName())
                        .putString("LocalAiName." + file.getName(), name).apply();
            } catch (Exception e) { error = e.getMessage() == null ? e.toString() : e.getMessage(); }
            synchronized (ACTIVE) { if (ACTIVE.get(a) == cancelled) ACTIVE.remove(a); }
            final String failure = error;
            a.runOnUiThread(() -> {
                if (a.isFinishing() || a.isDestroyed()) return;
                dialog.dismiss();
                if (cancelled.get()) android.widget.Toast.makeText(a, R.string.ai_cancelled, android.widget.Toast.LENGTH_SHORT).show();
                else if (failure != null) new AlertDialog.Builder(a).setTitle(R.string.ai_transfer_error).setMessage(failure).setPositiveButton(android.R.string.ok, null).show();
                else show(a);
            });
        }, "local-model-transfer").start();
    }
}
