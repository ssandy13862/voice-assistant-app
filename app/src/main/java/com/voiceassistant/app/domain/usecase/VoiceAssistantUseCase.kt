package com.voiceassistant.app.domain.usecase

import androidx.camera.core.ImageProxy
import com.voiceassistant.app.domain.model.*
import com.voiceassistant.app.domain.repository.FaceDetectionRepository
import com.voiceassistant.app.domain.repository.SpeechRepository
import com.voiceassistant.app.domain.repository.VadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.nio.ByteOrder
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
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    private val _state = MutableStateFlow(VoiceAssistantState.IDLE)
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()
    
    private val _conversationHistory = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationItem>> = _conversationHistory.asStateFlow()
    
    // 新增錯誤訊息流
    private val _errorMessage = MutableSharedFlow<String>()
    
    // 新增模型下載失敗事件流
    private val _modelDownloadFailed = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()
    val modelDownloadFailed: SharedFlow<String> = _modelDownloadFailed.asSharedFlow()
    
    private val conversationMessages = mutableListOf<String>()
    private var isFreeMode = false
    private var vadInitialized = false
    
    init {
        // 在初始化時就嘗試預載 VAD 模型
        coroutineScope.launch {
            preloadVadModel()
        }
    }
    
    /**
     * 預載 VAD 模型（在背景執行）
     */
    private suspend fun preloadVadModel() {
        try {
            android.util.Log.i("VoiceAssistantUseCase", "開始預載 VAD 模型...")
            vadInitialized = vadRepository.initializeVad()
            android.util.Log.i("VoiceAssistantUseCase", "VAD 模型預載結果: $vadInitialized")
            
            if (!vadInitialized) {
                _errorMessage.emit("預載 VAD 模型失敗，將使用簡化 VAD")
                _modelDownloadFailed.emit("Silero VAD 模型下載失敗，可能是網路連線問題。應用程式將使用簡化的語音檢測功能。")
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceAssistantUseCase", "預載 VAD 模型異常", e)
            _errorMessage.emit("VAD 模型初始化異常: ${e.message}")
        }
    }
    
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
        android.util.Log.i("VoiceAssistantUseCase", "嘗試開始聲音監聽...")
        
        // 如果 VAD 尚未初始化，嘗試初始化
        if (!vadInitialized) {
            android.util.Log.i("VoiceAssistantUseCase", "VAD 尚未初始化，嘗試再次初始化...")
            vadInitialized = vadRepository.initializeVad()
            android.util.Log.i("VoiceAssistantUseCase", "VAD 初始化結果: $vadInitialized")
        }
        
        if (!vadInitialized) {
            android.util.Log.e("VoiceAssistantUseCase", "VAD 初始化失敗，設置為錯誤狀態")
            _state.value = VoiceAssistantState.ERROR
            _errorMessage.emit("VAD 初始化失敗，將使用簡化語音檢測")
            _modelDownloadFailed.emit("無法初始化 Silero VAD 模型。請檢查網路連線並重新啟動應用程式，或繼續使用簡化語音檢測功能。")
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
        
        try {
            // 停止VAD監聽，開始錄製音頻
            vadRepository.stopAudioDetection()
            
            // 錄製語音（錄製3秒鐘的音頻）
            val audioFile = recordAudioToFile()
            
            if (audioFile != null && audioFile.exists()) {
                // 語音轉文字
                speechRepository.speechToText(audioFile)
                    .onSuccess { recognizedText ->
                        if (recognizedText.isNotBlank()) {
                            processUserInput(recognizedText)
                        } else {
                            android.util.Log.d("VoiceAssistant", "沒有識別到有效語音")
                            _state.value = VoiceAssistantState.LISTENING
                            if (isFreeMode) {
                                startListening() // 重新開始監聽
                            }
                        }
                    }
                    .onFailure { error ->
                        handleError("語音識別失敗: ${error.message}")
                    }
                
                // 清理音頻檔案
                try {
                    audioFile.delete()
                } catch (e: Exception) {
                    android.util.Log.w("VoiceAssistant", "清理音頻檔案失敗: ${e.message}")
                }
            } else {
                android.util.Log.e("VoiceAssistant", "音頻錄製失敗")
                handleError("音頻錄製失敗")
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceAssistant", "處理語音檢測失敗", e)
            handleError("處理語音檢測失敗: ${e.message}")
        }
    }
    
    /**
     * 錄製音頻到檔案
     */
    private suspend fun recordAudioToFile(): File? {
        return withContext(Dispatchers.IO) {
            try {
                val outputFile = File.createTempFile("voice_recording", ".wav")
                
                // 初始化音頻錄製器
                val sampleRate = 16000
                val channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO
                val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = android.media.AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2
                
                val audioRecord = android.media.AudioRecord(
                    android.media.MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                
                if (audioRecord.state != android.media.AudioRecord.STATE_INITIALIZED) {
                    android.util.Log.e("VoiceAssistant", "AudioRecord 初始化失敗")
                    return@withContext null
                }
                
                val buffer = ShortArray(bufferSize / 2)
                val outputStream = java.io.FileOutputStream(outputFile)
                val dataOutputStream = java.io.DataOutputStream(outputStream)
                
                // 寫入WAV檔案頭（預留空間給檔案大小）
                val headerPosition = dataOutputStream.size()
                writeWavHeader(dataOutputStream, sampleRate, 1, 16)
                val dataStartPosition = dataOutputStream.size()
                
                android.util.Log.d("VoiceAssistant", "開始錄製音頻...")
                audioRecord.startRecording()
                
                var totalDataBytes = 0
                
                // 錄製3秒鐘
                val recordingDuration = 3000 // 3秒
                val startTime = System.currentTimeMillis()
                
                while (System.currentTimeMillis() - startTime < recordingDuration) {
                    val readSize = audioRecord.read(buffer, 0, buffer.size)
                    if (readSize > 0) {
                        // 寫入PCM數據
                        for (i in 0 until readSize) {
                            dataOutputStream.writeShort(buffer[i].toInt())
                            totalDataBytes += 2 // 16-bit = 2 bytes
                        }
                    }
                }
                
                audioRecord.stop()
                audioRecord.release()
                dataOutputStream.close()
                outputStream.close()
                
                // 更新WAV檔案頭的檔案大小
                updateWavHeader(outputFile, totalDataBytes)
                
                android.util.Log.d("VoiceAssistant", "音頻錄製完成: ${outputFile.length()} bytes, PCM數據: $totalDataBytes bytes")
                outputFile
                
            } catch (e: Exception) {
                android.util.Log.e("VoiceAssistant", "錄製音頻失敗", e)
                null
            }
        }
    }
    
    /**
     * 更新WAV檔案頭的檔案大小
     */
    private fun updateWavHeader(file: File, dataSize: Int) {
        try {
            val randomAccessFile = java.io.RandomAccessFile(file, "rw")
            
            // 更新RIFF chunk size (total file size - 8)
            randomAccessFile.seek(4)
            randomAccessFile.writeInt(java.lang.Integer.reverseBytes(dataSize + 36))
            
            // 更新data chunk size
            randomAccessFile.seek(40)
            randomAccessFile.writeInt(java.lang.Integer.reverseBytes(dataSize))
            
            randomAccessFile.close()
            android.util.Log.d("VoiceAssistant", "WAV檔案頭已更新，數據大小: $dataSize bytes")
        } catch (e: Exception) {
            android.util.Log.e("VoiceAssistant", "更新WAV檔案頭失敗", e)
        }
    }
    
    /**
     * 寫入WAV檔案頭
     */
    private fun writeWavHeader(dataOutputStream: java.io.DataOutputStream, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        
        // WAV檔案頭（Little Endian 格式）
        dataOutputStream.writeBytes("RIFF")
        dataOutputStream.writeInt(java.lang.Integer.reverseBytes(0)) // 檔案大小，稍後更新
        dataOutputStream.writeBytes("WAVE")
        dataOutputStream.writeBytes("fmt ")
        dataOutputStream.writeInt(java.lang.Integer.reverseBytes(16)) // PCM格式大小
        dataOutputStream.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // PCM格式
        dataOutputStream.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
        dataOutputStream.writeInt(java.lang.Integer.reverseBytes(sampleRate))
        dataOutputStream.writeInt(java.lang.Integer.reverseBytes(byteRate))
        dataOutputStream.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt())
        dataOutputStream.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
        dataOutputStream.writeBytes("data")
        dataOutputStream.writeInt(java.lang.Integer.reverseBytes(0)) // 數據大小，稍後更新
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
            coroutineScope.launch {
                try {
                    val vadInitialized = vadRepository.initializeVad()
                    if (vadInitialized) {
                        _state.value = VoiceAssistantState.LISTENING
                        startListening()
                    } else {
                        _errorMessage.emit("VAD 初始化失敗，無法啟動自由模式")
                        isFreeMode = false
                    }
                } catch (e: Exception) {
                    _errorMessage.emit("啟動自由模式失敗: ${e.message}")
                    isFreeMode = false
                }
            }
        } else {
            // 退出自由模式
            stopListening()
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
     * 這個方法會實際錄製語音並測試語音識別
     */
    suspend fun startManualVoiceTest() {
        android.util.Log.i("VoiceAssistant", "開始手動語音測試...")
        _state.value = VoiceAssistantState.PROCESSING
        
        try {
            // 直接錄製語音並測試識別
            android.util.Log.d("VoiceAssistant", "開始錄製語音測試...")
            val audioFile = recordAudioToFile()
            
            if (audioFile != null && audioFile.exists()) {
                android.util.Log.d("VoiceAssistant", "音頻錄製成功，開始語音識別...")
                
                // 語音轉文字
                speechRepository.speechToText(audioFile)
                    .onSuccess { recognizedText ->
                        android.util.Log.i("VoiceAssistant", "語音識別成功: $recognizedText")
                        if (recognizedText.isNotBlank()) {
                            _errorMessage.emit("語音識別結果: $recognizedText")
                            // 處理用戶輸入
                            processUserInput(recognizedText)
                        } else {
                            android.util.Log.w("VoiceAssistant", "語音識別結果為空")
                            _errorMessage.emit("沒有識別到有效語音，請再試一次")
                            _state.value = VoiceAssistantState.IDLE
                        }
                    }
                    .onFailure { error ->
                        android.util.Log.e("VoiceAssistant", "語音識別失敗", error)
                        _errorMessage.emit("語音識別失敗: ${error.message}")
                        _state.value = VoiceAssistantState.IDLE
                    }
                
                // 清理暂存檔案
                try {
                    audioFile.delete()
                } catch (e: Exception) {
                    android.util.Log.w("VoiceAssistant", "清理暂存檔案失敗: ${e.message}")
                }
            } else {
                android.util.Log.e("VoiceAssistant", "音頻錄製失敗")
                _errorMessage.emit("音頻錄製失敗，請檢查麥克風權限")
                _state.value = VoiceAssistantState.IDLE
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceAssistant", "手動語音測試異常", e)
            _errorMessage.emit("語音測試失敗: ${e.message}")
            _state.value = VoiceAssistantState.IDLE
        }
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
     * 重試 VAD 初始化
     */
    suspend fun retryVadInitialization(): Boolean {
        return try {
            android.util.Log.i("VoiceAssistantUseCase", "重試 VAD 初始化...")
            
            // 重置狀態
            vadInitialized = false
            
            // 重新初始化 VAD
            vadInitialized = vadRepository.initializeVad()
            
            if (vadInitialized) {
                android.util.Log.i("VoiceAssistantUseCase", "VAD 重新初始化成功")
                // 如果之前處於錯誤狀態，恢復到待機狀態
                if (_state.value == VoiceAssistantState.ERROR) {
                    _state.value = VoiceAssistantState.IDLE
                }
            } else {
                android.util.Log.w("VoiceAssistantUseCase", "VAD 重新初始化失敗")
            }
            
            vadInitialized
        } catch (e: Exception) {
            android.util.Log.e("VoiceAssistantUseCase", "重試 VAD 初始化異常", e)
            false
        }
    }
    
    /**
     * 釋放資源
     */
    fun release() {
        vadRepository.release()
        speechRepository.stopTTS()
    }
    
    /**
     * 獲取 Speech Repository (用於測試)
     */
    fun getSpeechRepository(): SpeechRepository {
        return speechRepository
    }
}
