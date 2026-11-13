package com.gmail.heagoo.apkeditor.translate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/** Thread-safe diagnostic metadata only. Never record text, resource names or file paths. */
public final class TranslationTrace {
    public enum Stage { START, WAIT_ENGINE, LOAD_MODEL, MODEL_READY, PREPARE_STRING,
        NATIVE_COMPLETE, NATIVE_TEMPLATE, NATIVE_TOKENIZE, NATIVE_PREFILL, NATIVE_SAMPLE, NATIVE_DECODE, NATIVE_RETURNED, VALIDATE, STRING_DONE, CANCEL_REQUESTED,
        RELEASE_MODEL, FINISHED, ERROR }
    private final long start = System.nanoTime();
    private final Deque<String> events = new ArrayDeque<>();
    private Stage stage = Stage.START;
    private long stageTime = start;
    private int completed;
    private int rejected;
    private int batchAttempts, batchSuccesses, batchFallbacks, batchItems;
    private long batchStarted, batchTotalMs;
    private final java.util.EnumMap<TranslationRejectedException.Reason, Integer> rejectionCounts = new java.util.EnumMap<>(TranslationRejectedException.Reason.class);
    private int generated;
    private int promptTokens;
    private int templateStatus;
    private boolean noThinkPrefix;
    private int nativeDetail;
    private String engineInfo = "unavailable";
    private String backend = "unavailable";
    public synchronized void backend(boolean fast) { backend = fast ? "armv8.2-dotprod" : "baseline"; }
    private long prefillStart, prefillMs = -1, generationStart, lastTokenAt;
    private long lastDecodeStart, lastDecodeMs = -1, maxDecodeMs = -1, totalDecodeMs;
    private int decodedTokens;
    private int selectedThreads;
    private int reusedPromptTokens;
    public synchronized void engineInfo(String info) {
        // System capabilities only; cap and filter the native library's text.
        if (info != null && info.length() <= 2048 && info.matches("[A-Za-z0-9_ .:=|+/-]+")) engineInfo = info;
    }
    private final int total;
    private final int threads;
    private final long modelBytes;
    private final boolean sample;
    private final String language;

    public TranslationTrace(int total, int threads, long modelBytes, boolean sample, String language) {
        this.total = total;
        this.threads = threads;
        this.modelBytes = modelBytes;
        this.sample = sample;
        this.language = language != null && language.matches("-[a-z]{2,3}(-r[A-Z]{2})?") ? language : "other";
        stage(Stage.START);
    }
    public synchronized void stage(Stage next) {
        stage = next;
        stageTime = System.nanoTime();
        add("stage=" + next);
    }
    public synchronized void stringStart(int chars, int maxTokens) {
        stage = Stage.PREPARE_STRING;
        stageTime = System.nanoTime();
        generated = 0; promptTokens = 0; templateStatus = 0; nativeDetail = 0; reusedPromptTokens = 0;
        prefillStart = generationStart = lastTokenAt = lastDecodeStart = totalDecodeMs = 0;
        prefillMs = lastDecodeMs = maxDecodeMs = -1; decodedTokens = 0;
        add("string=" + (completed + 1) + "/" + total + " chars=" + chars + " token_limit=" + maxTokens);
    }
    /** Called by JNI at phase boundaries and on every generated token. */
    public synchronized void nativeProgress(int phase, int count, int prompt, int detail) {
        if (phase == 11) {
            noThinkPrefix = count == 1;
            add("qwen3_no_think_prefix=" + (noThinkPrefix ? "applied" : "not_applied"));
            return;
        }
        if (phase == 10) {
            add("thread_trial_skipped threads=" + count + " after_slow_ms=" + detail);
            return;
        }
        if (phase == 9) {
            reusedPromptTokens = count;
            add("prompt_cache reused_tokens=" + count + " total_tokens=" + prompt);
            return;
        }
        if (phase == 7) {
            add("thread_trial threads=" + count + " token=" + prompt + " decode_ms=" + detail);
            return;
        }
        if (phase == 8) {
            selectedThreads = count;
            add("thread_selected threads=" + count + " token=" + prompt + " avg_decode_ms=" + detail);
            return;
        }
        Stage[] stages = {Stage.NATIVE_COMPLETE, Stage.NATIVE_TEMPLATE, Stage.NATIVE_TOKENIZE,
                Stage.NATIVE_PREFILL, Stage.NATIVE_SAMPLE, Stage.NATIVE_DECODE, Stage.NATIVE_RETURNED};
        if (phase < 1 || phase >= stages.length) return;
        long now = System.nanoTime();
        if (phase == 3) prefillStart = now;
        if (phase == 4 && count == 0 && prefillStart != 0 && prefillMs < 0) {
            prefillMs = TimeUnit.NANOSECONDS.toMillis(now - prefillStart);
            generationStart = now;
        }
        if (phase == 4 && count > 0 && lastDecodeStart != 0) {
            lastDecodeMs = TimeUnit.NANOSECONDS.toMillis(now - lastDecodeStart);
            totalDecodeMs += lastDecodeMs; decodedTokens++;
            maxDecodeMs = Math.max(maxDecodeMs, lastDecodeMs);
            lastDecodeStart = 0;
        }
        if (phase == 5) { lastTokenAt = now; lastDecodeStart = now; }
        stage = stages[phase];
        stageTime = now;
        generated = count;
        if (prompt > 0) promptTokens = prompt;
        if (phase == 2) templateStatus = detail;
        if (phase == 6) nativeDetail = detail;
        if (phase <= 3 || phase == 6 || (phase == 5 && count % 16 == 0))
            add("native phase=" + stage + " generated=" + generated + " prompt_tokens=" + promptTokens + " detail=" + detail);
    }
    public synchronized String nativeStatus() {
        return stage.name() + " (" + generated + " tokens)";
    }
    public synchronized void nativeResult(int tokens, long promptMs, long generateMs) {
        stage = Stage.NATIVE_RETURNED;
        stageTime = System.nanoTime();
        add("native_result tokens=" + tokens + " prompt_ms=" + promptMs + " generation_ms=" + generateMs);
    }
    public synchronized void failure(boolean io, boolean linkage) {
        stage = Stage.ERROR;
        stageTime = System.nanoTime();
        add("stage=ERROR kind=" + (linkage ? "linkage" : io ? "io" : "other"));
    }
    public synchronized void cancelRequested() { add("cancel_requested (waiting for current operation)"); }
    public synchronized void translationCacheHit() { add("translation_cache_hit"); }
    public synchronized void batchAttempt(int count) {
        batchAttempts++; batchItems += count; batchStarted = System.nanoTime(); add("batch_attempt items=" + count);
    }
    public synchronized void batchSuccess() { batchSuccesses++; finishBatch("batch_success"); }
    public synchronized void batchFallback() { batchFallbacks++; finishBatch("batch_fallback to_individual=true"); }
    private void finishBatch(String event) {
        long elapsed = batchStarted == 0 ? 0 : TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - batchStarted);
        batchTotalMs += elapsed; batchStarted = 0;
        add(event + " elapsed_ms=" + elapsed);
    }
    public synchronized void stringRejected(TranslationRejectedException.Reason reason) {
        rejected++;
        Integer count = rejectionCounts.get(reason);
        rejectionCounts.put(reason, count == null ? 1 : count + 1);
        add("string_rejected reason=" + reason.name());
    }
    public synchronized void stringDone() { completed++; stage(Stage.STRING_DONE); }
    private void add(String text) {
        if (events.size() == 40) events.removeFirst();
        events.addLast("+" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + "ms " + text);
    }
    public synchronized String report() {
        long now = System.nanoTime();
        StringBuilder out = new StringBuilder("Local AI diagnostic v1\n");
        out.append("mode=").append(sample ? "sample" : "full").append(" target=").append(language)
                .append(" model_bytes=").append(modelBytes).append(" threads=").append(threads).append('\n');
        out.append("completed=").append(completed).append('/').append(total)
                .append(" rejected=").append(rejected).append(" stage=").append(stage).append(" stage_elapsed_ms=")
                .append(TimeUnit.NANOSECONDS.toMillis(now - stageTime))
                .append(" total_elapsed_ms=").append(TimeUnit.NANOSECONDS.toMillis(now - start)).append('\n');
        out.append("prompt_tokens=").append(promptTokens).append(" generated_tokens=").append(generated)
                .append(" template_status=").append(templateStatus).append(" native_detail=").append(nativeDetail).append('\n');
        out.append("batch_attempts=").append(batchAttempts).append(" batch_items=").append(batchItems)
                .append(" batch_successes=").append(batchSuccesses).append(" batch_fallbacks=").append(batchFallbacks)
                .append(" batch_total_ms=").append(batchTotalMs).append('\n');
        out.append("qwen3_no_think_prefix=").append(noThinkPrefix ? "applied" : "not_applied").append('\n');
        out.append("reused_prompt_tokens=").append(reusedPromptTokens).append('\n');
        out.append("prefill_ms=").append(prefillMs).append(" generation_elapsed_ms=")
                .append(generationStart == 0 ? -1 : TimeUnit.NANOSECONDS.toMillis(now - generationStart))
                .append(" last_token_age_ms=").append(lastTokenAt == 0 ? -1 : TimeUnit.NANOSECONDS.toMillis(now - lastTokenAt))
                .append(" decoded_tokens=").append(decodedTokens).append(" decode_avg_ms=")
                .append(decodedTokens == 0 ? -1 : totalDecodeMs / decodedTokens)
                .append(" decode_last_ms=").append(lastDecodeMs).append(" decode_max_ms=").append(maxDecodeMs).append('\n');
        out.append("selected_generation_threads=").append(selectedThreads == 0 ? "testing" : selectedThreads).append('\n');
        for (java.util.Map.Entry<TranslationRejectedException.Reason, Integer> entry : rejectionCounts.entrySet())
            out.append("rejected_").append(entry.getKey().name()).append('=').append(entry.getValue()).append('\n');
        out.append("engine_backend=").append(backend).append('\n');
        out.append("engine_features=").append(engineInfo).append('\n');
        for (String event : events) out.append(event).append('\n');
        return out.toString();
    }
}
