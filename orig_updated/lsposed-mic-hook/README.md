# lsposed-mic-hook

LSposed module that hooks **every** microphone path (Java + Native) inside a
target app and replaces real mic audio with PCM decoded from an MP3 file.

## What gets hooked

| Layer | Target | Method |
|---|---|---|
| Java | `AudioRecord` | All `read()` overloads (byte/short/float/ByteBuffer + readMode variants) |
| Java | `MediaRecorder` | `setAudioSource()` → redirected to REMOTE_SUBMIX |
| Native | `libaaudio.so` | `AAudioStream_read`, `AAudioStream_write` |
| Native | `libaudioclient.so` | `AudioRecord::read` (mangled C++ symbol) |

## Requirements

- Rooted Android device (Android 9+, API 28+)
- LSposed Manager installed
- JitPack access during the first Gradle sync (to download the ShadowHook AAR)

## Build setup

### 1. ShadowHook native dependency — fully automated

`libshadowhook` is pulled automatically by Gradle. No manual AAR download needed.

The `extractShadowhook` Gradle task runs before every CMake build step and:

1. Downloads the AAR from JitPack (`com.github.bytedance:android-inline-hook:shadowhook-1.0.9`)
2. Extracts `shadowhook.h` → `app/src/main/cpp/shadowhook/include/`
3. Extracts `libshadowhook.so` for each ABI → `app/src/main/cpp/shadowhook/libs/<abi>/`

The extracted `.so` files are also packaged inside the APK via the `jniLibs` source set
so the dynamic linker can find them at runtime.

**All you need is an active internet connection on the first sync.**

### 2. LSposed API

Already declared in `app/build.gradle`:
```groovy
compileOnly("io.github.libxposed:api:100")
```

### 3. Build

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Device setup

```bash
# Push your MP3
adb shell mkdir -p /sdcard/MicHook
adb push your_audio.mp3 /sdcard/MicHook/inject.mp3

# Optional: point to a different MP3 at runtime
adb shell "echo '/sdcard/MicHook/other.mp3' > /sdcard/MicHook/config.txt"

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

## LSposed activation

1. Open **LSposed Manager**
2. Find **MicHook** → enable it
3. Tap **Scope** → select your target app
4. Force-stop the target app and relaunch it
5. Check LSposed logs — you should see `MicHook → Injected into: <package>`

## Logs

```bash
adb logcat -s MicHook MicHook_Native MicHook_Injector
```

## Changing the MP3 without rebuilding

Write the new path to `/sdcard/MicHook/config.txt`:
```bash
adb shell "echo '/sdcard/Download/new_voice.mp3' > /sdcard/MicHook/config.txt"
```
Then force-stop and relaunch the target app.

## Architecture

```
Target App Process (injected by LSposed)
│
├── Java Layer (Xposed hooks)
│   ├── AudioRecord.read(byte[])          → AudioInjector.fillBuffer()
│   ├── AudioRecord.read(byte[], readMode) → AudioInjector.fillBuffer()
│   ├── AudioRecord.read(short[])          → AudioInjector.fillShortBuffer()
│   ├── AudioRecord.read(float[])          → PCM16→float conversion
│   ├── AudioRecord.read(ByteBuffer)       → AudioInjector.fillByteBuffer()
│   └── MediaRecorder.setAudioSource()     → REMOTE_SUBMIX redirect
│
├── Native Layer (ShadowHook PLT hooks)
│   ├── libaaudio.so::AAudioStream_read    → inject_pcm() via JNI
│   └── libaudioclient.so::AudioRecord::read → inject_pcm() via JNI
│
└── Background Decoder Thread (AudioInjector)
    └── MP3 → MediaCodec → PCM16 → LinkedBlockingQueue → hooks
```

## ShadowHook dependency graph

```
Gradle sync
    └── downloads AAR from JitPack
            └── extractShadowhook task
                    ├── shadowhook/include/shadowhook.h   (for CMake)
                    └── shadowhook/libs/<abi>/libshadowhook.so
                            ├── packaged into APK (jniLibs source set)
                            └── linked by CMake into libmichook_native.so
```
