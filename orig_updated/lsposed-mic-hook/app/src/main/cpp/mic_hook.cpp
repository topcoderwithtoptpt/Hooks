/**
 * mic_hook.cpp
 *
 * Native audio injection hooks using ShadowHook (ByteDance PLT/inline engine).
 * Hooks:
 *   1. libaaudio.so   → AAudioStream_read
 *   2. libaaudio.so   → AAudioStream_write
 *   3. libaudioclient → AudioRecord::read (mangled C++ symbol)
 *
 * PCM data is pulled from AudioInjector.fillBuffer() via JNI on every hook
 * invocation, so whatever the Kotlin decoder queued up is what the app hears.
 */

#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstring>
#include <cstdint>
#include <atomic>

// ShadowHook public header
// Place shadowhook/include/shadowhook.h in the path configured in CMakeLists
#include "shadowhook.h"

#define LOG_TAG "MicHook_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// ── Globals ──────────────────────────────────────────────────────────────────

static JavaVM* g_jvm = nullptr;

// ShadowHook stub handles — used for cleanup
static void* g_stub_aaudio_read       = nullptr;
static void* g_stub_aaudio_write      = nullptr;
static void* g_stub_audioclient_read  = nullptr;

static std::atomic<bool> g_hooks_active{false};

// ── JVM helpers ──────────────────────────────────────────────────────────────

struct JNIGuard {
    JNIEnv* env    = nullptr;
    bool attached  = false;

    JNIGuard() {
        if (!g_jvm) return;
        int st = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
        if (st == JNI_EDETACHED) {
            g_jvm->AttachCurrentThread(&env, nullptr);
            attached = true;
        }
    }
    ~JNIGuard() {
        if (attached && g_jvm) g_jvm->DetachCurrentThread();
    }
    bool ok() const { return env != nullptr; }
};

/**
 * Calls AudioInjector.fillBuffer(byte[], 0, byteCount) from native code.
 * Copies result into `dest`.
 */
static void inject_pcm(void* dest, int32_t numFrames, int bytesPerFrame = 2) {
    if (!dest || numFrames <= 0 || !g_jvm) return;

    JNIGuard guard;
    if (!guard.ok()) return;

    JNIEnv* env = guard.env;

    jclass cls = env->FindClass("com/lo/michook/audio/AudioInjector");
    if (!cls) { LOGE("AudioInjector class not found"); return; }

    jmethodID fillMethod = env->GetStaticMethodID(cls, "fillBuffer", "([BII)I");
    if (!fillMethod) {
        LOGE("fillBuffer method not found");
        env->DeleteLocalRef(cls);
        return;
    }

    int byteCount = numFrames * bytesPerFrame;
    jbyteArray jbuf = env->NewByteArray(byteCount);
    if (!jbuf) {
        env->DeleteLocalRef(cls);
        return;
    }

    env->CallStaticIntMethod(cls, fillMethod, jbuf, 0, byteCount);

    // Copy JVM bytes → native buffer
    env->GetByteArrayRegion(jbuf, 0, byteCount, reinterpret_cast<jbyte*>(dest));

    env->DeleteLocalRef(jbuf);
    env->DeleteLocalRef(cls);

    LOGD("inject_pcm: wrote %d bytes (%d frames)", byteCount, numFrames);
}

// ── AAudio type definitions ───────────────────────────────────────────────────

typedef int32_t aaudio_result_t;

typedef aaudio_result_t (*fn_AAudioStream_read)(
    void*    stream,
    void*    buffer,
    int32_t  numFrames,
    int64_t  timeoutNanoseconds
);

typedef aaudio_result_t (*fn_AAudioStream_write)(
    void*       stream,
    const void* buffer,
    int32_t     numFrames,
    int64_t     timeoutNanoseconds
);

// ── AAudioStream_read hook ────────────────────────────────────────────────────

static aaudio_result_t hook_AAudioStream_read(
    void*   stream,
    void*   buffer,
    int32_t numFrames,
    int64_t timeoutNanoseconds
) {
    // Call original — lets AAudio manage buffer state / stream health
    fn_AAudioStream_read original =
        (fn_AAudioStream_read) shadowhook_get_prev_func(g_stub_aaudio_read);

    aaudio_result_t result = original(stream, buffer, numFrames, timeoutNanoseconds);

    // Replace whatever the mic captured with our MP3 PCM
    if (result > 0 && buffer) {
        inject_pcm(buffer, result, /*bytesPerFrame=*/2);
        LOGI("AAudioStream_read → injected %d frames", result);
    }

    return result;
}

// ── AAudioStream_write hook (optional — log only) ─────────────────────────────

static aaudio_result_t hook_AAudioStream_write(
    void*       stream,
    const void* buffer,
    int32_t     numFrames,
    int64_t     timeoutNanoseconds
) {
    fn_AAudioStream_write original =
        (fn_AAudioStream_write) shadowhook_get_prev_func(g_stub_aaudio_write);

    LOGD("AAudioStream_write: %d frames", numFrames);
    return original(stream, buffer, numFrames, timeoutNanoseconds);
}

// ── libaudioclient AudioRecord::read hook ─────────────────────────────────────
// Mangled symbol: _ZN7android11AudioRecord4readEPvjb
// Signature:      ssize_t AudioRecord::read(void* buffer, size_t userSize, bool blocking)

typedef int32_t (*fn_AudioRecord_read)(
    void*    self,
    void*    buffer,
    uint32_t userSize,
    bool     blocking
);

static int32_t hook_AudioRecord_read(
    void*    self,
    void*    buffer,
    uint32_t userSize,
    bool     blocking
) {
    fn_AudioRecord_read original =
        (fn_AudioRecord_read) shadowhook_get_prev_func(g_stub_audioclient_read);

    int32_t result = original(self, buffer, userSize, blocking);

    if (result > 0 && buffer) {
        // PCM16 mono: 2 bytes per frame
        int32_t frames = result / 2;
        inject_pcm(buffer, frames, 2);
        LOGI("AudioRecord::read (native) → injected %d bytes", result);
    }

    return result;
}

// ── Install functions ─────────────────────────────────────────────────────────

static bool install_aaudio_hooks() {
    g_stub_aaudio_read = shadowhook_hook_sym_name(
        "libaaudio.so",
        "AAudioStream_read",
        (void*) hook_AAudioStream_read,
        nullptr
    );

    if (!g_stub_aaudio_read) {
        LOGE("AAudioStream_read hook failed: %s",
             shadowhook_to_errmsg(shadowhook_get_errno()));
        return false;
    }

    g_stub_aaudio_write = shadowhook_hook_sym_name(
        "libaaudio.so",
        "AAudioStream_write",
        (void*) hook_AAudioStream_write,
        nullptr
    );

    LOGI("AAudio hooks installed ✓ (read=%p, write=%p)",
         g_stub_aaudio_read, g_stub_aaudio_write);
    return true;
}

static bool install_audioclient_hook() {
    // Try mangled C++ symbol — works on AOSP and most vendor ROMs
    g_stub_audioclient_read = shadowhook_hook_sym_name(
        "libaudioclient.so",
        "_ZN7android11AudioRecord4readEPvjb",
        (void*) hook_AudioRecord_read,
        nullptr
    );

    if (!g_stub_audioclient_read) {
        LOGE("libaudioclient AudioRecord::read hook failed: %s",
             shadowhook_to_errmsg(shadowhook_get_errno()));
        return false;
    }

    LOGI("libaudioclient AudioRecord::read hook installed ✓");
    return true;
}

// ── JNI_OnLoad ───────────────────────────────────────────────────────────────

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    g_jvm = vm;
    LOGI("JNI_OnLoad — MicHook native ready");
    return JNI_VERSION_1_6;
}

// ── Exported JNI functions (called from NativeHook.kt) ───────────────────────

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_lo_michook_hooks_NativeHook_hookAAudio(JNIEnv* env, jobject thiz) {
    if (g_hooks_active.load()) {
        LOGI("Hooks already active — skipping re-init");
        return JNI_TRUE;
    }

    // Init ShadowHook — UNIQUE mode preferred; fall back to SHARED
    int init_rc = shadowhook_init(SHADOWHOOK_MODE_UNIQUE, false);
    if (init_rc != 0) {
        LOGE("shadowhook_init(UNIQUE) failed (%d), trying SHARED", init_rc);
        init_rc = shadowhook_init(SHADOWHOOK_MODE_SHARED, false);
    }

    if (init_rc != 0) {
        LOGE("shadowhook_init failed entirely: %d", init_rc);
        return JNI_FALSE;
    }

    bool ok = install_aaudio_hooks();
    if (ok) g_hooks_active.store(true);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_lo_michook_hooks_NativeHook_hookOpenSLES(JNIEnv* env, jobject thiz) {
    // Install the deeper libaudioclient hook — covers OpenSL ES paths too
    bool ok = install_audioclient_hook();
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_lo_michook_hooks_NativeHook_unhookAll(JNIEnv* env, jobject thiz) {
    if (g_stub_aaudio_read)      shadowhook_unhook(g_stub_aaudio_read);
    if (g_stub_aaudio_write)     shadowhook_unhook(g_stub_aaudio_write);
    if (g_stub_audioclient_read) shadowhook_unhook(g_stub_audioclient_read);

    g_stub_aaudio_read       = nullptr;
    g_stub_aaudio_write      = nullptr;
    g_stub_audioclient_read  = nullptr;
    g_hooks_active.store(false);

    LOGI("All native hooks removed ✓");
}

} // extern "C"
