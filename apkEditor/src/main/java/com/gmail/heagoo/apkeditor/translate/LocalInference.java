package com.gmail.heagoo.apkeditor.translate;

import android.os.Build;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** A CPU inference session. Inference and close run on the worker; cancel may run on UI. */
public final class LocalInference implements AutoCloseable {
    private static final java.util.concurrent.Semaphore ENGINE = new java.util.concurrent.Semaphore(1);
    private final Object handleLock = new Object();
    private final TranslationTrace trace;
    private final AtomicBoolean cancelled;
    private long handle;
    private boolean acquired;
    public static int threadCount() { return Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors())); }

    public LocalInference(File model, TranslationTrace trace, AtomicBoolean cancelled) throws IOException {
        this.trace = trace;
        this.cancelled = cancelled;
        if (!Arrays.asList(Build.SUPPORTED_64_BIT_ABIS).contains("arm64-v8a") || !android.os.Process.is64Bit())
            throw new IOException("Local AI requires an arm64 process");
        if (model == null || !model.isFile()) throw new IOException("Model file not found");
        try {
            trace.stage(TranslationTrace.Stage.WAIT_ENGINE);
            ENGINE.acquire(); acquired = true;
            if (cancelled.get()) throw new IOException("Translation cancelled");
            trace.stage(TranslationTrace.Stage.LOAD_MODEL);
            InstrumentedLlama.load();
            long opened = InstrumentedLlama.open(model.getAbsolutePath(), threadCount());
            if (opened == 0) throw new IOException("Model could not be loaded");
            synchronized (handleLock) {
                handle = opened;
                if (cancelled.get()) InstrumentedLlama.cancel(handle);
            }
            trace.stage(TranslationTrace.Stage.MODEL_READY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Local AI was interrupted", e);
        } catch (LinkageError | RuntimeException | IOException e) {
            close();
            throw new IOException("Local AI engine could not load the model: " + e.getMessage(), e);
        }
    }

    public void requestCancel() {
        cancelled.set(true);
        synchronized (handleLock) { if (handle != 0) InstrumentedLlama.cancel(handle); }
    }
    public String translate(String value, String language) throws Exception {
        if (handle == 0) throw new IOException("Model is closed");
        if (value.length() > 1000) throw new IOException("String is too long for local translation");
        String prompt = "Translate the following application UI text into " + language
                + ". Output only the translation. Preserve all placeholders, escapes and tags exactly.\n"
                + value + "\n/no_think";
        int max = TranslationBudget.maxTokens(value);
        trace.stringStart(value.length(), max);
        byte[] result = InstrumentedLlama.complete(handle, prompt.getBytes(StandardCharsets.UTF_8),
                "You are a precise and natural software localization translator.".getBytes(StandardCharsets.UTF_8),
                max, trace::nativeProgress);
        trace.stage(TranslationTrace.Stage.VALIDATE);
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(result)).toString().trim();
        } catch (CharacterCodingException e) { throw new IOException("Invalid UTF-8 from model", e); }
        if (text.isEmpty()) throw new IOException("Translation is empty");
        if (text.contains("<think>") || text.contains("</think>")) throw new IOException("Model returned reasoning instead of translation");
        if (!TranslationTokens.preserved(value, text)) throw new IOException("Translation changed a resource placeholder");
        return text;
    }
    @Override public void close() {
        try {
            synchronized (handleLock) {
                if (handle != 0) {
                    trace.stage(TranslationTrace.Stage.RELEASE_MODEL);
                    InstrumentedLlama.close(handle); handle = 0;
                }
            }
        } finally { if (acquired) { acquired = false; ENGINE.release(); } }
    }
}
