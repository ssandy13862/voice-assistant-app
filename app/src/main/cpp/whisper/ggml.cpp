// 簡化的 GGML 實現 - 用於構建測試
// 實際使用時需要替換為真正的 ggml.cpp

#include <android/log.h>

#define TAG "GGML"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

// 模擬 GGML 初始化
void ggml_init() {
    LOGI("GGML 初始化（模擬）");
}

// 模擬 GGML 清理
void ggml_cleanup() {
    LOGI("GGML 清理（模擬）");
}
