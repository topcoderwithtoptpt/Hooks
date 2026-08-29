package com.lo.michook.hooks

import android.util.Log
import com.lo.michook.MainHook.Companion.TAG
import com.lo.michook.audio.AudioInjector
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.nio.ByteBuffer

/**
 * AudioRecordHook
 *
 * Intercepts every AudioRecord.read() overload BEFORE the original runs.
 * We set param.result ourselves, which skips the real mic read entirely.
 * The buffer is filled with PCM decoded from the injected MP3.
 */
object AudioRecordHook {

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {

        // ── Hook 1: read(byte[], int, int) ───────────────────────────────────
        XposedHelpers.findAndHookMethod(
            "android.media.AudioRecord",
            lpparam.classLoader,
            "read",
            ByteArray::class.java,
            Int::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val buffer = param.args[0] as? ByteArray ?: return
                    val offset = param.args[1] as Int
                    val size   = param.args[2] as Int
                    val written = AudioInjector.fillBuffer(buffer, offset, size)
                    param.result = written
                    XposedBridge.log("$TAG → read(byte[]) injected $written bytes")
                }
            }
        )

        // ── Hook 2: read(byte[], int, int, int) — API 23+ readMode flag ─────
        XposedHelpers.findAndHookMethod(
            "android.media.AudioRecord",
            lpparam.classLoader,
            "read",
            ByteArray::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val buffer = param.args[0] as? ByteArray ?: return
                    val offset = param.args[1] as Int
                    val size   = param.args[2] as Int
                    val written = AudioInjector.fillBuffer(buffer, offset, size)
                    param.result = written
                    XposedBridge.log("$TAG → read(byte[], readMode) injected $written bytes")
                }
            }
        )

        // ── Hook 3: read(short[], int, int) ─────────────────────────────────
        XposedHelpers.findAndHookMethod(
            "android.media.AudioRecord",
            lpparam.classLoader,
            "read",
            ShortArray::class.java,
            Int::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val buffer = param.args[0] as? ShortArray ?: return
                    val offset = param.args[1] as Int
                    val size   = param.args[2] as Int
                    AudioInjector.fillShortBuffer(buffer, offset, size)
                    param.result = size
                    XposedBridge.log("$TAG → read(short[]) injected $size shorts")
                }
            }
        )

        // ── Hook 4: read(short[], int, int, int) — readMode variant ─────────
        XposedHelpers.findAndHookMethod(
            "android.media.AudioRecord",
            lpparam.classLoader,
            "read",
            ShortArray::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val buffer = param.args[0] as? ShortArray ?: return
                    val offset = param.args[1] as Int
                    val size   = param.args[2] as Int
                    AudioInjector.fillShortBuffer(buffer, offset, size)
                    param.result = size
                }
            }
        )

        // ── Hook 5: read(ByteBuffer, int) — used by many NDK wrappers ───────
        XposedHelpers.findAndHookMethod(
            "android.media.AudioRecord",
            lpparam.classLoader,
            "read",
            ByteBuffer::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val buffer = param.args[0] as? ByteBuffer ?: return
                    val size   = param.args[1] as Int
                    AudioInjector.fillByteBuffer(buffer, size)
                    param.result = size
                    XposedBridge.log("$TAG → read(ByteBuffer) injected $size bytes")
                }
            }
        )

        // ── Hook 6: read(ByteBuffer, int, int) — readMode variant ───────────
        XposedHelpers.findAndHookMethod(
            "android.media.AudioRecord",
            lpparam.classLoader,
            "read",
            ByteBuffer::class.java,
            Int::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val buffer = param.args[0] as? ByteBuffer ?: return
                    val size   = param.args[1] as Int
                    AudioInjector.fillByteBuffer(buffer, size)
                    param.result = size
                }
            }
        )

        // ── Hook 7: read(float[], int, int, int) — float PCM (API 23+) ──────
        XposedHelpers.findAndHookMethod(
            "android.media.AudioRecord",
            lpparam.classLoader,
            "read",
            FloatArray::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val buffer = param.args[0] as? FloatArray ?: return
                    val offset = param.args[1] as Int
                    val size   = param.args[2] as Int

                    // Convert PCM16 → float [-1.0, 1.0]
                    val bytes = ByteArray(size * 2)
                    AudioInjector.fillBuffer(bytes, 0, bytes.size)
                    for (i in 0 until size) {
                        val lo    = bytes[i * 2].toInt() and 0xFF
                        val hi    = bytes[i * 2 + 1].toInt()
                        val s     = ((hi shl 8) or lo).toShort()
                        buffer[offset + i] = s / 32768.0f
                    }
                    param.result = size
                    XposedBridge.log("$TAG → read(float[]) injected $size floats")
                }
            }
        )

        // ── Hook 8: startRecording — log only, let it proceed ───────────────
        XposedHelpers.findAndHookMethod(
            "android.media.AudioRecord",
            lpparam.classLoader,
            "startRecording",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("$TAG → AudioRecord.startRecording() intercepted")
                }
            }
        )

        Log.d(TAG, "AudioRecord hooks installed ✓ (8 hooks)")
    }
}
