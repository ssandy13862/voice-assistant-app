package com.voiceassistant.app.data.repository

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.voiceassistant.app.domain.model.FaceDetectionResult
import com.voiceassistant.app.domain.model.FacePosition
import com.voiceassistant.app.domain.repository.FaceDetectionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google ML Kit人臉檢測實作
 */
@Singleton
class FaceDetectionRepositoryImpl @Inject constructor() : FaceDetectionRepository {

    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.2f) // 提高最小人臉尺寸以減少誤檢
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE) // 關閉輪廓檢測
        .enableTracking()
        .build()

    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)
    private var detectionSensitivity = 0.5f

    override suspend fun detectFaces(imageProxy: ImageProxy): FaceDetectionResult {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            try {
                val faces = faceDetector.process(image).await()
                
                if (faces.isEmpty()) {
                    return FaceDetectionResult(0, 0f, null)
                }
                
                // 找到最大的人臉
                val largestFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                
                largestFace?.let { face ->
                    val boundingBox = face.boundingBox
                    val facePosition = FacePosition(
                        x = boundingBox.left.toFloat(),
                        y = boundingBox.top.toFloat(),
                        width = boundingBox.width().toFloat(),
                        height = boundingBox.height().toFloat()
                    )
                    
                    // 計算置信度（基於人臉大小）
                    val imageArea = imageProxy.width * imageProxy.height
                    val faceArea = boundingBox.width() * boundingBox.height()
                    val confidence = (faceArea.toFloat() / imageArea).coerceIn(0f, 1f)
                    
                    return FaceDetectionResult(
                        facesDetected = faces.size,
                        largestFaceConfidence = confidence,
                        facePosition = facePosition
                    )
                }
            } catch (e: Exception) {
                // 處理檢測錯誤
                return FaceDetectionResult(0, 0f, null)
            }
        }
        
        return FaceDetectionResult(0, 0f, null)
    }

    override fun startFaceDetection(): Flow<FaceDetectionResult> = callbackFlow {
        // 這裡需要與Camera整合，實際的相機流處理在UI層
        // 此方法主要用於狀態管理
        awaitClose { }
    }

    override fun stopFaceDetection() {
        // 停止檢測邏輯
    }

    override fun setDetectionSensitivity(sensitivity: Float) {
        this.detectionSensitivity = sensitivity.coerceIn(0f, 1f)
    }
}
