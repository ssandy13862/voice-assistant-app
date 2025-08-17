// 簡化的 GGML Alloc 實現
#include <android/log.h>
#define TAG "GGML-Alloc"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

void ggml_alloc_init() {
    LOGI("GGML Alloc 初始化（模擬）");
}
