package com.lo.michook.audio

import java.io.File

data class MicHookSettings(
    val audioFilePath: String = DEFAULT_MP3,
    val loopAudio: Boolean = true,
    val volumeMultiplier: Float = 1.0f,
    val muteAudio: Boolean = false,
    val autoStartInjector: Boolean = true
) {
    companion object {
        const val DEFAULT_DIR = "/sdcard/MicHook"
        const val DEFAULT_MP3 = "/sdcard/MicHook/inject.mp3"
        const val CONFIG_FILE = "/sdcard/MicHook/config.txt"
        const val DOWNLOAD_DIR = "/storage/emulated/0/Download"
        const val DOWNLOAD_MP3 = "/storage/emulated/0/Download/inject.mp3"
    }
}

object ConfigManager {

    private val CONFIG_LOCATIONS = listOf(
        "/sdcard/MicHook/config.txt",
        "/storage/emulated/0/MicHook/config.txt",
        "/storage/emulated/0/Download/michook_config.txt",
        "/sdcard/Download/michook_config.txt",
        "/data/local/tmp/michook_config.txt"
    )

    fun getEffectivePath(): String {
        for (loc in CONFIG_LOCATIONS) {
            try {
                val file = File(loc)
                if (file.exists() && file.canRead()) {
                    val content = file.readText().trim()
                    if (content.isNotEmpty()) {
                        val audioFile = File(content)
                        if (audioFile.exists() && audioFile.canRead()) {
                            return content
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Check fallback direct audio paths
        val audioCandidates = listOf(
            MicHookSettings.DEFAULT_MP3,
            "/storage/emulated/0/MicHook/inject.mp3",
            "/storage/emulated/0/Download/inject.mp3",
            "/sdcard/Download/inject.mp3",
            "/storage/emulated/0/Music/inject.mp3",
            "/sdcard/Music/inject.mp3",
            "/sdcard/inject.mp3",
            "/data/local/tmp/inject.mp3"
        )

        for (candidate in audioCandidates) {
            try {
                val f = File(candidate)
                if (f.exists() && f.canRead()) {
                    return f.absolutePath
                }
            } catch (_: Exception) {}
        }

        return MicHookSettings.DEFAULT_MP3
    }

    fun saveConfigPath(path: String): Boolean {
        var anySuccess = false
        val targets = listOf(
            File("/sdcard/MicHook/config.txt"),
            File("/storage/emulated/0/Download/michook_config.txt")
        )

        for (file in targets) {
            try {
                file.parentFile?.mkdirs()
                file.writeText(path.trim())
                anySuccess = true
            } catch (_: Exception) {}
        }

        return anySuccess
    }
}
