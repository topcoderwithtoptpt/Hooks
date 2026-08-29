package com.lo.michook.hooks

import android.util.Log
import com.lo.michook.MainHook.Companion.TAG
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * NativeHook
 *
 * JNI bridge into our native library (libmichook_native.so / ShadowHook).
 * Intercepts low-level native audio:
 *   • libaaudio.so   → AAudioStream_read / AAudioStream_write
 *   • libOpenSLES.so → OpenSL ES recording interfaces
 *   • libaudioclient → AudioRecord::read C++ methods
 */
object NativeHook {

    private var isNativeLoaded = false

    init {
        loadNativeLibrarySafely()
    }

    private fun loadNativeLibrarySafely(): Boolean {
        if (isNativeLoaded) return true
        return try {
            System.loadLibrary("michook_native")
            isNativeLoaded = true
            Log.d(TAG, "Native library libmichook_native.so loaded successfully ✓")
            true
        } catch (e: Throwable) {
            try {
                System.loadLibrary("shadowhook")
                System.loadLibrary("michook_native")
                isNativeLoaded = true
                Log.d(TAG, "Native library loaded with shadowhook dependency ✓")
                true
            } catch (t: Throwable) {
                Log.w(TAG, "Native hook library optional load notice: ${t.message}")
                isNativeLoaded = false
                false
            }
        }
    }

    // JNI declarations
    private external fun hookAAudio(): Boolean
    private external fun hookOpenSLES(): Boolean
    external fun unhookAll()

    fun install(lpparam: XC_LoadPackage.LoadPackageParam): Boolean {
        if (!isNativeLoaded) {
            loadNativeLibrarySafely()
        }

        if (!isNativeLoaded) {
            XposedBridge.log("$TAG → Native hooking bypassed (Java AudioRecord layer is fully active)")
            return false
        }

        return try {
            val aaudio = hookAAudio()
            val opensles = hookOpenSLES()

            XposedBridge.log(
                "$TAG → Native audio hooks: " +
                "AAudio=$aaudio, OpenSLES/AudioClient=$opensles " +
                "[${lpparam.packageName}]"
            )
            true
        } catch (e: Throwable) {
            XposedBridge.log("$TAG → Native hook notice: ${e.message}")
            Log.d(TAG, "Native hook note", e)
            false
        }
    }
}
