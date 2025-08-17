// 簡化的 GGML Quants 實現
#include <android/log.h>
#define TAG "GGML-Quants"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

void ggml_quants_init() {
    LOGI("GGML Quants 初始化（模擬）");
}
