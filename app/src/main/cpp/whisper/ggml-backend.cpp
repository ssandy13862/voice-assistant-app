// 簡化的 GGML Backend 實現
#include <android/log.h>
#define TAG "GGML-Backend"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

void ggml_backend_init() {
    LOGI("GGML Backend 初始化（模擬）");
}
