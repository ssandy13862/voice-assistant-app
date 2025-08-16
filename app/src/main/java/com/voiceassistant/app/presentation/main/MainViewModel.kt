package com.voiceassistant.app.presentation.main

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.app.domain.model.*
import com.voiceassistant.app.domain.usecase.VoiceAssistantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主界面ViewModel
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val voiceAssistantUseCase: VoiceAssistantUseCase
) : ViewModel() {

    init {
        // 觀察UseCase的錯誤訊息並轉發到UI
        viewModelScope.launch {
            voiceAssistantUseCase.errorMessage.collect { error ->
                _errorMessage.emit(error)
            }
        }
    }

    // 語音助理狀態
    val assistantState: StateFlow<VoiceAssistantState> = voiceAssistantUseCase.state

    // 對話歷史
    val conversationHistory: StateFlow<List<ConversationItem>> = voiceAssistantUseCase.conversationHistory

    // 人臉檢測結果
    private val _faceDetectionResult = MutableStateFlow<FaceDetectionResult?>(null)
    val faceDetectionResult: StateFlow<FaceDetectionResult?> = _faceDetectionResult.asStateFlow()

    // 自由模式狀態
    private val _isFreeMode = MutableStateFlow(false)
    val isFreeMode: StateFlow<Boolean> = _isFreeMode.asStateFlow()

    // 錯誤訊息
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    // 權限狀態
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    // 幀處理控制
    private var lastProcessingTime = 0L
    private val processingInterval = 500L // 每500毫秒處理一幀（2fps）

    /**
     * 處理相機圖像幀進行人臉檢測
     */
    fun processCameraFrame(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // 限制處理頻率，避免過度的人臉檢測
        if (currentTime - lastProcessingTime < processingInterval) {
            imageProxy.close()
            return
        }
        
        lastProcessingTime = currentTime
        
        viewModelScope.launch {
            try {
                val result = voiceAssistantUseCase.processFaceDetection(imageProxy)
                _faceDetectionResult.value = result
            } catch (e: Exception) {
                _errorMessage.emit("人臉檢測錯誤: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }
    }

    /**
     * 切換自由模式
     */
    fun toggleFreeMode() {
        voiceAssistantUseCase.toggleFreeMode()
        _isFreeMode.value = !_isFreeMode.value
    }

    /**
     * 手動觸發語音輸入（用於測試）
     */
    fun startManualVoiceInput() {
        viewModelScope.launch {
            try {
                // 調用語音助理的測試功能
                voiceAssistantUseCase.startManualVoiceTest()
            } catch (e: Exception) {
                _errorMessage.emit("語音輸入錯誤: ${e.message}")
            }
        }
    }

    /**
     * 中斷TTS播放
     */
    fun interruptSpeaking() {
        voiceAssistantUseCase.interruptSpeaking()
    }

    /**
     * 清除對話歷史
     */
    fun clearConversationHistory() {
        voiceAssistantUseCase.clearConversationHistory()
    }

    /**
     * 設置權限狀態
     */
    fun setPermissionsGranted(granted: Boolean) {
        _permissionsGranted.value = granted
    }

    /**
     * 獲取狀態顯示文本
     */
    fun getStateDisplayText(state: VoiceAssistantState): String {
        return when (state) {
            VoiceAssistantState.IDLE -> "待機中"
            VoiceAssistantState.DETECTING -> "檢測人臉中"
            VoiceAssistantState.LISTENING -> "聆聽中"
            VoiceAssistantState.PROCESSING -> "處理中"
            VoiceAssistantState.SPEAKING -> "說話中"
            VoiceAssistantState.ERROR -> "錯誤狀態"
        }
    }

    /**
     * 獲取狀態顏色資源ID
     */
    fun getStateColor(state: VoiceAssistantState): Int {
        return when (state) {
            VoiceAssistantState.IDLE -> android.R.color.darker_gray
            VoiceAssistantState.DETECTING -> android.R.color.holo_blue_light
            VoiceAssistantState.LISTENING -> android.R.color.holo_green_light
            VoiceAssistantState.PROCESSING -> android.R.color.holo_orange_light
            VoiceAssistantState.SPEAKING -> android.R.color.holo_purple
            VoiceAssistantState.ERROR -> android.R.color.holo_red_light
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistantUseCase.release()
    }
}
