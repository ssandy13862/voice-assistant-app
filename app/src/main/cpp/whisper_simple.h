#ifndef WHISPER_SIMPLE_H
#define WHISPER_SIMPLE_H

#include <string>

#ifdef __cplusplus
extern "C" {
#endif

// 簡化的 Whisper API - 匹配現有實現
struct whisper_context;

struct whisper_params {
    int n_threads;
    int translate;  // 改為 int 而非 bool，避免 C/C++ 兼容問題
    char language[8];  // 改為 char 數組而非 std::string，避免 C 兼容問題
};

// Whisper 函數聲明
whisper_context* whisper_init_from_file(const char* path_model);
void whisper_free(whisper_context* ctx);
int whisper_full(whisper_context* ctx, whisper_params params, 
                 const float* samples, int n_samples);
int whisper_full_n_segments(whisper_context* ctx);
const char* whisper_full_get_segment_text(whisper_context* ctx, int i_segment);
whisper_params whisper_full_default_params(int strategy);

#ifdef __cplusplus
}
#endif

#endif // WHISPER_SIMPLE_H