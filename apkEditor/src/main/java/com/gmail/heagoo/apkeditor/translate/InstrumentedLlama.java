package com.gmail.heagoo.apkeditor.translate;

import dev.ffmpegkit.llama.LlamaJNI;
import java.io.IOException;

/** JNI calls for the pinned llama-android 0.1.1 C ABI; one session per worker. */
final class InstrumentedLlama {
    private static boolean fast;
    static void load() {
        System.loadLibrary("local_ai_trace");
        fast = false;
        if (supportsFastCpu()) {
            try { System.loadLibrary("local_llama_fast"); fast = true; }
            catch (LinkageError ignored) { /* Fall back to the baseline engine. */ }
        }
        if (!fast) {
            // Initialises the AAR's native dependencies for the baseline engine.
            LlamaJNI ignored = LlamaJNI.INSTANCE;
        }
    }
    static boolean isFast() { return fast; }
    static native boolean supportsFastCpu();
    interface Observer { void onNativeProgress(int phase, int generated, int promptTokens, int detail); }
    static native long open(String path, int threads, boolean fast) throws IOException;
    static native byte[] complete(long handle, byte[] user, byte[] system, int maxTokens, Observer observer) throws IOException;
    static native String systemInfo(long handle);
    static native void cancel(long handle);
    static native void close(long handle);
    private InstrumentedLlama() {}
}
