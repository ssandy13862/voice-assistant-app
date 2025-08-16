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

    // 语音助理状态
    val assistantState: StateFlow<VoiceAssistantState> = voiceAssistantUseCase.state

    // 对话历史
    val conversationHistory: StateFlow<List<ConversationItem>> = voiceAssistantUseCase.conversationHistory

    // 人脸检测结果
    private val _faceDetectionResult = MutableStateFlow<FaceDetectionResult?>(null)
    val faceDetectionResult: StateFlow<FaceDetectionResult?> = _faceDetectionResult.asStateFlow()

    // 自由模式状态
    private val _isFreeMode = MutableStateFlow(false)
    val isFreeMode: StateFlow<Boolean> = _isFreeMode.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    // 权限状态
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    /**
     * 处理相机图像帧进行人脸检测
     */
    fun processCameraFrame(imageProxy: ImageProxy) {
        viewModelScope.launch {
            try {
                val result = voiceAssistantUseCase.processFaceDetection(imageProxy)
                _faceDetectionResult.value = result
            } catch (e: Exception) {
                _errorMessage.emit("人脸检测错误: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }
    }

    /**
     * 切换自由模式
     */
    fun toggleFreeMode() {
        voiceAssistantUseCase.toggleFreeMode()
        _isFreeMode.value = !_isFreeMode.value
    }

    /**
     * 手动触发语音输入（用于测试）
     */
    fun startManualVoiceInput() {
        viewModelScope.launch {
            try {
                // 模拟语音输入
                val mockInput = "你好，今天天气怎么样？"
                // voiceAssistantUseCase.processUserInput(mockInput)
            } catch (e: Exception) {
                _errorMessage.emit("语音输入错误: ${e.message}")
            }
        }
    }

    /**
     * 中断TTS播放
     */
    fun interruptSpeaking() {
        voiceAssistantUseCase.interruptSpeaking()
    }

    /**
     * 清除对话历史
     */
    fun clearConversationHistory() {
        voiceAssistantUseCase.clearConversationHistory()
    }

    /**
     * 设置权限状态
     */
    fun setPermissionsGranted(granted: Boolean) {
        _permissionsGranted.value = granted
    }

    /**
     * 获取状态显示文本
     */
    fun getStateDisplayText(state: VoiceAssistantState): String {
        return when (state) {
            VoiceAssistantState.IDLE -> "待机中"
            VoiceAssistantState.DETECTING -> "检测人脸中"
            VoiceAssistantState.LISTENING -> "聆听中"
            VoiceAssistantState.PROCESSING -> "处理中"
            VoiceAssistantState.SPEAKING -> "说话中"
            VoiceAssistantState.ERROR -> "错误状态"
        }
    }

    /**
     * 获取状态颜色资源ID
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
