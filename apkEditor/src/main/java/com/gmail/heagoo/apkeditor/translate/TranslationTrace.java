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
    private int generated;
    private int promptTokens;
    private int templateStatus;
    private int nativeDetail;
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
        generated = 0; promptTokens = 0; templateStatus = 0; nativeDetail = 0;
        add("string=" + (completed + 1) + "/" + total + " chars=" + chars + " token_limit=" + maxTokens);
    }
    /** Called by JNI at phase boundaries and on every generated token. */
    public synchronized void nativeProgress(int phase, int count, int prompt, int detail) {
        Stage[] stages = {Stage.NATIVE_COMPLETE, Stage.NATIVE_TEMPLATE, Stage.NATIVE_TOKENIZE,
                Stage.NATIVE_PREFILL, Stage.NATIVE_SAMPLE, Stage.NATIVE_DECODE, Stage.NATIVE_RETURNED};
        if (phase < 1 || phase >= stages.length) return;
        stage = stages[phase];
        stageTime = System.nanoTime();
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
                .append(" stage=").append(stage).append(" stage_elapsed_ms=")
                .append(TimeUnit.NANOSECONDS.toMillis(now - stageTime))
                .append(" total_elapsed_ms=").append(TimeUnit.NANOSECONDS.toMillis(now - start)).append('\n');
        out.append("prompt_tokens=").append(promptTokens).append(" generated_tokens=").append(generated)
                .append(" template_status=").append(templateStatus).append(" native_detail=").append(nativeDetail).append('\n');
        for (String event : events) out.append(event).append('\n');
        return out.toString();
    }
}
