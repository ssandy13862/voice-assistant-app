#include "audio_processor.h"
#include <android/log.h>
#include <algorithm>
#include <cmath>

#define TAG "AudioProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// 簡化的 Whisper 結構（實際使用時需要包含 whisper.h）
struct whisper_context {
    // 模型數據
    bool loaded;
};

AudioProcessor::AudioProcessor() : whisper_ctx(nullptr), is_initialized(false) {
    LOGI("AudioProcessor 構造函數");
}

AudioProcessor::~AudioProcessor() {
    cleanup();
}

bool AudioProcessor::initWhisper(const std::string& model_path) {
    LOGI("初始化 Whisper 模型: %s", model_path.c_str());
    
    // 這裡應該加載真實的 Whisper 模型
    // 暫時使用簡化版本
    whisper_ctx = new whisper_context();
    static_cast<whisper_context*>(whisper_ctx)->loaded = true;
    
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
    
    // 這裡應該調用真實的 Whisper 推理
    // 暫時返回模擬結果
    if (processed_audio.size() < 1000) {
        return ""; // 音頻太短
    }
    
    // 基於音頻能量的簡單語音檢測
    float energy = 0.0f;
    for (float sample : processed_audio) {
        energy += sample * sample;
    }
    energy /= processed_audio.size();
    
    if (energy < 0.001f) {
        return ""; // 靜音
    }
    
    // 模擬轉錄結果（實際實現需要調用 Whisper）
    std::vector<std::string> mock_results = {
        "你好",
        "今天天氣怎麼樣",
        "請告訴我一個笑話",
        "現在幾點了",
        "謝謝你的幫助",
        "播放音樂",
        "設定鬧鐘"
    };
    
    // 基於音頻特徵選擇結果
    size_t index = static_cast<size_t>(energy * 1000) % mock_results.size();
    std::string result = mock_results[index];
    
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
        delete static_cast<whisper_context*>(whisper_ctx);
        whisper_ctx = nullptr;
    }
    is_initialized = false;
    LOGI("AudioProcessor 清理完成");
}
