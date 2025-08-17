#include "vad.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>
#include <numeric>

#define TAG "EnergyVAD"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

// 簡單的能量型 VAD 實現，作為 VAD 類的輔助功能

namespace EnergyVAD {

class SimpleVAD {
private:
    float noise_floor_;
    float voice_threshold_;
    std::vector<float> energy_history_;
    size_t history_size_;
    
public:
    SimpleVAD(size_t history_size = 10) 
        : noise_floor_(0.0f)
        , voice_threshold_(0.001f)
        , history_size_(history_size) {
        energy_history_.reserve(history_size);
    }
    
    void updateNoiseFloor(float energy) {
        energy_history_.push_back(energy);
        if (energy_history_.size() > history_size_) {
            energy_history_.erase(energy_history_.begin());
        }
        
        // 使用歷史能量的最小值作為噪音底線
        if (!energy_history_.empty()) {
            noise_floor_ = *std::min_element(energy_history_.begin(), energy_history_.end());
            voice_threshold_ = noise_floor_ * 3.0f; // 閾值設為噪音底線的3倍
        }
    }
    
    bool isVoice(float energy) {
        updateNoiseFloor(energy);
        return energy > voice_threshold_;
    }
    
    float getNoiseFloor() const { return noise_floor_; }
    float getVoiceThreshold() const { return voice_threshold_; }
};

// 全局 VAD 實例（用於 JNI 調用）
static SimpleVAD* g_simple_vad = nullptr;

void initSimpleVAD() {
    if (!g_simple_vad) {
        g_simple_vad = new SimpleVAD();
        LOGI("SimpleVAD 初始化完成");
    }
}

bool detectVoiceActivity(const std::vector<float>& audio_frame) {
    if (!g_simple_vad) {
        initSimpleVAD();
    }
    
    // 計算幀能量
    float energy = 0.0f;
    for (float sample : audio_frame) {
        energy += sample * sample;
    }
    energy /= audio_frame.size();
    
    return g_simple_vad->isVoice(energy);
}

float getVoiceActivityProbability(const std::vector<float>& audio_frame) {
    if (!g_simple_vad) {
        initSimpleVAD();
    }
    
    float energy = 0.0f;
    for (float sample : audio_frame) {
        energy += sample * sample;
    }
    energy /= audio_frame.size();
    
    float threshold = g_simple_vad->getVoiceThreshold();
    if (threshold > 0.0f) {
        return std::min(1.0f, energy / threshold);
    }
    
    return 0.0f;
}

void cleanupSimpleVAD() {
    if (g_simple_vad) {
        delete g_simple_vad;
        g_simple_vad = nullptr;
        LOGI("SimpleVAD 清理完成");
    }
}

} // namespace EnergyVAD
