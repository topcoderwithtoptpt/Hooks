package com.lo.michook

import android.util.Log
import com.lo.michook.audio.AudioInjector
import com.lo.michook.hooks.AudioRecordHook
import com.lo.michook.hooks.MediaRecorderHook
import com.lo.michook.hooks.NativeHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

class MainHook : IXposedHookLoadPackage {

    companion object {
        const val TAG = "MicHook"

        // ── MP3 config ──────────────────────────────────────────────────────
        // Default path. Override by writing a new path to /sdcard/MicHook/config.txt
        const val DEFAULT_MP3 = "/sdcard/MicHook/inject.mp3"
        const val CONFIG_FILE = "/sdcard/MicHook/config.txt"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        // Never hook ourselves or the system
        if (lpparam.packageName == "com.lo.michook") return
        if (lpparam.packageName == "android") return

        XposedBridge.log("$TAG → Injected into: ${lpparam.packageName}")
        Log.d(TAG, "Process hooked: ${lpparam.packageName}")

        try {
            // Resolve MP3 path — config file overrides default
            val mp3Path = resolveMP3Path()
            XposedBridge.log("$TAG → Using MP3: $mp3Path")

            // Boot up the decoder before hooks fire
            AudioInjector.loadAndDecode(mp3Path)

            // ── Layer 1: Java AudioRecord (all variants) ────────────────────
            AudioRecordHook.install(lpparam)

            // ── Layer 2: Java MediaRecorder ─────────────────────────────────
            MediaRecorderHook.install(lpparam)

            // ── Layer 3: Native AAudio + OpenSL ES + libaudioclient ─────────
            NativeHook.install(lpparam)

            XposedBridge.log("$TAG → All hooks installed ✓")

        } catch (e: Throwable) {
            XposedBridge.log("$TAG → Hook installation failed: ${e.message}")
            Log.e(TAG, "Fatal hook error", e)
        }
    }

    private fun resolveMP3Path(): String {
        val config = File(CONFIG_FILE)
        return if (config.exists()) {
            val path = config.readText().trim()
            if (path.isNotEmpty()) path else DEFAULT_MP3
        } else {
            DEFAULT_MP3
        }
    }
}
