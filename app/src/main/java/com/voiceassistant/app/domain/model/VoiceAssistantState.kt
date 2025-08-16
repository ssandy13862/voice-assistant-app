package com.voiceassistant.app.domain.model

/**
 * 語音助理狀態枚舉
 */
enum class VoiceAssistantState {
    IDLE,           // 待機狀態
    DETECTING,      // 人臉檢測中
    LISTENING,      // 聆聽中
    PROCESSING,     // 處理中（STT + AI對話）
    SPEAKING,       // 說話中（TTS）
    ERROR           // 錯誤狀態
}

/**
 * 音頻狀態
 */
enum class AudioState {
    SILENT,         // 靜音
    SPEAKING,       // 說話中
    NOISE           // 噪音
}

/**
 * 語音助理會話資料
 */
data class ConversationItem(
    val id: String,
    val userInput: String,
    val aiResponse: String,
    val timestamp: Long,
    val isFromUser: Boolean
)

/**
 * 人臉檢測結果
 */
data class FaceDetectionResult(
    val facesDetected: Int,
    val largestFaceConfidence: Float,
    val facePosition: FacePosition?
)

/**
 * 人臉位置資訊
 */
data class FacePosition(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * API配置
 */
data class ApiConfig(
    val openAiApiKey: String,
    val whisperApiUrl: String,
    val vadModelPath: String
)

/**
 * 語音處理結果
 */
sealed class VoiceProcessingResult {
    object Idle : VoiceProcessingResult()
    data class SpeechDetected(val audioData: ByteArray) : VoiceProcessingResult()
    data class TextRecognized(val text: String, val confidence: Float) : VoiceProcessingResult()
    data class AiResponse(val response: String) : VoiceProcessingResult()
    data class Error(val message: String, val exception: Exception?) : VoiceProcessingResult()
}
