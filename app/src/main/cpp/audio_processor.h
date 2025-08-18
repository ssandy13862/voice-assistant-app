#ifndef AUDIO_PROCESSOR_H
#define AUDIO_PROCESSOR_H

#include <string>
#include <vector>
#include "whisper/whisper.h"

class AudioProcessor {
public:
    AudioProcessor();
    ~AudioProcessor();
    
    bool initWhisper(const std::string& model_path);
    std::string transcribe(const std::vector<float>& audio_data);
    void cleanup();
    
private:
    void* whisper_ctx;
    bool is_initialized;
    
    // 音頻預處理
    std::vector<float> preprocessAudio(const std::vector<float>& raw_audio);
    
    // 音頻重採樣到 16kHz
    std::vector<float> resample16k(const std::vector<float>& audio, int original_rate);
};

#endif // AUDIO_PROCESSOR_H
