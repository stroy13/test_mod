#include <jni.h>
#include <dlfcn.h>
#include <atomic>
#include <chrono>
#include <mutex>
#include <string>
#include <vector>
#include "llama.h"

// All public C signatures come from the llama.cpp revision used by llama-android 0.1.1.
struct Api {
    void *lib = nullptr;
#define FUNC(name) decltype(&name) name##_fn = nullptr;
    FUNC(llama_backend_init) FUNC(llama_print_system_info) FUNC(llama_model_default_params) FUNC(llama_context_default_params)
    FUNC(llama_model_load_from_file) FUNC(llama_model_free) FUNC(llama_init_from_model) FUNC(llama_free)
    FUNC(llama_model_get_vocab) FUNC(llama_model_chat_template) FUNC(llama_chat_apply_template)
    FUNC(llama_get_memory) FUNC(llama_memory_clear) FUNC(llama_tokenize) FUNC(llama_batch_get_one)
    FUNC(llama_decode) FUNC(llama_set_n_threads) FUNC(llama_sampler_init_greedy) FUNC(llama_sampler_free)
    FUNC(llama_sampler_sample) FUNC(llama_sampler_accept) FUNC(llama_vocab_is_eog) FUNC(llama_token_to_piece)
#undef FUNC
    bool load() {
        lib = dlopen("libllama.so", RTLD_NOW | RTLD_LOCAL);
        if (!lib) return false;
#define LOAD(name) name##_fn = reinterpret_cast<decltype(name##_fn)>(dlsym(lib, #name)); if (!name##_fn) return false;
        LOAD(llama_backend_init) LOAD(llama_print_system_info) LOAD(llama_model_default_params) LOAD(llama_context_default_params)
        LOAD(llama_model_load_from_file) LOAD(llama_model_free) LOAD(llama_init_from_model) LOAD(llama_free)
        LOAD(llama_model_get_vocab) LOAD(llama_model_chat_template) LOAD(llama_chat_apply_template)
        LOAD(llama_get_memory) LOAD(llama_memory_clear) LOAD(llama_tokenize) LOAD(llama_batch_get_one)
        LOAD(llama_decode) LOAD(llama_set_n_threads) LOAD(llama_sampler_init_greedy) LOAD(llama_sampler_free)
        LOAD(llama_sampler_sample) LOAD(llama_sampler_accept) LOAD(llama_vocab_is_eog) LOAD(llama_token_to_piece)
#undef LOAD
        return true;
    }
    ~Api() { if (lib) dlclose(lib); }
};
struct Session {
    Api api;
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    std::atomic<bool> cancelled{false};
    int max_threads = 1;
    int trials = 0;
    int best_threads = 1;
    long long sums[3] = {0, 0, 0};
    ~Session() {
        if (ctx) api.llama_free_fn(ctx);
        if (model) api.llama_model_free_fn(model);
    }
};
static bool abort_decode(void *p) { return static_cast<Session *>(p)->cancelled.load(); }
static void fail(JNIEnv *env, const char *message) {
    jclass cls = env->FindClass("java/io/IOException");
    if (cls) env->ThrowNew(cls, message);
}
static std::string bytes(JNIEnv *env, jbyteArray input) {
    jsize size = env->GetArrayLength(input);
    std::string out(size, '\0');
    if (size) env->GetByteArrayRegion(input, 0, size, reinterpret_cast<jbyte *>(&out[0]));
    return out;
}
static bool report(JNIEnv *env, jobject observer, jmethodID method, int phase, int generated, int prompt, int detail) {
    env->CallVoidMethod(observer, method, phase, generated, prompt, detail);
    return !env->ExceptionCheck();
}
extern "C" JNIEXPORT jlong JNICALL
Java_com_gmail_heagoo_apkeditor_translate_InstrumentedLlama_open(JNIEnv *env, jclass, jstring path, jint threads) {
    Session *s = new Session();
    if (!s->api.load()) { delete s; fail(env, "llama native symbols unavailable (engine version mismatch)"); return 0; }
    static std::once_flag backend_init;
    std::call_once(backend_init, [&] { s->api.llama_backend_init_fn(); });
    s->max_threads = threads;
    s->best_threads = threads;
    llama_model_params mp = s->api.llama_model_default_params_fn();
    static ggml_backend_dev_t no_devices[] = { nullptr };
    mp.devices = no_devices; mp.n_gpu_layers = 0;
    const char *p = env->GetStringUTFChars(path, nullptr);
    if (!p) { delete s; return 0; }
    s->model = s->api.llama_model_load_from_file_fn(p, mp);
    env->ReleaseStringUTFChars(path, p);
    if (!s->model) { delete s; fail(env, "Model load failed"); return 0; }
    llama_context_params cp = s->api.llama_context_default_params_fn();
    cp.n_ctx = 2048; cp.n_threads = threads; cp.n_threads_batch = threads; cp.op_offload = false;
    cp.abort_callback = abort_decode; cp.abort_callback_data = s;
    s->ctx = s->api.llama_init_from_model_fn(s->model, cp);
    if (!s->ctx) { delete s; fail(env, "Model context creation failed"); return 0; }
    return reinterpret_cast<jlong>(s);
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_gmail_heagoo_apkeditor_translate_InstrumentedLlama_systemInfo(JNIEnv *env, jclass, jlong handle) {
    Session *s = reinterpret_cast<Session *>(handle);
    if (!s) return nullptr;
    const char *info = s->api.llama_print_system_info_fn();
    return env->NewStringUTF(info ? info : "unavailable");
}
extern "C" JNIEXPORT void JNICALL
Java_com_gmail_heagoo_apkeditor_translate_InstrumentedLlama_cancel(JNIEnv *, jclass, jlong handle) {
    Session *s = reinterpret_cast<Session *>(handle); if (s) s->cancelled.store(true);
}
extern "C" JNIEXPORT void JNICALL
Java_com_gmail_heagoo_apkeditor_translate_InstrumentedLlama_close(JNIEnv *, jclass, jlong handle) {
    delete reinterpret_cast<Session *>(handle);
}
// Progress codes: 1=template, 2=tokenize, 3=prefill, 4=sample, 5=decode, 6=finished.
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_gmail_heagoo_apkeditor_translate_InstrumentedLlama_complete(JNIEnv *env, jclass, jlong handle,
        jbyteArray userBytes, jbyteArray systemBytes, jint maxTokens, jobject observer) {
    Session *s = reinterpret_cast<Session *>(handle);
    if (!s || !s->ctx || !observer) { fail(env, "No model session"); return nullptr; }
    jclass cls = env->GetObjectClass(observer);
    jmethodID callback = env->GetMethodID(cls, "onNativeProgress", "(IIII)V");
    if (!callback) return nullptr;
    Api &a = s->api;
    std::string user = bytes(env, userBytes), system = bytes(env, systemBytes), text;
    if (env->ExceptionCheck()) return nullptr;
    if (!report(env, observer, callback, 1, 0, 0, 0)) return nullptr;
    const char *tmpl = a.llama_model_chat_template_fn(s->model, nullptr);
    int templateStatus = 0;
    if (tmpl) {
        llama_chat_message messages[] = {{"system", system.c_str()}, {"user", user.c_str()}};
        std::vector<char> buffer(system.size() + user.size() + 2048);
        int n = a.llama_chat_apply_template_fn(tmpl, messages, 2, true, buffer.data(), (int32_t) buffer.size());
        if (n > (int) buffer.size()) {
            buffer.resize(n);
            n = a.llama_chat_apply_template_fn(tmpl, messages, 2, true, buffer.data(), (int32_t) buffer.size());
        }
        if (n >= 0 && n <= (int) buffer.size()) { text.assign(buffer.data(), n); templateStatus = 1; }
        else templateStatus = -1;
    }
    if (templateStatus != 1) text = system + "\n\n" + user;
    if (!report(env, observer, callback, 2, 0, 0, templateStatus)) return nullptr;
    const llama_vocab *vocab = a.llama_model_get_vocab_fn(s->model);
    int needed = -a.llama_tokenize_fn(vocab, text.data(), (int32_t) text.size(), nullptr, 0, true, true);
    if (needed <= 0 || needed > 1536) { fail(env, "Invalid or oversized prompt"); return nullptr; }
    std::vector<llama_token> tokens(needed);
    int n = a.llama_tokenize_fn(vocab, text.data(), (int32_t) text.size(), tokens.data(), needed, true, true);
    if (n <= 0) { fail(env, "Tokenization failed"); return nullptr; }
    tokens.resize(n);
    if (s->cancelled.load()) { fail(env, "Translation cancelled"); return nullptr; }
    a.llama_memory_clear_fn(a.llama_get_memory_fn(s->ctx), true);
    if (!report(env, observer, callback, 3, 0, n, 0)) return nullptr;
    int rc = a.llama_decode_fn(s->ctx, a.llama_batch_get_one_fn(tokens.data(), n));
    if (rc != 0) { report(env, observer, callback, 6, 0, n, rc); fail(env, s->cancelled.load() ? "Translation cancelled" : "Prompt decode failed"); return nullptr; }
    llama_sampler *sampler = a.llama_sampler_init_greedy_fn();
    if (!sampler) { fail(env, "Sampler creation failed"); return nullptr; }
    std::string output;
    int generated = 0;
    bool ended = false;
    for (; generated < maxTokens; ) {
        if (s->cancelled.load()) break;
        if (!report(env, observer, callback, 4, generated, n, 0)) break;
        llama_token token = a.llama_sampler_sample_fn(sampler, s->ctx, -1);
        if (a.llama_vocab_is_eog_fn(vocab, token)) { ended = true; break; }
        a.llama_sampler_accept_fn(sampler, token);
        char piece[256];
        int len = a.llama_token_to_piece_fn(vocab, token, piece, sizeof(piece), 0, true);
        if (len < 0) { rc = -100; break; }
        output.append(piece, len);
        ++generated;
        if (!report(env, observer, callback, 5, generated, n, 0)) break;
        // Compare two decode calls at each supported thread count on actual tokens.
        // Prefill keeps the configured batch thread count; no token is regenerated.
        const int candidates[] = {1, 2, s->max_threads};
        int candidateCount = s->max_threads >= 4 ? 3 : s->max_threads >= 2 ? 2 : 1;
        bool trial = s->trials < candidateCount * 2;
        int index = trial ? s->trials / 2 : 0;
        int chosen = trial ? candidates[index] : s->best_threads;
        a.llama_set_n_threads_fn(s->ctx, chosen, s->max_threads);
        auto started = std::chrono::steady_clock::now();
        rc = a.llama_decode_fn(s->ctx, a.llama_batch_get_one_fn(&token, 1));
        auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - started).count();
        if (rc != 0) break;
        if (trial) {
            s->sums[index] += ms;
            ++s->trials;
            if (!report(env, observer, callback, 7, chosen, generated, (int) ms)) break;
            if (s->trials == candidateCount * 2) {
                int best = 0;
                for (int i = 1; i < candidateCount; ++i)
                    if (s->sums[i] < s->sums[best]) best = i;
                s->best_threads = candidates[best];
                if (!report(env, observer, callback, 8, s->best_threads, generated, (int)(s->sums[best] / 2))) break;
            }
        }
    }
    a.llama_sampler_free_fn(sampler);
    if (env->ExceptionCheck()) return nullptr;
    if (s->cancelled.load()) { fail(env, "Translation cancelled"); return nullptr; }
    if (rc != 0) { report(env, observer, callback, 6, generated, n, rc); fail(env, "Token decode failed"); return nullptr; }
    if (!ended) { report(env, observer, callback, 6, generated, n, 1); fail(env, "Translation incomplete (token limit)"); return nullptr; }
    if (!report(env, observer, callback, 6, generated, n, 0)) return nullptr;
    jbyteArray result = env->NewByteArray((jsize) output.size());
    if (result && !output.empty()) env->SetByteArrayRegion(result, 0, (jsize) output.size(), reinterpret_cast<const jbyte *>(output.data()));
    return result;
}
