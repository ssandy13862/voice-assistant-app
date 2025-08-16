package com.voiceassistant.app.domain.usecase

import androidx.camera.core.ImageProxy
import com.voiceassistant.app.domain.model.*
import com.voiceassistant.app.domain.repository.FaceDetectionRepository
import com.voiceassistant.app.domain.repository.SpeechRepository
import com.voiceassistant.app.domain.repository.VadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音助理核心业务逻辑
 */
@Singleton
class VoiceAssistantUseCase @Inject constructor(
    private val faceDetectionRepository: FaceDetectionRepository,
    private val vadRepository: VadRepository,
    private val speechRepository: SpeechRepository
) {
    
    private val _state = MutableStateFlow(VoiceAssistantState.IDLE)
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()
    
    private val _conversationHistory = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationItem>> = _conversationHistory.asStateFlow()
    
    private val conversationMessages = mutableListOf<String>()
    private var isFreeMode = false
    
    /**
     * 处理人脸检测结果
     */
    suspend fun processFaceDetection(imageProxy: ImageProxy): FaceDetectionResult {
        val result = faceDetectionRepository.detectFaces(imageProxy)
        
        // 根据人脸检测结果更新状态
        when {
            result.facesDetected > 0 && result.largestFaceConfidence > 0.3f -> {
                if (_state.value == VoiceAssistantState.IDLE) {
                    _state.value = VoiceAssistantState.DETECTING
                    startListening()
                }
            }
            result.facesDetected == 0 -> {
                if (_state.value in listOf(VoiceAssistantState.DETECTING, VoiceAssistantState.LISTENING)) {
                    if (!isFreeMode) {
                        stopListening()
                        _state.value = VoiceAssistantState.IDLE
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * 开始语音监听
     */
    private suspend fun startListening() {
        if (!vadRepository.initializeVad()) {
            _state.value = VoiceAssistantState.ERROR
            return
        }
        
        _state.value = VoiceAssistantState.LISTENING
        
        vadRepository.startAudioDetection()
            .collect { audioState ->
                when (audioState) {
                    AudioState.SPEAKING -> {
                        // 检测到语音，开始录音
                        handleSpeechDetected()
                    }
                    AudioState.SILENT -> {
                        // 静音状态
                    }
                    AudioState.NOISE -> {
                        // 噪音，继续监听
                    }
                }
            }
    }
    
    /**
     * 停止语音监听
     */
    private fun stopListening() {
        vadRepository.stopAudioDetection()
        if (speechRepository.isTTSSpeaking()) {
            speechRepository.stopTTS()
        }
    }
    
    /**
     * 处理检测到的语音
     */
    private suspend fun handleSpeechDetected() {
        _state.value = VoiceAssistantState.PROCESSING
        
        // 这里需要实现音频录制逻辑
        // 暂时使用模拟数据
        val mockAudioFile = File("mock_audio.wav")
        
        // 语音转文字
        speechRepository.speechToText(mockAudioFile)
            .onSuccess { recognizedText ->
                if (recognizedText.isNotBlank()) {
                    processUserInput(recognizedText)
                } else {
                    _state.value = VoiceAssistantState.LISTENING
                }
            }
            .onFailure { error ->
                handleError("语音识别失败: ${error.message}")
            }
    }
    
    /**
     * 处理用户输入
     */
    private suspend fun processUserInput(userInput: String) {
        // 添加到对话历史
        conversationMessages.add(userInput)
        
        // AI对话处理
        speechRepository.processAiConversation(userInput, conversationMessages)
            .onSuccess { aiResponse ->
                conversationMessages.add(aiResponse)
                
                // 添加到UI显示的对话历史
                addConversationItem(userInput, aiResponse)
                
                // 语音合成
                speakResponse(aiResponse)
            }
            .onFailure { error ->
                handleError("AI对话处理失败: ${error.message}")
            }
    }
    
    /**
     * 语音合成并播放AI回应
     */
    private suspend fun speakResponse(response: String) {
        _state.value = VoiceAssistantState.SPEAKING
        
        speechRepository.textToSpeech(response)
            .onSuccess {
                // TTS播放完成，返回监听状态
                _state.value = if (isFreeMode || _state.value == VoiceAssistantState.DETECTING) {
                    VoiceAssistantState.LISTENING
                } else {
                    VoiceAssistantState.IDLE
                }
            }
            .onFailure { error ->
                handleError("语音合成失败: ${error.message}")
            }
    }
    
    /**
     * 添加对话项到历史记录
     */
    private fun addConversationItem(userInput: String, aiResponse: String) {
        val currentHistory = _conversationHistory.value.toMutableList()
        
        val conversationItem = ConversationItem(
            id = System.currentTimeMillis().toString(),
            userInput = userInput,
            aiResponse = aiResponse,
            timestamp = System.currentTimeMillis(),
            isFromUser = false
        )
        
        currentHistory.add(conversationItem)
        _conversationHistory.value = currentHistory
    }
    
    /**
     * 切换自由模式
     */
    fun toggleFreeMode() {
        isFreeMode = !isFreeMode
        
        if (isFreeMode) {
            // 进入自由模式，开始持续监听
            _state.value = VoiceAssistantState.LISTENING
        } else {
            // 退出自由模式
            if (_state.value == VoiceAssistantState.LISTENING) {
                _state.value = VoiceAssistantState.IDLE
            }
        }
    }
    
    /**
     * 手动停止TTS（用于插话打断）
     */
    fun interruptSpeaking() {
        if (_state.value == VoiceAssistantState.SPEAKING) {
            speechRepository.stopTTS()
            _state.value = VoiceAssistantState.LISTENING
        }
    }
    
    /**
     * 清除对话历史
     */
    fun clearConversationHistory() {
        _conversationHistory.value = emptyList()
        conversationMessages.clear()
    }
    
    /**
     * 处理错误
     */
    private fun handleError(errorMessage: String) {
        _state.value = VoiceAssistantState.ERROR
        // 错误处理逻辑，比如显示错误信息
        
        // 3秒后自动恢复到空闲状态
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(3000)
            _state.value = VoiceAssistantState.IDLE
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        vadRepository.release()
        speechRepository.stopTTS()
    }
}
