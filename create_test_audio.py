#!/usr/bin/env python3
"""
建立測試音頻檔案的腳本
生成符合 VAD/STT 測試需求的合成音頻
"""

import numpy as np
import wave
import os
from pathlib import Path

def create_synthetic_speech_audio(filename, duration=2.0, sample_rate=16000):
    """
    建立合成語音音頻，模擬人聲特徵
    """
    # 時間軸
    t = np.linspace(0, duration, int(sample_rate * duration), False)
    
    # 模擬語音的多頻率組合
    # 基頻約 150-300Hz（人聲範圍）
    fundamental_freq = 200.0  # 基頻
    
    # 生成複合波形
    signal = (
        0.4 * np.sin(2 * np.pi * fundamental_freq * t) +           # 基頻
        0.3 * np.sin(2 * np.pi * fundamental_freq * 2 * t) +       # 二次諧波
        0.2 * np.sin(2 * np.pi * fundamental_freq * 3 * t) +       # 三次諧波
        0.1 * np.sin(2 * np.pi * fundamental_freq * 4 * t)         # 四次諧波
    )
    
    # 添加包絡線（音量變化）
    envelope = np.sin(np.pi * t / duration) ** 2  # 更平滑的包絡線
    signal = signal * envelope
    
    # 添加輕微的隨機噪音（模擬真實語音的複雜性）
    noise = np.random.normal(0, 0.02, len(signal))
    signal = signal + noise
    
    # 正規化音量
    signal = signal * 0.7  # 避免過大音量
    
    # 轉換為 16-bit PCM
    audio_data = (signal * 32767).astype(np.int16)
    
    return audio_data, sample_rate

def create_varied_speech_audio(filename, duration=3.0, sample_rate=16000):
    """
    建立變化的語音音頻，模擬說話時的音調變化
    """
    t = np.linspace(0, duration, int(sample_rate * duration), False)
    
    # 模擬說話時的音調變化（fundamental frequency modulation）
    base_freq = 180.0
    freq_variation = 50.0  # 音調變化範圍
    
    # 音調隨時間變化（模擬語調）
    instantaneous_freq = base_freq + freq_variation * np.sin(2 * np.pi * 0.5 * t)
    
    # 生成調頻信號
    phase = 2 * np.pi * np.cumsum(instantaneous_freq) / sample_rate
    
    signal = (
        0.5 * np.sin(phase) +                                    # 基頻（調頻）
        0.3 * np.sin(2 * phase) +                               # 二次諧波
        0.2 * np.sin(3 * phase)                                 # 三次諧波
    )
    
    # 複雜包絡線（模擬多個音節）
    syllable_pattern = np.sin(np.pi * t * 3 / duration) ** 4  # 3個音節的模式
    overall_envelope = np.sin(np.pi * t / duration)           # 整體包絡
    signal = signal * syllable_pattern * overall_envelope
    
    # 添加噪音
    noise = np.random.normal(0, 0.03, len(signal))
    signal = signal + noise
    
    # 正規化
    signal = signal * 0.6
    audio_data = (signal * 32767).astype(np.int16)
    
    return audio_data, sample_rate

def write_wav_file(filename, audio_data, sample_rate):
    """
    寫入 WAV 檔案
    """
    with wave.open(filename, 'w') as wav_file:
        wav_file.setnchannels(1)  # 單聲道
        wav_file.setsampwidth(2)  # 16-bit
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(audio_data.tobytes())
    
    print(f"✓ 建立音頻檔案: {filename}")
    print(f"  - 長度: {len(audio_data) / sample_rate:.1f} 秒")
    print(f"  - 檔案大小: {os.path.getsize(filename) / 1024:.1f} KB")

def main():
    # 建立 assets 目錄路徑
    assets_dir = Path("/Users/sandy/voice-assistant-app/app/src/main/assets")
    
    print("🎵 開始建立測試音頻檔案...")
    print(f"目標目錄: {assets_dir}")
    
    if not assets_dir.exists():
        print(f"❌ 目錄不存在: {assets_dir}")
        return
    
    # 建立不同類型的測試音頻
    test_files = [
        {
            "filename": "test_audio_hello.wav",
            "duration": 1.5,
            "type": "simple",
            "description": "簡單合成語音 - 模擬 '你好'"
        },
        {
            "filename": "test_audio_thanks.wav", 
            "duration": 2.0,
            "type": "simple",
            "description": "簡單合成語音 - 模擬 '謝謝'"
        },
        {
            "filename": "test_audio_question.wav",
            "duration": 3.5,
            "type": "varied",
            "description": "變化合成語音 - 模擬 '今天天氣如何'"
        },
        {
            "filename": "test_voice_sample.wav",
            "duration": 2.5,
            "type": "varied",
            "description": "通用測試語音"
        }
    ]
    
    for file_info in test_files:
        print(f"\n📁 建立: {file_info['filename']}")
        print(f"   說明: {file_info['description']}")
        
        filepath = assets_dir / file_info["filename"]
        
        if file_info["type"] == "simple":
            audio_data, sample_rate = create_synthetic_speech_audio(
                str(filepath), 
                duration=file_info["duration"]
            )
        else:  # varied
            audio_data, sample_rate = create_varied_speech_audio(
                str(filepath),
                duration=file_info["duration"]
            )
        
        write_wav_file(str(filepath), audio_data, sample_rate)
    
    print(f"\n🎉 完成！建立了 {len(test_files)} 個測試音頻檔案")
    print("\n💡 這些是合成音頻，用於測試 VAD 和 STT 系統")
    print("   雖然不包含真實語言內容，但具備人聲的頻率特徵")
    
    # 顯示檔案列表
    print(f"\n📋 Assets 目錄內容:")
    for file in sorted(assets_dir.iterdir()):
        if file.is_file():
            size = file.stat().st_size / 1024
            print(f"   {file.name} ({size:.1f} KB)")

if __name__ == "__main__":
    main()