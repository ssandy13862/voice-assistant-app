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
 * 語音助理核心業務邏輯
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
    
    // 新增錯誤訊息流
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()
    
    private val conversationMessages = mutableListOf<String>()
    private var isFreeMode = false
    
    /**
     * 處理人臉檢測結果
     */
    suspend fun processFaceDetection(imageProxy: ImageProxy): FaceDetectionResult {
        val result = faceDetectionRepository.detectFaces(imageProxy)
        
        // 根據人臉檢測結果更新狀態
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
     * 開始語音監聽
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
                        // 檢測到語音，開始錄音
                        handleSpeechDetected()
                    }
                    AudioState.SILENT -> {
                        // 靜音狀態
                    }
                    AudioState.NOISE -> {
                        // 噪音，繼續監聽
                    }
                }
            }
    }
    
    /**
     * 停止語音監聽
     */
    private fun stopListening() {
        vadRepository.stopAudioDetection()
        if (speechRepository.isTTSSpeaking()) {
            speechRepository.stopTTS()
        }
    }
    
    /**
     * 處理檢測到的語音
     */
    private suspend fun handleSpeechDetected() {
        _state.value = VoiceAssistantState.PROCESSING
        
        // 這裡需要實作音頻錄製邏輯
        // 暫時使用模擬資料
        val mockAudioFile = File("mock_audio.wav")
        
        // 語音轉文字
        speechRepository.speechToText(mockAudioFile)
            .onSuccess { recognizedText ->
                if (recognizedText.isNotBlank()) {
                    processUserInput(recognizedText)
                } else {
                    _state.value = VoiceAssistantState.LISTENING
                }
            }
            .onFailure { error ->
                handleError("語音識別失敗: ${error.message}")
            }
    }
    
    /**
     * 處理使用者輸入
     */
    private suspend fun processUserInput(userInput: String) {
        android.util.Log.d("VoiceAssistant", "開始處理用戶輸入: $userInput")
        
        // 新增到對話歷史
        conversationMessages.add(userInput)
        
        // AI對話處理
        speechRepository.processAiConversation(userInput, conversationMessages)
            .onSuccess { aiResponse ->
                android.util.Log.d("VoiceAssistant", "AI回應成功: $aiResponse")
                conversationMessages.add(aiResponse)
                
                // 新增到UI顯示的對話歷史
                addConversationItem(userInput, aiResponse)
                
                // 語音合成
                speakResponse(aiResponse)
            }
            .onFailure { error ->
                android.util.Log.e("VoiceAssistant", "AI對話處理失敗: ${error.message}")
                handleError("AI對話處理失敗: ${error.message}")
            }
    }
    
    /**
     * 語音合成並播放AI回應
     */
    private suspend fun speakResponse(response: String) {
        _state.value = VoiceAssistantState.SPEAKING
        
        speechRepository.textToSpeech(response)
            .onSuccess {
                // TTS播放完成，返回監聽狀態
                _state.value = if (isFreeMode || _state.value == VoiceAssistantState.DETECTING) {
                    VoiceAssistantState.LISTENING
                } else {
                    VoiceAssistantState.IDLE
                }
            }
            .onFailure { error ->
                handleError("語音合成失敗: ${error.message}")
            }
    }
    
    /**
     * 新增對話項到歷史記錄
     */
    private fun addConversationItem(userInput: String, aiResponse: String) {
        android.util.Log.d("VoiceAssistant", "準備添加對話項 - 用戶: $userInput, AI: $aiResponse")
        
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
        
        android.util.Log.d("VoiceAssistant", "對話項已添加，當前對話歷史數量: ${currentHistory.size}")
    }
    
    /**
     * 切換自由模式
     */
    fun toggleFreeMode() {
        isFreeMode = !isFreeMode
        
        if (isFreeMode) {
            // 進入自由模式，開始持續監聽
            _state.value = VoiceAssistantState.LISTENING
        } else {
            // 退出自由模式
            if (_state.value == VoiceAssistantState.LISTENING) {
                _state.value = VoiceAssistantState.IDLE
            }
        }
    }
    
    /**
     * 手動停止TTS（用於插話打斷）
     */
    fun interruptSpeaking() {
        if (_state.value == VoiceAssistantState.SPEAKING) {
            speechRepository.stopTTS()
            _state.value = VoiceAssistantState.LISTENING
        }
    }
    
    /**
     * 清除對話歷史
     */
    fun clearConversationHistory() {
        _conversationHistory.value = emptyList()
        conversationMessages.clear()
    }
    
    /**
     * 手動測試語音輸入（用於測試功能）
     * 這個方法會隨機選擇一個測試語句，模擬完整的語音交互流程
     */
    suspend fun startManualVoiceTest() {
        val testInputs = listOf(
            "你好，今天天氣怎麼樣？",
            "請告訴我一個笑話",
            "現在幾點了？",
            "你可以做什麼？",
            "謝謝你的幫助",
            "播放音樂",
            "設定鬧鐘明天早上7點",
            "今天的新聞有什麼？",
            "幫我查詢天氣預報",
            "我想學習新的知識"
        )
        
        _state.value = VoiceAssistantState.PROCESSING
        
        // 隨機選擇測試輸入
        val randomInput = testInputs.random()
        
        // 添加日誌
        android.util.Log.d("VoiceAssistant", "開始測試語音輸入: $randomInput")
        
        // 直接處理用戶輸入，跳過語音識別步驟
        processUserInput(randomInput)
    }
    
    /**
     * 處理錯誤
     */
    private fun handleError(errorMessage: String) {
        _state.value = VoiceAssistantState.ERROR
        
        // 發送錯誤訊息到UI層
        kotlinx.coroutines.GlobalScope.launch {
            _errorMessage.emit(errorMessage)
        }
        
        // 3秒後自動恢復到空闒狀態
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(3000)
            _state.value = VoiceAssistantState.IDLE
        }
    }
    
    /**
     * 釋放資源
     */
    fun release() {
        vadRepository.release()
        speechRepository.stopTTS()
    }
}
