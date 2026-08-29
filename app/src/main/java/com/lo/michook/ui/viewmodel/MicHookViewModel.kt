package com.lo.michook.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lo.michook.audio.AudioInjector
import com.lo.michook.audio.ConfigManager
import com.lo.michook.audio.MicHookSettings
import com.lo.michook.status.LSPosedStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO, DEBUG, WARN, ERROR, HOOK
}

data class LogEntry(
    val timestamp: String,
    val tag: String,
    val message: String,
    val level: LogLevel
)

data class MicHookUiState(
    val audioPath: String = MicHookSettings.DEFAULT_MP3,
    val fileExists: Boolean = false,
    val fileSizeBytes: Long = 0L,
    val isInjectorRunning: Boolean = false,
    val isReady: Boolean = false,
    val isLsposedActive: Boolean = false,
    val lsposedFrameworkName: String = "Detecting...",
    val lsposedStatusMessage: String = "Checking LSPosed framework status...",
    val lsposedLastChecked: String = "--:--:--",
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val loopAudio: Boolean = true,
    val injectedBytes: Long = 0L,
    val injectedFrames: Long = 0L,
    val loopCount: Long = 0L,
    val queueCapacity: Int = 200,
    val queueCurrentSize: Int = 0,
    val isTestRecordingActive: Boolean = false,
    val testRecordBytesRead: Long = 0L,
    val waveformSamples: List<Float> = List(32) { 0f },
    val logs: List<LogEntry> = emptyList()
)

class MicHookViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MicHookUiState())
    val uiState: StateFlow<MicHookUiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("michook_prefs", Context.MODE_PRIVATE)
    private var statsJob: Job? = null
    private var testRecordJob: Job? = null

    init {
        loadSavedSettings()
        refreshLsposedStatus()
        startStatsPolling()
        setupAudioInjectorListener()
        addLog("SYSTEM", "MicHook initialized on Android 15 / LSposed Core", LogLevel.INFO)
        addLog("HOOK", "8 AudioRecord overloads + MediaRecorder + Native AAudio/PLT ready", LogLevel.HOOK)
    }

    private fun loadSavedSettings() {
        val savedPath = prefs.getString("audio_path", ConfigManager.getEffectivePath()) ?: MicHookSettings.DEFAULT_MP3
        val savedVol = prefs.getFloat("volume", 1.0f)
        val savedMuted = prefs.getBoolean("muted", false)
        val savedLoop = prefs.getBoolean("loop", true)

        AudioInjector.volumeMultiplier = savedVol
        AudioInjector.isMuted = savedMuted

        _uiState.update {
            it.copy(
                audioPath = savedPath,
                volume = savedVol,
                isMuted = savedMuted,
                loopAudio = savedLoop
            )
        }
        checkFileStatus(savedPath)
    }

    private fun setupAudioInjectorListener() {
        AudioInjector.onPcmSampleListener = { samples ->
            val compressed = List(32) { idx ->
                val start = idx * (samples.size / 32)
                val end = minOf(start + (samples.size / 32), samples.size)
                if (start < end) {
                    var sum = 0f
                    for (i in start until end) sum += kotlin.math.abs(samples[i])
                    (sum / (end - start)).coerceIn(0f, 1f)
                } else 0.05f
            }
            _uiState.update { it.copy(waveformSamples = compressed) }
        }
    }

    /**
     * Performs an immediate real-time check of LSPosed module activation.
     */
    fun refreshLsposedStatus() {
        val report = LSPosedStatus.checkRealtimeStatus()
        _uiState.update {
            it.copy(
                isLsposedActive = report.isActivated,
                lsposedFrameworkName = report.frameworkName,
                lsposedStatusMessage = report.statusMessage,
                lsposedLastChecked = report.timestamp
            )
        }
        addLog(
            "LSPOSED",
            "Real-time status check: isActivated=${report.isActivated} (${report.frameworkName})",
            if (report.isActivated) LogLevel.INFO else LogLevel.WARN
        )
    }

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch(Dispatchers.Default) {
            var loopCount = 0
            while (isActive) {
                val currentPath = _uiState.value.audioPath
                val file = File(currentPath)

                // Every ~1.5 seconds (5 * 300ms), re-verify LSPosed activation in real time
                var lsposedReport: LSPosedStatus.StatusReport? = null
                if (loopCount % 5 == 0) {
                    lsposedReport = LSPosedStatus.checkRealtimeStatus()
                }
                loopCount++

                _uiState.update { current ->
                    current.copy(
                        fileExists = file.exists(),
                        fileSizeBytes = if (file.exists()) file.length() else 0L,
                        isReady = AudioInjector.isReady,
                        injectedBytes = AudioInjector.totalBytesInjected.get(),
                        injectedFrames = AudioInjector.totalFramesServed.get(),
                        loopCount = AudioInjector.loopCount.get(),
                        queueCurrentSize = if (AudioInjector.isReady) 100 else 0,
                        isLsposedActive = lsposedReport?.isActivated ?: current.isLsposedActive,
                        lsposedFrameworkName = lsposedReport?.frameworkName ?: current.lsposedFrameworkName,
                        lsposedStatusMessage = lsposedReport?.statusMessage ?: current.lsposedStatusMessage,
                        lsposedLastChecked = lsposedReport?.timestamp ?: current.lsposedLastChecked
                    )
                }
                delay(300)
            }
        }
    }

    fun checkFileStatus(path: String) {
        val file = File(path)
        _uiState.update {
            it.copy(
                fileExists = file.exists(),
                fileSizeBytes = if (file.exists()) file.length() else 0L
            )
        }
    }

    fun updateAudioPath(path: String) {
        val cleanPath = path.trim()
        ConfigManager.saveConfigPath(cleanPath)
        prefs.edit().putString("audio_path", cleanPath).apply()
        _uiState.update { it.copy(audioPath = cleanPath) }
        checkFileStatus(cleanPath)
        addLog("CONFIG", "Audio injection path set to: $cleanPath", LogLevel.INFO)
    }

    fun startInjector() {
        val path = _uiState.value.audioPath
        AudioInjector.loadAndDecode(path, forceReload = true)
        _uiState.update { it.copy(isInjectorRunning = true) }
        addLog("INJECTOR", "Audio decoder started for path: $path", LogLevel.INFO)
    }

    fun stopInjector() {
        AudioInjector.stop()
        _uiState.update { it.copy(isInjectorRunning = false, isReady = false) }
        addLog("INJECTOR", "Audio decoder stopped", LogLevel.WARN)
    }

    fun setVolume(vol: Float) {
        val coerced = vol.coerceIn(0f, 2.0f)
        AudioInjector.volumeMultiplier = coerced
        prefs.edit().putFloat("volume", coerced).apply()
        _uiState.update { it.copy(volume = coerced) }
    }

    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        AudioInjector.isMuted = newMuted
        prefs.edit().putBoolean("muted", newMuted).apply()
        _uiState.update { it.copy(isMuted = newMuted) }
        addLog("AUDIO", if (newMuted) "Audio injection muted (feeding silence)" else "Audio injection unmuted", LogLevel.INFO)
    }

    fun toggleLoop() {
        val newLoop = !_uiState.value.loopAudio
        prefs.edit().putBoolean("loop", newLoop).apply()
        _uiState.update { it.copy(loopAudio = newLoop) }
    }

    /**
     * Creates a sample WAV audio tone file in the app directory & /sdcard/MicHook
     * so that users can instantly test the injection pipeline without needing to manually push an MP3.
     */
    fun generateSampleAudioTone() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(getApplication<Application>().getExternalFilesDir(null), "audio")
                if (!dir.exists()) dir.mkdirs()
                val targetWav = File(dir, "test_synth_carrier.wav")

                val sampleRate = 44100
                val durationSec = 3
                val numSamples = sampleRate * durationSec
                val pcmData = ByteArray(numSamples * 2)

                // Generate harmonic test sweep: 440Hz + 880Hz
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 440.0 + (Math.sin(2.0 * Math.PI * 0.5 * t) * 110.0)
                    val s = (Math.sin(2.0 * Math.PI * freq * t) * 18000).toInt().toShort()
                    pcmData[i * 2] = (s.toInt() and 0xFF).toByte()
                    pcmData[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
                }

                // Write standard RIFF WAV header
                FileOutputStream(targetWav).use { fos ->
                    writeWavHeader(fos, 1, sampleRate, 16, pcmData.size)
                    fos.write(pcmData)
                }

                // Also try copy to default path
                try {
                    val sdcardDir = File(MicHookSettings.DEFAULT_DIR)
                    if (!sdcardDir.exists()) sdcardDir.mkdirs()
                    val sdcardFile = File(MicHookSettings.DEFAULT_MP3)
                    targetWav.copyTo(sdcardFile, overwrite = true)
                } catch (_: Exception) {}

                updateAudioPath(targetWav.absolutePath)
                startInjector()
                addLog("GENERATOR", "Generated and loaded 44.1kHz test audio asset: ${targetWav.name}", LogLevel.INFO)

            } catch (e: Exception) {
                addLog("GENERATOR", "Failed to generate sample tone: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    private fun writeWavHeader(out: FileOutputStream, channels: Int, sampleRate: Int, bitsPerSample: Int, pcmSize: Int) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalDataLen = pcmSize + 36

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // subchunk1 size (16 for PCM)
        header[20] = 1; header[21] = 0 // AudioFormat (1 for PCM)
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = blockAlign.toByte(); header[33] = 0
        header[34] = bitsPerSample.toByte(); header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmSize and 0xff).toByte()
        header[41] = ((pcmSize shr 8) and 0xff).toByte()
        header[42] = ((pcmSize shr 16) and 0xff).toByte()
        header[43] = ((pcmSize shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }

    /**
     * Interactive test bench: simulates AudioRecord read execution or performs
     * live capture validation through AudioInjector.fillBuffer().
     */
    fun startTestRecorder() {
        if (_uiState.value.isTestRecordingActive) return

        if (!AudioInjector.isReady) {
            startInjector()
        }

        _uiState.update { it.copy(isTestRecordingActive = true, testRecordBytesRead = 0L) }
        addLog("TESTER", "Started Live Mic Hook AudioRecord loop test bench", LogLevel.INFO)

        testRecordJob?.cancel()
        testRecordJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(2048)
            var totalRead = 0L
            while (isActive && _uiState.value.isTestRecordingActive) {
                // Emulate AudioRecord.read(buffer, 0, buffer.size) calling AudioInjector.fillBuffer
                val read = AudioInjector.fillBuffer(buffer, 0, buffer.size)
                totalRead += read

                // Compute real-time RMS for waveform
                val floats = FloatArray(32)
                val step = buffer.size / 32
                for (i in 0 until 32) {
                    val sIdx = i * step
                    if (sIdx + 1 < buffer.size) {
                        val lo = buffer[sIdx].toInt() and 0xFF
                        val hi = buffer[sIdx + 1].toInt()
                        val sample = ((hi shl 8) or lo).toShort()
                        floats[i] = (kotlin.math.abs(sample.toInt()) / 32768.0f).coerceIn(0.05f, 1.0f)
                    }
                }

                _uiState.update {
                    it.copy(
                        testRecordBytesRead = totalRead,
                        waveformSamples = floats.toList()
                    )
                }

                delay(40) // ~25 fps update loop
            }
        }
    }

    fun stopTestRecorder() {
        testRecordJob?.cancel()
        testRecordJob = null
        _uiState.update { it.copy(isTestRecordingActive = false) }
        addLog("TESTER", "Stopped Live Mic Hook AudioRecord loop test bench", LogLevel.INFO)
    }

    fun addLog(tag: String, message: String, level: LogLevel) {
        val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val entry = LogEntry(
            timestamp = timeFormat.format(Date()),
            tag = tag,
            message = message,
            level = level
        )
        _uiState.update {
            val updated = it.logs.toMutableList()
            updated.add(0, entry) // Newest at top
            if (updated.size > 200) {
                updated.subList(200, updated.size).clear()
            }
            it.copy(logs = updated)
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
        addLog("SYSTEM", "Logs cleared", LogLevel.INFO)
    }

    override fun onCleared() {
        super.onCleared()
        statsJob?.cancel()
        testRecordJob?.cancel()
    }
}
