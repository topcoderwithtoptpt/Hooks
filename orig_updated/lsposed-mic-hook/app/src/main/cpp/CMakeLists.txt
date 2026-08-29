cmake_minimum_required(VERSION 3.22.1)
project(michook_native CXX)

# ── System libraries ─────────────────────────────────────────────────────────
find_library(LOG_LIB     log)
find_library(ANDROID_LIB android)
find_library(AAUDIO_LIB  aaudio)
find_library(OPENSLES_LIB OpenSLES)

# ── ShadowHook (ByteDance) ───────────────────────────────────────────────────
#
# libshadowhook is pulled by Gradle as an AAR from JitPack:
#   com.github.bytedance:android-inline-hook:shadowhook-1.0.9
#
# The extractShadowhook Gradle task (see app/build.gradle) unzips the AAR and
# places the files exactly here before CMake runs:
#
#   CMakeLists source dir / shadowhook/include/shadowhook.h
#   CMakeLists source dir / shadowhook/libs/<ABI>/libshadowhook.so
#
# So the paths below are always valid by the time this file is parsed.

set(SHADOWHOOK_INCLUDE "${CMAKE_SOURCE_DIR}/shadowhook/include")
set(SHADOWHOOK_LIB_DIR "${CMAKE_SOURCE_DIR}/shadowhook/libs/${ANDROID_ABI}")

# Verify the .so exists so you get a clear error instead of a linker mystery
if(NOT EXISTS "${SHADOWHOOK_LIB_DIR}/libshadowhook.so")
    message(FATAL_ERROR
        "libshadowhook.so not found at ${SHADOWHOOK_LIB_DIR}\n"
        "Run the Gradle sync / build first so the extractShadowhook task\n"
        "unpacks the AAR. See app/build.gradle for details."
    )
endif()

add_library(shadowhook SHARED IMPORTED)
set_target_properties(shadowhook PROPERTIES
    IMPORTED_LOCATION "${SHADOWHOOK_LIB_DIR}/libshadowhook.so"
)

# ── Our module ───────────────────────────────────────────────────────────────
add_library(
    michook_native
    SHARED
    mic_hook.cpp
)

target_include_directories(michook_native PRIVATE
    ${SHADOWHOOK_INCLUDE}
)

target_link_libraries(michook_native
    shadowhook
    ${LOG_LIB}
    ${ANDROID_LIB}
    ${AAUDIO_LIB}
    ${OPENSLES_LIB}
)

target_compile_options(michook_native PRIVATE
    -O2
    -fvisibility=hidden
    -fstack-protector-strong
    -Wno-unused-result
)

# Strip debug symbols in release
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    add_custom_command(TARGET michook_native POST_BUILD
        COMMAND ${CMAKE_STRIP} --strip-unneeded
        $<TARGET_FILE:michook_native>
    )
endif()
