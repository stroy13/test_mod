package com.gmail.heagoo.apkeditor.translate;
import static org.junit.Assert.*;
import org.junit.Test;

public class TranslationTraceTest {
    @Test public void reportTracksBlockingStageAndCancellationWithoutChangingStage() {
        TranslationTrace t = new TranslationTrace(3, 4, 1024, true, "-ru");
        t.stringStart(21, 96);
        t.stage(TranslationTrace.Stage.NATIVE_COMPLETE);
        t.cancelRequested();
        String report = t.report();
        assertTrue(report.contains("stage=NATIVE_COMPLETE"));
        assertTrue(report.contains("chars=21 token_limit=96"));
        assertTrue(report.contains("cancel_requested"));
        assertTrue(report.contains("completed=0/3"));
    }
    @Test public void boundsEventsAndRejectsUntrustedLanguage() {
        TranslationTrace t = new TranslationTrace(100, 1, 9, false, "private text");
        for (int i = 0; i < 60; i++) { t.stringStart(4, 96); t.stringDone(); }
        String report = t.report();
        assertFalse(report.contains("private text"));
        assertTrue(report.contains("target=other"));
        assertFalse(report.contains("string=1/100"));
        assertTrue(report.contains("string=60/100"));
        assertTrue(report.split("\n").length <= 50);
    }
    @Test public void tracksNativeStagesAndTokenCount() {
        TranslationTrace t = new TranslationTrace(1, 4, 100, true, "-en");
        t.stringStart(110, 142);
        t.nativeProgress(2, 0, 0, 1);
        t.nativeProgress(9, 32, 55, 0);
        t.nativeProgress(3, 0, 55, 0);
        assertTrue(t.report().contains("reused_prompt_tokens=32"));
        assertTrue(t.report().contains("stage=NATIVE_PREFILL"));
        t.nativeProgress(4, 0, 55, 0);
        t.nativeProgress(5, 1, 55, 0);
        t.nativeProgress(4, 1, 55, 0);
        t.nativeProgress(7, 2, 1, 140);
        t.nativeProgress(10, 4, 5, 3100);
        assertTrue(t.report().contains("thread_trial_skipped threads=4 after_slow_ms=3100"));
        t.nativeProgress(8, 2, 6, 140);
        assertTrue(t.report().contains("thread_trial threads=2 token=1 decode_ms=140"));
        assertTrue(t.report().contains("selected_generation_threads=2"));
        t.nativeProgress(5, 17, 55, 0);
        assertTrue(t.report().contains("stage=NATIVE_DECODE"));
        assertTrue(t.report().contains("decoded_tokens=1"));
        assertFalse(t.report().contains("prefill_ms=-1"));
        assertTrue(t.report().contains("prompt_tokens=55 generated_tokens=17 template_status=1"));
    }
    @Test public void recordsTokenMetrics() {
        TranslationTrace t = new TranslationTrace(1, 1, 1, true, "-ru");
        t.nativeResult(12, 32, 100);
        assertTrue(t.report().contains("native_result tokens=12 prompt_ms=32 generation_ms=100"));
    }
    @Test public void reportsRejectedReasonWithoutResourceText() {
        TranslationTrace t = new TranslationTrace(1, 1, 1, true, "-ru");
        t.stringRejected(TranslationRejectedException.Reason.PLACEHOLDER);
        t.stringDone();
        assertTrue(t.report().contains("rejected=1"));
        assertTrue(t.report().contains("string_rejected reason=PLACEHOLDER"));
    }
    @Test public void recordsNoThinkPrefixWithoutPromptContent() {
        TranslationTrace t = new TranslationTrace(1, 1, 1, true, "-ru");
        t.nativeProgress(11, 1, 0, 0);
        assertTrue(t.report().contains("qwen3_no_think_prefix=applied"));
        t.nativeProgress(11, 0, 0, 0);
        assertTrue(t.report().contains("qwen3_no_think_prefix=not_applied"));
    }
}
