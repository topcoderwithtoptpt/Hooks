package com.lo.michook.status

import android.os.SystemClock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LSPosedStatus
 *
 * Real-time framework activation detector for MicHook LSPosed module.
 *
 * The canonical method isModuleActive() returns false by default.
 * When MicHook is enabled and active in LSPosed, MainHook hooks this
 * method to return true.
 */
object LSPosedStatus {

    /**
     * Hooked dynamically by LSPosed/XposedBridge to return true when active.
     * If LSPosed is disabled or not hooked, this returns false.
     */
    @JvmStatic
    fun isModuleActive(): Boolean {
        return false
    }

    /**
     * Hooked to return active framework name and details.
     */
    @JvmStatic
    fun getFrameworkName(): String {
        return "Inactive / Disabled"
    }

    /**
     * Hooked to return the timestamp when the hook was injected.
     */
    @JvmStatic
    fun getActivationTimestamp(): Long {
        return 0L
    }

    /**
     * Comprehensive real-time check result.
     */
    data class StatusReport(
        val isActivated: Boolean,
        val frameworkName: String,
        val statusMessage: String,
        val timestamp: String,
        val checkEpochMs: Long = System.currentTimeMillis()
    )

    fun checkRealtimeStatus(): StatusReport {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTimeStr = timeFormat.format(Date())

        // 1. Primary check: Xposed method hook replacement
        val hookActive = isModuleActive()
        if (hookActive) {
            val name = getFrameworkName()
            return StatusReport(
                isActivated = true,
                frameworkName = if (name.isNotBlank() && name != "Inactive / Disabled") name else "LSPosed Active",
                statusMessage = "Plugin is enabled and active in LSPosed. Audio interception ready.",
                timestamp = currentTimeStr
            )
        }

        // 2. Secondary check: Inspect if XposedBridge or LSPosed environment is reachable
        val envDetected = detectXposedEnvironment()
        if (envDetected) {
            return StatusReport(
                isActivated = false,
                frameworkName = "LSPosed Framework Detected",
                statusMessage = "LSPosed is present on device, but MicHook module is not enabled or needs app restart.",
                timestamp = currentTimeStr
            )
        }

        return StatusReport(
            isActivated = false,
            frameworkName = "LSPosed Disabled",
            statusMessage = "Module is disabled. Enable MicHook in LSPosed Manager and restart target apps.",
            timestamp = currentTimeStr
        )
    }

    private fun detectXposedEnvironment(): Boolean {
        return try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            true
        } catch (_: Throwable) {
            // Check known LSPosed / Magisk / Zygisk files
            val paths = listOf(
                "/system/framework/edxp.jar",
                "/data/adb/lspd",
                "/data/adb/modules/zygisk_lsposed",
                "/data/adb/modules/riru_lsposed"
            )
            paths.any { File(it).exists() }
        }
    }
}
