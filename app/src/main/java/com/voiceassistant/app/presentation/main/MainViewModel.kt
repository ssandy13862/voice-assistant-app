package com.voiceassistant.app.presentation.main

import android.app.Application
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
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
    application: Application,
    private val voiceAssistantUseCase: VoiceAssistantUseCase,
    private val vadTestHelper: com.voiceassistant.app.utils.VadTestHelper
) : AndroidViewModel(application) {

    init {
        // 觀察UseCase的錯誤訊息並轉發到UI
        viewModelScope.launch {
            voiceAssistantUseCase.errorMessage.collect { error ->
                _errorMessage.emit(error)
            }
        }
        
        // 觀察模型下載失敗事件
        viewModelScope.launch {
            voiceAssistantUseCase.modelDownloadFailed.collect { message ->
                _modelDownloadFailed.emit(message)
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
    
    // 模型下載失敗事件
    private val _modelDownloadFailed = MutableSharedFlow<String>()
    val modelDownloadFailed: SharedFlow<String> = _modelDownloadFailed.asSharedFlow()

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
     * 執行 VAD 診斷（長按測試按鈕觸發）
     */
    fun runVadDiagnosis() {
        viewModelScope.launch {
            try {
                val diagnosis = vadTestHelper.diagnoseVadInitialization()
                android.util.Log.i("MainViewModel", diagnosis)
                _errorMessage.emit("診斷完成，請查看 Logcat 的 VadTestHelper 和 MainViewModel 標籤")
            } catch (e: Exception) {
                _errorMessage.emit("診斷失敗: ${e.message}")
            }
        }
    }

    /**
     * 執行 Silero VAD 完整測試（包含 Whisper STT 測試）
     */
    fun testSileroVad() {
        viewModelScope.launch {
            try {
                android.util.Log.i("MainViewModel", "開始執行完整語音系統測試...")
                _errorMessage.emit("開始語音系統測試（VAD + Whisper STT），請查看 Logcat")
                
                val testResult = vadTestHelper.testSileroVad()
                
                android.util.Log.i("MainViewModel", "語音系統測試結果: $testResult")
                val statusMessage = if (testResult) {
                    "語音系統測試成功！VAD 和 Whisper 都正常運作"
                } else {
                    "語音系統測試失敗，請查看 Logcat 詳情"
                }
                _errorMessage.emit(statusMessage)
                
            } catch (e: Exception) {
                _errorMessage.emit("測試異常: ${e.message}")
            }
        }
    }
    
    /**
     * 測試 Whisper 語音轉文字功能（使用真實音頻檔案）
     */
    fun testWhisperSTT() {
        viewModelScope.launch {
            try {
                android.util.Log.i("MainViewModel", "開始測試 Whisper STT...")
                _errorMessage.emit("測試 Whisper 語音轉文字中...")
                
                // 測試現有的音頻檔案
                val testAudioFiles = listOf("morning.wav", "breakfesttt.wav")
                val speechRepository = voiceAssistantUseCase.getSpeechRepository()
                
                for (audioFileName in testAudioFiles) {
                    try {
                        // 從 assets 複製到臨時檔案
                        val context = getApplication<Application>()
                        val tempFile = java.io.File(context.cacheDir, "test_$audioFileName")
                        context.assets.open(audioFileName).use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        android.util.Log.i("MainViewModel", "測試音頻檔案: $audioFileName (${tempFile.length()} bytes)")
                        
                        // 調用 Whisper 轉錄
                        val result = speechRepository.speechToText(tempFile)
                        
                        if (result.isSuccess) {
                            val transcription = result.getOrNull() ?: ""
                            android.util.Log.i("MainViewModel", "Whisper 轉錄結果 ($audioFileName): '$transcription'")
                            _errorMessage.emit("轉錄結果 ($audioFileName): '$transcription'")
                        } else {
                            android.util.Log.e("MainViewModel", "轉錄失敗 ($audioFileName): ${result.exceptionOrNull()?.message}")
                            _errorMessage.emit("轉錄失敗 ($audioFileName): ${result.exceptionOrNull()?.message}")
                        }
                        
                        // 清理臨時檔案
                        tempFile.delete()
                        
                    } catch (e: Exception) {
                        android.util.Log.e("MainViewModel", "測試音頻檔案 $audioFileName 失敗", e)
                        _errorMessage.emit("測試 $audioFileName 失敗: ${e.message}")
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Whisper STT 測試異常", e)
                _errorMessage.emit("Whisper STT 測試異常: ${e.message}")
            }
        }
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
    
    /**
     * 重試 VAD 初始化
     */
    fun retryVadInitialization() {
        viewModelScope.launch {
            try {
                _errorMessage.emit("正在重新下載 VAD 模型...")
                // 通過重新初始化 VAD 來觸發重新下載
                val success = voiceAssistantUseCase.retryVadInitialization()
                if (success) {
                    _errorMessage.emit("VAD 模型重新下載成功！")
                } else {
                    _errorMessage.emit("VAD 模型重新下載失敗，請檢查網路連線")
                }
            } catch (e: Exception) {
                _errorMessage.emit("重試初始化失敗: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistantUseCase.release()
    }
}
