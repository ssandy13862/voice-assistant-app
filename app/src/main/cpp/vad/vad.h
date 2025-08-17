#ifndef VAD_H
#define VAD_H

#include <vector>

class VAD {
public:
    VAD(int sample_rate = 16000, int frame_length = 512);
    ~VAD();
    
    bool init();
    bool detect(const std::vector<float>& audio_frame);
    float getVoiceProbability(const std::vector<float>& audio_frame);
    void reset();
    
    // 設置參數
    void setEnergyThreshold(float threshold);
    void setZeroCrossingThreshold(float threshold);
    void setSmoothingFactor(float factor);
    
private:
    int sample_rate_;
    int frame_length_;
    
    // VAD 參數
    float energy_threshold_;
    float zero_crossing_threshold_;
    float smoothing_factor_;
    
    // 狀態變量
    float previous_energy_;
    bool previous_voice_state_;
    int voice_frame_count_;
    int silence_frame_count_;
    
    // 音頻分析函數
    float calculateEnergy(const std::vector<float>& frame);
    float calculateZeroCrossingRate(const std::vector<float>& frame);
    float calculateSpectralCentroid(const std::vector<float>& frame);
    bool applyStateMachine(bool current_voice_detection);
};

#endif // VAD_H
