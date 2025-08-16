package com.voiceassistant.app.domain.repository

import com.voiceassistant.app.domain.model.AudioState
import kotlinx.coroutines.flow.Flow

/**
 * 語音活動檢測(VAD)倉庫介面
 */
interface VadRepository {
    
    /**
     * 初始化VAD模型
     */
    suspend fun initializeVad(): Boolean
    
    /**
     * 開始音頻檢測
     */
    fun startAudioDetection(): Flow<AudioState>
    
    /**
     * 停止音頻檢測
     */
    fun stopAudioDetection()
    
    /**
     * 處理音頻資料並返回檢測結果
     */
    suspend fun processAudioData(audioData: ByteArray): AudioState
    
    /**
     * 設定檢測閾值
     */
    fun setVadThreshold(threshold: Float)
    
    /**
     * 釋放資源
     */
    fun release()
}
