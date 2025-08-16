package com.voiceassistant.app.domain.repository

import androidx.camera.core.ImageProxy
import com.voiceassistant.app.domain.model.FaceDetectionResult
import kotlinx.coroutines.flow.Flow

/**
 * 人臉檢測倉庫介面
 */
interface FaceDetectionRepository {
    
    /**
     * 檢測圖像中的人臉
     */
    suspend fun detectFaces(imageProxy: ImageProxy): FaceDetectionResult
    
    /**
     * 開始人臉檢測流
     */
    fun startFaceDetection(): Flow<FaceDetectionResult>
    
    /**
     * 停止人臉檢測
     */
    fun stopFaceDetection()
    
    /**
     * 設定檢測靈敏度
     */
    fun setDetectionSensitivity(sensitivity: Float)
}
