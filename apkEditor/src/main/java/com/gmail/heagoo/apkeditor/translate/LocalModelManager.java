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
            "https://huggingface.co/bartowski/Qwen_Qwen3-0.6B-GGUF/resolve/main/Qwen_Qwen3-0.6B-Q4_K_M.gguf?download=true",
            "https://huggingface.co/bartowski/Qwen_Qwen3-1.7B-GGUF/resolve/main/Qwen_Qwen3-1.7B-Q4_K_M.gguf?download=true",
            "https://huggingface.co/bartowski/Qwen_Qwen3-4B-GGUF/resolve/main/Qwen_Qwen3-4B-Q4_K_M.gguf?download=true",
            "https://huggingface.co/bartowski/Qwen2.5-3B-Instruct-GGUF/resolve/main/Qwen2.5-3B-Instruct-Q4_K_M.gguf?download=true",
            "https://huggingface.co/bartowski/Qwen_Qwen3-4B-GGUF/resolve/main/Qwen_Qwen3-4B-Q5_K_M.gguf?download=true"};
    private LocalModelManager() {}
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
        new AlertDialog.Builder(a).setTitle(R.string.ai_models)
                .setMessage(a.getString(R.string.ai_models_description, label))
                .setItems(new String[]{a.getString(R.string.ai_choose), a.getString(R.string.ai_import),
                        a.getString(R.string.ai_download), a.getString(R.string.ai_delete)}, (dialog, which) -> {
                    if (which == 0) choose(a, false);
                    else if (which == 1) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                        a.startActivityForResult(intent, PICK_MODEL);
                    } else if (which == 2) catalog(a);
                    else choose(a, true);
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
                new AlertDialog.Builder(a).setMessage(R.string.ai_browser_download)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dd, w) -> {
                            try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LINKS[i]))); }
                            catch (Exception e) { android.widget.Toast.makeText(a, e.getMessage(), 1).show(); }
                        }).show()).setNegativeButton(android.R.string.cancel, null).show();
    }
    public static void importUri(Activity a, Uri uri) {
        if (uri == null) return;
        String display = "model.gguf";
        try (Cursor c = a.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && c.getString(0) != null) display = c.getString(0);
        } catch (Exception ignored) {}
        final String name = display;
        ProgressDialog progress = new ProgressDialog(a);
        progress.setMessage(a.getString(R.string.ai_importing));
        progress.setIndeterminate(true); progress.setCancelable(false); progress.show();
        new Thread(() -> {
            File file = null; String error = null;
            try (InputStream in = a.getContentResolver().openInputStream(uri)) {
                if (in == null) throw new java.io.IOException("Cannot open file");
                file = LocalModelStore.importModel(in, directory(a));
                PreferenceManager.getDefaultSharedPreferences(a).edit().putString(SELECTED, file.getName())
                        .putString("LocalAiName." + file.getName(), name).apply();
            } catch (Exception e) { error = e.getMessage(); }
            final String failure = error;
            a.runOnUiThread(() -> {
                if (a.isFinishing() || a.isDestroyed()) return;
                progress.dismiss();
                if (failure != null) new AlertDialog.Builder(a).setMessage(failure).setPositiveButton(android.R.string.ok, null).show();
                else show(a);
            });
        }, "local-model-import").start();
    }
}
