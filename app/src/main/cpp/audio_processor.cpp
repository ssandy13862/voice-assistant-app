#include "audio_processor.h"
#include <android/log.h>
#include <algorithm>
#include <cmath>

#define TAG "AudioProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

AudioProcessor::AudioProcessor() : whisper_ctx(nullptr), is_initialized(false) {
    LOGI("AudioProcessor 構造函數");
}

AudioProcessor::~AudioProcessor() {
    cleanup();
}

bool AudioProcessor::initWhisper(const std::string& model_path) {
    LOGI("初始化 Whisper 模型: %s", model_path.c_str());
    
    // 使用現有的簡化 API
    whisper_ctx = whisper_init_from_file(model_path.c_str());
    
    if (whisper_ctx == nullptr) {
        LOGE("無法加載 Whisper 模型: %s", model_path.c_str());
        return false;
    }
    
    is_initialized = true;
    LOGI("Whisper 模型初始化完成");
    return true;
}

std::string AudioProcessor::transcribe(const std::vector<float>& audio_data) {
    if (!is_initialized || !whisper_ctx) {
        LOGE("Whisper 未初始化");
        return "";
    }
    
    LOGI("開始轉錄音頻，樣本數: %zu", audio_data.size());
    
    // 預處理音頻
    std::vector<float> processed_audio = preprocessAudio(audio_data);
    
    if (processed_audio.size() < 1000) {
        LOGE("音頻樣本太少: %zu", processed_audio.size());
        return ""; // 音頻太短
    }
    
    // 設置 Whisper 推理參數
    whisper_params wparams = whisper_full_default_params(0);
    
    // 執行語音識別 - 使用現有的簡化 API
    if (whisper_full(static_cast<struct whisper_context*>(whisper_ctx), wparams, processed_audio.data(), processed_audio.size()) != 0) {
        LOGE("Whisper 識別失敗");
        return "";
    }
    
    // 獲取識別結果
    const int n_segments = whisper_full_n_segments(static_cast<struct whisper_context*>(whisper_ctx));
    if (n_segments <= 0) {
        LOGI("沒有識別到語音內容");
        return "";
    }
    
    std::string result;
    for (int i = 0; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text(static_cast<struct whisper_context*>(whisper_ctx), i);
        if (text != nullptr) {
            result += text;
        }
    }
    
    LOGI("轉錄結果: %s", result.c_str());
    return result;
}

std::vector<float> AudioProcessor::preprocessAudio(const std::vector<float>& raw_audio) {
    std::vector<float> processed = raw_audio;
    
    // 1. 正規化音頻
    float max_val = 0.0f;
    for (float sample : processed) {
        max_val = std::max(max_val, std::abs(sample));
    }
    
    if (max_val > 0.0f) {
        for (float& sample : processed) {
            sample /= max_val;
        }
    }
    
    // 2. 高通濾波器（移除低頻噪音）
    const float alpha = 0.97f;
    if (processed.size() > 1) {
        for (size_t i = 1; i < processed.size(); ++i) {
            processed[i] = alpha * (processed[i] - processed[i-1]) + alpha * processed[i-1];
        }
    }
    
    // 3. 確保音頻長度適合 Whisper（16kHz 採樣率）
    processed = resample16k(processed, 16000);
    
    return processed;
}

std::vector<float> AudioProcessor::resample16k(const std::vector<float>& audio, int original_rate) {
    // 如果已經是 16kHz，直接返回
    if (original_rate == 16000) {
        return audio;
    }
    
    // 簡單的線性重採樣
    float ratio = static_cast<float>(original_rate) / 16000.0f;
    size_t new_size = static_cast<size_t>(audio.size() / ratio);
    
    std::vector<float> resampled(new_size);
    for (size_t i = 0; i < new_size; ++i) {
        float index = i * ratio;
        size_t index_int = static_cast<size_t>(index);
        
        if (index_int < audio.size() - 1) {
            float frac = index - index_int;
            resampled[i] = audio[index_int] * (1.0f - frac) + audio[index_int + 1] * frac;
        } else if (index_int < audio.size()) {
            resampled[i] = audio[index_int];
        } else {
            resampled[i] = 0.0f;
        }
    }
    
    return resampled;
}

void AudioProcessor::cleanup() {
    if (whisper_ctx) {
        whisper_free(static_cast<struct whisper_context*>(whisper_ctx));
        whisper_ctx = nullptr;
    }
    is_initialized = false;
    LOGI("AudioProcessor 清理完成");
}
