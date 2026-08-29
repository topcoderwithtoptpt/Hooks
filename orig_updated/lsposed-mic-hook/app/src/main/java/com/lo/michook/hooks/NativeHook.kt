package com.lo.michook.hooks

import android.util.Log
import com.lo.michook.MainHook.Companion.TAG
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * NativeHook
 *
 * JNI bridge into our native library (libmichook_native.so).
 * The .so uses ShadowHook to PLT-hook:
 *   • libaaudio.so   → AAudioStream_read / AAudioStream_write
 *   • libOpenSLES.so → SLAndroidSimpleBufferQueueItf enqueue
 *   • libaudioclient → AudioRecord::read (lowest Java-reachable layer)
 *
 * Injected PCM is pulled from AudioInjector.fillBuffer() via JNI callback.
 */
object NativeHook {

    init {
        try {
            System.loadLibrary("michook_native")
            Log.d(TAG, "Native lib loaded ✓")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native lib load failed: ${e.message}")
        }
    }

    // JNI declarations
    private external fun hookAAudio(): Boolean
    private external fun hookOpenSLES(): Boolean
    external fun unhookAll()

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val aaudio   = hookAAudio()
            val opensles = hookOpenSLES()

            XposedBridge.log(
                "$TAG → Native hooks: " +
                "AAudio=$aaudio, OpenSLES=$opensles " +
                "[${lpparam.packageName}]"
            )
        } catch (e: Throwable) {
            XposedBridge.log("$TAG → Native hook error: ${e.message}")
            Log.e(TAG, "Native hook error", e)
        }
    }
}
