package com.gmail.heagoo.apkeditor.translate;

import android.os.Build;
import dev.ffmpegkit.llama.LlamaJNI;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/** A single-use CPU inference session. All calls must run off the UI thread. */
public final class LocalInference implements AutoCloseable {
    private static final java.util.concurrent.Semaphore ENGINE = new java.util.concurrent.Semaphore(1);
    private long handle;
    private boolean acquired;
    private final TranslationTrace trace;

    public static int threadCount() { return Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors())); }

    public LocalInference(File model, TranslationTrace trace) throws IOException {
        this.trace = trace;
        if (!Arrays.asList(Build.SUPPORTED_64_BIT_ABIS).contains("arm64-v8a") || !android.os.Process.is64Bit())
            throw new IOException("Local AI requires an arm64 process");
        if (model == null || !model.isFile()) throw new IOException("Model file not found");
        try {
            trace.stage(TranslationTrace.Stage.WAIT_ENGINE);
            ENGINE.acquire();
            acquired = true;
            trace.stage(TranslationTrace.Stage.LOAD_MODEL);
            handle = LlamaJNI.INSTANCE.nativeLoadModel(model.getAbsolutePath(), 2048, threadCount(), 0);
            if (handle == 0) throw new IOException("Model could not be loaded");
            trace.stage(TranslationTrace.Stage.MODEL_READY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Local AI was interrupted", e);
        } catch (LinkageError | RuntimeException | IOException e) {
            if (acquired) { acquired = false; ENGINE.release(); }
            throw new IOException("Local AI engine could not load the model", e);
        }
    }

    public String translate(String value, String language) throws Exception {
        if (handle == 0) throw new IOException("Model is closed");
        if (value.length() > 1000) throw new IOException("String is too long for local translation");
        // Reject supplementary code points until the native wrapper uses proper UTF-8/JNI conversion.
        if (value.codePoints().anyMatch(c -> c > 0xffff)) throw new IOException("Unsupported supplementary Unicode characters");
        String prompt = "Translate the following application UI text into " + language
                + ". Output only the translation. Preserve all placeholders, escapes and tags exactly.\n"
                + value + "\n/no_think";
        int max = TranslationBudget.maxTokens(value);
        trace.stringStart(value.length(), max);
        trace.stage(TranslationTrace.Stage.NATIVE_COMPLETE);
        JSONObject result = new JSONObject(LlamaJNI.INSTANCE.nativeComplete(handle, prompt,
                "You are a precise and natural software localization translator.", max, 0.0f, 0.9f, 40, 0));
        trace.nativeResult(result.optInt("tokensGenerated", -1), result.optLong("promptEvalTimeMs", -1), result.optLong("generateTimeMs", -1));
        trace.stage(TranslationTrace.Stage.VALIDATE);
        if (result.has("error")) throw new IOException(result.optString("error"));
        String text = result.optString("text", "").trim();
        if (result.optInt("tokensGenerated", max) >= max || text.isEmpty()) throw new IOException("Translation incomplete");
        if (text.contains("<think>") || text.contains("</think>")) throw new IOException("Model returned reasoning instead of translation");
        if (!TranslationTokens.preserved(value, text)) throw new IOException("Translation changed a resource placeholder");
        return text;
    }

    @Override public void close() {
        try {
            trace.stage(TranslationTrace.Stage.RELEASE_MODEL);
            if (handle != 0) { LlamaJNI.INSTANCE.nativeReleaseModel(handle); handle = 0; }
        } finally {
            if (acquired) { acquired = false; ENGINE.release(); }
        }
    }
}
