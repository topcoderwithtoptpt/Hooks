package com.lo.michook.hooks

import android.util.Log
import com.lo.michook.MainHook.Companion.TAG
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * MediaRecorderHook
 *
 * MediaRecorder doesn't expose raw PCM buffers — it records directly
 * to a file or fd. We intercept at setAudioSource / start and can
 * redirect the source to REMOTE_SUBMIX (which picks up MediaPlayer
 * output or silence) instead of the real mic.
 *
 * For full PCM injection via MediaRecorder the cleanest approach is
 * to redirect to a virtual audio source. Paired with AudioRecord hooks
 * this covers all recording paths.
 */
object MediaRecorderHook {

    // AudioSource.REMOTE_SUBMIX = 8 — routes to audio mix, not mic
    private const val REMOTE_SUBMIX = 8

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {

        // ── Hook: setAudioSource(int) ─────────────────────────────────────────
        XposedHelpers.findAndHookMethod(
            "android.media.MediaRecorder",
            lpparam.classLoader,
            "setAudioSource",
            Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val original = param.args[0] as Int
                    XposedBridge.log(
                        "$TAG → MediaRecorder.setAudioSource($original) " +
                        "→ redirecting to REMOTE_SUBMIX"
                    )
                    // Redirect to REMOTE_SUBMIX — captures loopback/mix, not mic
                    param.args[0] = REMOTE_SUBMIX
                }
            }
        )

        // ── Hook: start() ────────────────────────────────────────────────────
        XposedHelpers.findAndHookMethod(
            "android.media.MediaRecorder",
            lpparam.classLoader,
            "start",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("$TAG → MediaRecorder.start() intercepted")
                }
            }
        )

        // ── Hook: prepare() ──────────────────────────────────────────────────
        XposedHelpers.findAndHookMethod(
            "android.media.MediaRecorder",
            lpparam.classLoader,
            "prepare",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("$TAG → MediaRecorder.prepare() intercepted")
                }
            }
        )

        Log.d(TAG, "MediaRecorder hooks installed ✓")
    }
}
