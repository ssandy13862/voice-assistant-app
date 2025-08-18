#ifndef WHISPER_H
#define WHISPER_H

#ifdef __cplusplus
extern "C" {
#endif

struct whisper_context;

typedef struct {
    int n_threads;
    int translate;  // 改為 int 而非 bool
    char language[8];
} whisper_params;

// Whisper API 函數
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

#endif // WHISPER_H