package com.lo.michook.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

/**
 * AudioInjector
 *
 * Decodes an MP3 file to raw PCM16 in a background daemon thread,
 * fills a lock-free queue, and exposes fill*() helpers that hooks
 * call instead of zeroing the mic buffer.
 *
 * The file loops indefinitely so the stream never runs dry.
 */
object AudioInjector {

    private const val TAG = "MicHook_Injector"

    // 200-chunk queue — roughly ~1–2 sec of audio at 44100 Hz mono
    val pcmQueue = LinkedBlockingQueue<ByteArray>(200)

    @Volatile var isReady    = false
    @Volatile private var isDecoding = false

    // ── fillBuffer ───────────────────────────────────────────────────────────
    // Called from Java hooks — fills a ByteArray with MP3 PCM
    @JvmStatic
    fun fillBuffer(dest: ByteArray, offset: Int, size: Int): Int {
        if (!isReady) {
            dest.fill(0, offset, offset + size)
            return size
        }

        var filled = 0
        while (filled < size) {
            val chunk = pcmQueue.poll() ?: break
            val toCopy = minOf(chunk.size, size - filled)
            System.arraycopy(chunk, 0, dest, offset + filled, toCopy)
            filled += toCopy

            // Leftover bytes go back to front of queue
            if (toCopy < chunk.size) {
                pcmQueue.offerFirst(chunk.copyOfRange(toCopy, chunk.size))
            }
        }

        // Pad with silence if queue ran dry momentarily
        if (filled < size) {
            dest.fill(0, offset + filled, offset + size)
        }

        return size
    }

    // ── fillShortBuffer ──────────────────────────────────────────────────────
    // For AudioRecord.read(short[], int, int)
    @JvmStatic
    fun fillShortBuffer(dest: ShortArray, offset: Int, size: Int) {
        val bytes = ByteArray(size * 2)
        fillBuffer(bytes, 0, bytes.size)
        for (i in 0 until size) {
            val lo = bytes[i * 2].toInt() and 0xFF
            val hi = bytes[i * 2 + 1].toInt()
            dest[offset + i] = ((hi shl 8) or lo).toShort()
        }
    }

    // ── fillByteBuffer ───────────────────────────────────────────────────────
    // For AudioRecord.read(ByteBuffer, int)
    @JvmStatic
    fun fillByteBuffer(dest: ByteBuffer, numBytes: Int) {
        val bytes = ByteArray(numBytes)
        fillBuffer(bytes, 0, numBytes)
        val pos = dest.position()
        dest.put(bytes, 0, numBytes)
        dest.position(pos)
    }

    // ── loadAndDecode ────────────────────────────────────────────────────────
    // Kick off background decode. Safe to call multiple times — idempotent.
    fun loadAndDecode(mp3Path: String) {
        if (isDecoding) return
        isDecoding = true

        Thread({
            decodeLoop(mp3Path)
        }, "MicHook-Decoder").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    // ── decodeLoop ───────────────────────────────────────────────────────────
    // Infinite loop: decode MP3 → push PCM → seek back → repeat
    private fun decodeLoop(path: String) {
        val file = File(path)
        if (!file.exists()) {
            Log.e(TAG, "MP3 not found: $path")
            isDecoding = false
            return
        }

        Log.i(TAG, "Starting decode loop: $path")

        while (true) {
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(path)

                // Find the first audio track
                var trackIndex = -1
                var format: MediaFormat? = null

                for (i in 0 until extractor.trackCount) {
                    val fmt  = extractor.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        trackIndex = i
                        format = fmt
                        break
                    }
                }

                if (trackIndex < 0 || format == null) {
                    Log.e(TAG, "No audio track found in: $path")
                    Thread.sleep(2000)
                    continue
                }

                extractor.selectTrack(trackIndex)

                val mime  = format.getString(MediaFormat.KEY_MIME)!!
                val codec = MediaCodec.createDecoderByType(mime)
                codec.configure(format, null, null, 0)
                codec.start()

                val bufferInfo = MediaCodec.BufferInfo()
                var sawEOS     = false

                while (!sawEOS) {
                    // ── Feed input ───────────────────────────────────────────
                    val inIdx = codec.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf      = codec.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)

                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inIdx, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawEOS = true
                        } else {
                            codec.queueInputBuffer(
                                inIdx, 0, sampleSize,
                                extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                    }

                    // ── Drain output → PCM ───────────────────────────────────
                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                    if (outIdx >= 0) {
                        val outBuf = codec.getOutputBuffer(outIdx)!!

                        if (bufferInfo.size > 0) {
                            val pcm = ByteArray(bufferInfo.size)
                            outBuf.get(pcm)

                            // Blocking put — natural backpressure if queue is full
                            while (!pcmQueue.offer(pcm)) {
                                Thread.sleep(5)
                            }
                        }

                        codec.releaseOutputBuffer(outIdx, false)
                    }
                }

                codec.stop()
                codec.release()
                extractor.release()

                isReady = true
                Log.i(TAG, "Loop complete — restarting MP3")

            } catch (e: Exception) {
                Log.e(TAG, "Decode error: ${e.message}", e)
                Thread.sleep(1000)
            }
        }
    }
}
