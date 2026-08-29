package com.lo.michook

import android.util.Log
import com.lo.michook.audio.AudioInjector
import com.lo.michook.audio.ConfigManager
import com.lo.michook.hooks.AudioRecordHook
import com.lo.michook.hooks.MediaRecorderHook
import com.lo.michook.hooks.NativeHook
import com.lo.michook.status.LSPosedStatus
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    companion object {
        const val TAG = "MicHook"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // ── Real-time Activation Check for our own module package ───────────
        if (lpparam.packageName == "com.lo.michook" ||
            lpparam.packageName == "com.aistudio.michook.zqxk" ||
            lpparam.packageName.endsWith(".michook")
        ) {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.lo.michook.status.LSPosedStatus",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
                )
                XposedHelpers.findAndHookMethod(
                    "com.lo.michook.status.LSPosedStatus",
                    lpparam.classLoader,
                    "getFrameworkName",
                    XC_MethodReplacement.returnConstant("LSPosed Framework (Active)")
                )
                XposedHelpers.findAndHookMethod(
                    "com.lo.michook.status.LSPosedStatus",
                    lpparam.classLoader,
                    "getActivationTimestamp",
                    XC_MethodReplacement.returnConstant(System.currentTimeMillis())
                )
                XposedBridge.log("$TAG → Real-time module activation hook applied to ${lpparam.packageName} ✓")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to hook module activation status check", e)
            }
            return
        }

        // Never hook core system services
        if (lpparam.packageName == "android") return
        if (lpparam.packageName == "com.android.systemui") return

        XposedBridge.log("$TAG → Injected into: ${lpparam.packageName}")
        Log.d(TAG, "Process hooked: ${lpparam.packageName}")

        try {
            // Resolve audio file path — config file overrides default
            val audioPath = ConfigManager.getEffectivePath()
            XposedBridge.log("$TAG → Using audio source: $audioPath")

            // Boot up the decoder before hooks fire
            AudioInjector.loadAndDecode(audioPath)

            // ── Layer 1: Java AudioRecord (all 8 read variants) ─────────────
            AudioRecordHook.install(lpparam)

            // ── Layer 2: Java MediaRecorder (redirect to REMOTE_SUBMIX) ─────
            MediaRecorderHook.install(lpparam)

            // ── Layer 3: Native AAudio + OpenSL ES + libaudioclient ─────────
            NativeHook.install(lpparam)

            XposedBridge.log("$TAG → All hooks initialized successfully for ${lpparam.packageName} ✓")

        } catch (e: Throwable) {
            XposedBridge.log("$TAG → Hook installation failed: ${e.message}")
            Log.e(TAG, "Fatal hook initialization error", e)
        }
    }
}
