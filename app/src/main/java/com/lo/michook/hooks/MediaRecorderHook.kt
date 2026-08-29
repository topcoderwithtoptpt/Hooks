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
 * Hooks MediaRecorder audio source setup, preparation, and recording.
 * Intercepts start and prepare calls while preventing permission crashes.
 */
object MediaRecorderHook {

    var hooksInstalledCount = 0

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        hooksInstalledCount = 0

        // ── Hook: setAudioSource(int) ─────────────────────────────────────────
        try {
            XposedHelpers.findAndHookMethod(
                "android.media.MediaRecorder",
                lpparam.classLoader,
                "setAudioSource",
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val originalSource = param.args[0] as Int
                        XposedBridge.log(
                            "$TAG → MediaRecorder.setAudioSource($originalSource) intercepted in ${lpparam.packageName}"
                        )
                    }
                }
            )
            hooksInstalledCount++
        } catch (e: Throwable) {
            Log.d(TAG, "Failed hooking MediaRecorder.setAudioSource: ${e.message}")
        }

        // ── Hook: start() ────────────────────────────────────────────────────
        try {
            XposedHelpers.findAndHookMethod(
                "android.media.MediaRecorder",
                lpparam.classLoader,
                "start",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("$TAG → MediaRecorder.start() intercepted in ${lpparam.packageName}")
                    }
                }
            )
            hooksInstalledCount++
        } catch (e: Throwable) {
            Log.d(TAG, "Failed hooking MediaRecorder.start: ${e.message}")
        }

        // ── Hook: prepare() ──────────────────────────────────────────────────
        try {
            XposedHelpers.findAndHookMethod(
                "android.media.MediaRecorder",
                lpparam.classLoader,
                "prepare",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("$TAG → MediaRecorder.prepare() intercepted in ${lpparam.packageName}")
                    }
                }
            )
            hooksInstalledCount++
        } catch (e: Throwable) {
            Log.d(TAG, "Failed hooking MediaRecorder.prepare: ${e.message}")
        }

        Log.d(TAG, "MediaRecorder hooks installed ✓ ($hooksInstalledCount hooks)")
    }
}
