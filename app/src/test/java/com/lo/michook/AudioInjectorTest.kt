package com.lo.michook

import com.lo.michook.audio.AudioInjector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class AudioInjectorTest {

    @Test
    fun testFillBufferOutputsValidPcm() {
        val buffer = ByteArray(1024)
        val written = AudioInjector.fillBuffer(buffer, 0, buffer.size)

        assertEquals(1024, written)
        // Verify buffer is populated with non-zero audio samples (synthetic carrier wave or decoded stream)
        var nonZeroCount = 0
        for (b in buffer) {
            if (b != 0.toByte()) nonZeroCount++
        }
        assertTrue("Audio buffer must contain active sound data", nonZeroCount > 0)
    }

    @Test
    fun testFillShortBufferOutputsValidShorts() {
        val shortBuffer = ShortArray(512)
        val written = AudioInjector.fillShortBuffer(shortBuffer, 0, shortBuffer.size)

        assertEquals(512, written)
        var nonZeroCount = 0
        for (s in shortBuffer) {
            if (s != 0.toShort()) nonZeroCount++
        }
        assertTrue("Short audio buffer must contain active sound data", nonZeroCount > 0)
    }

    @Test
    fun testFillByteBuffer() {
        val byteBuf = ByteBuffer.allocateDirect(1024)
        val written = AudioInjector.fillByteBuffer(byteBuf, 1024)

        assertEquals(1024, written)
        assertEquals(0, byteBuf.position()) // Position must be preserved
    }

    @Test
    fun testVolumeScalingAndMute() {
        AudioInjector.isMuted = true
        val buffer = ByteArray(512) { 0xFF.toByte() }
        val written = AudioInjector.fillBuffer(buffer, 0, buffer.size)

        assertEquals(512, written)
        for (b in buffer) {
            assertEquals("Muted stream must be silence", 0.toByte(), b)
        }

        AudioInjector.isMuted = false
    }
}
