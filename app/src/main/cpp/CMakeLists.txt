cmake_minimum_required(VERSION 3.22.1)

project("voiceassistant")

# 設置 C++ 標準
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 包含頭文件目錄
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/whisper
    ${CMAKE_CURRENT_SOURCE_DIR}/vad
)

# Whisper.cpp 源文件
set(WHISPER_SOURCES
    whisper/whisper.cpp
    whisper/ggml.cpp
    whisper/ggml-alloc.cpp
    whisper/ggml-backend.cpp
    whisper/ggml-quants.cpp
)

# VAD 源文件
set(VAD_SOURCES
    vad/vad.cpp
    vad/energy_vad.cpp
)

# JNI 接口源文件
set(JNI_SOURCES
    jni_interface.cpp
    audio_processor.cpp
)

# 創建共享庫
add_library(
    voiceassistant
    SHARED
    ${WHISPER_SOURCES}
    ${VAD_SOURCES}
    ${JNI_SOURCES}
)

# 查找並鏈接 Android 庫
find_library(log-lib log)
find_library(android-lib android)

# 鏈接庫
target_link_libraries(
    voiceassistant
    ${log-lib}
    ${android-lib}
)

# 編譯選項
target_compile_options(voiceassistant PRIVATE
    -O3
    -ffast-math
    -funroll-loops
    -DGGML_USE_ACCELERATE=1
    -DANDROID
)

# 如果是 ARM64，啟用 NEON
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(voiceassistant PRIVATE -mfpu=neon)
endif()
