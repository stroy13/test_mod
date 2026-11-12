package com.gmail.heagoo.apkeditor.translate;

import dev.ffmpegkit.llama.LlamaJNI;
import java.io.IOException;

/** JNI calls for the pinned llama-android 0.1.1 C ABI; one session per worker. */
final class InstrumentedLlama {
    static void load() {
        // Initialises the AAR's native dependencies before our dynamically linked bridge.
        LlamaJNI ignored = LlamaJNI.INSTANCE;
        System.loadLibrary("local_ai_trace");
    }
    interface Observer { void onNativeProgress(int phase, int generated, int promptTokens, int detail); }
    static native long open(String path, int threads) throws IOException;
    static native byte[] complete(long handle, byte[] user, byte[] system, int maxTokens, Observer observer) throws IOException;
    static native String systemInfo(long handle);
    static native void cancel(long handle);
    static native void close(long handle);
    private InstrumentedLlama() {}
}
