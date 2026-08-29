package com.lo.michook

import com.lo.michook.status.LSPosedStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LSPosedStatusTest {

    @Test
    fun testDefaultLSPosedStatusWhenNotInHookedProcess() {
        val report = LSPosedStatus.checkRealtimeStatus()
        assertNotNull(report)
        assertNotNull(report.timestamp)
        assertNotNull(report.frameworkName)
        assertNotNull(report.statusMessage)
    }

    @Test
    fun testIsModuleActiveDefaultReturnsFalse() {
        // In local JVM test without Xposed method replacement, default is false
        val active = LSPosedStatus.isModuleActive()
        assertEquals(false, active)
    }
}
