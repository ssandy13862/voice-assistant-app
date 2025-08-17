#include "vad.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define TAG "VAD"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

VAD::VAD(int sample_rate, int frame_length)
    : sample_rate_(sample_rate)
    , frame_length_(frame_length)
    , energy_threshold_(0.001f)
    , zero_crossing_threshold_(0.1f)
    , smoothing_factor_(0.8f)
    , previous_energy_(0.0f)
    , previous_voice_state_(false)
    , voice_frame_count_(0)
    , silence_frame_count_(0) {
    
    LOGI("VAD 構造函數: sample_rate=%d, frame_length=%d", sample_rate, frame_length);
}

VAD::~VAD() {
    LOGI("VAD 析構函數");
}

bool VAD::init() {
    LOGI("初始化 VAD");
    reset();
    return true;
}

bool VAD::detect(const std::vector<float>& audio_frame) {
    if (audio_frame.size() != static_cast<size_t>(frame_length_)) {
        LOGE("音頻幀長度不匹配: expected=%d, got=%zu", frame_length_, audio_frame.size());
        return false;
    }
    
    // 計算音頻特徵
    float energy = calculateEnergy(audio_frame);
    float zcr = calculateZeroCrossingRate(audio_frame);
    float spectral_centroid = calculateSpectralCentroid(audio_frame);
    
    // 多特徵融合決策
    bool energy_voice = energy > energy_threshold_;
    bool zcr_voice = zcr > zero_crossing_threshold_;
    
    // 語音通常有適中的過零率和較高的能量
    bool current_detection = energy_voice && (zcr < 0.5f) && (spectral_centroid > 1000.0f);
    
    // 應用狀態機進行平滑
    bool final_decision = applyStateMachine(current_detection);
    
    // 更新歷史
    previous_energy_ = energy;
    previous_voice_state_ = final_decision;
    
    return final_decision;
}

float VAD::getVoiceProbability(const std::vector<float>& audio_frame) {
    if (audio_frame.size() != static_cast<size_t>(frame_length_)) {
        return 0.0f;
    }
    
    float energy = calculateEnergy(audio_frame);
    float zcr = calculateZeroCrossingRate(audio_frame);
    float spectral_centroid = calculateSpectralCentroid(audio_frame);
    
    // 計算各特徵的置信度
    float energy_confidence = std::min(1.0f, energy / energy_threshold_);
    float zcr_confidence = 1.0f - std::abs(zcr - 0.3f) / 0.3f; // 語音通常在 0.2-0.4 範圍
    float spectral_confidence = std::min(1.0f, spectral_centroid / 3000.0f);
    
    // 加權融合
    float probability = (energy_confidence * 0.5f + 
                        zcr_confidence * 0.3f + 
                        spectral_confidence * 0.2f);
    
    return std::max(0.0f, std::min(1.0f, probability));
}

void VAD::reset() {
    previous_energy_ = 0.0f;
    previous_voice_state_ = false;
    voice_frame_count_ = 0;
    silence_frame_count_ = 0;
}

void VAD::setEnergyThreshold(float threshold) {
    energy_threshold_ = threshold;
}

void VAD::setZeroCrossingThreshold(float threshold) {
    zero_crossing_threshold_ = threshold;
}

void VAD::setSmoothingFactor(float factor) {
    smoothing_factor_ = factor;
}

float VAD::calculateEnergy(const std::vector<float>& frame) {
    float energy = 0.0f;
    for (float sample : frame) {
        energy += sample * sample;
    }
    return energy / frame.size();
}

float VAD::calculateZeroCrossingRate(const std::vector<float>& frame) {
    int zero_crossings = 0;
    for (size_t i = 1; i < frame.size(); ++i) {
        if ((frame[i] >= 0.0f && frame[i-1] < 0.0f) || 
            (frame[i] < 0.0f && frame[i-1] >= 0.0f)) {
            zero_crossings++;
        }
    }
    return static_cast<float>(zero_crossings) / (frame.size() - 1);
}

float VAD::calculateSpectralCentroid(const std::vector<float>& frame) {
    // 簡化的頻譜重心計算
    float weighted_sum = 0.0f;
    float magnitude_sum = 0.0f;
    
    for (size_t i = 0; i < frame.size(); ++i) {
        float frequency = static_cast<float>(i) * sample_rate_ / (2.0f * frame.size());
        float magnitude = std::abs(frame[i]);
        
        weighted_sum += frequency * magnitude;
        magnitude_sum += magnitude;
    }
    
    return magnitude_sum > 0.0f ? weighted_sum / magnitude_sum : 0.0f;
}

bool VAD::applyStateMachine(bool current_voice_detection) {
    if (current_voice_detection) {
        voice_frame_count_++;
        silence_frame_count_ = 0;
        
        // 需要連續幾幀檢測到語音才認為是語音
        if (voice_frame_count_ >= 3) {
            return true;
        }
    } else {
        silence_frame_count_++;
        voice_frame_count_ = 0;
        
        // 需要連續幾幀靜音才認為是靜音
        if (silence_frame_count_ >= 5) {
            return false;
        }
    }
    
    // 保持之前的狀態
    return previous_voice_state_;
}
