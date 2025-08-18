package com.voiceassistant.app.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import com.voiceassistant.app.domain.model.AudioState
import com.voiceassistant.app.domain.repository.VadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.log10

/**
 * 語音活動檢測實作
 * 使用 Silero VAD 模型進行準確的語音檢測
 * 整合了 ONNX Runtime 推理引擎
 */
@Singleton
class VadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whisperNativeRepository: WhisperNativeRepository,
    private val audioRecorderRepository: AudioRecorderRepository,
    private val sileroVadRepository: SileroVadRepository
) : VadRepository, AudioRecorderRepository.AudioProcessingCallback {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var vadThreshold = 0.5f // Silero VAD 閾值（0.0-1.0）
    private var useSileroVad = true // 是否使用 Silero VAD
    private var sileroFailureCount = 0 // Silero VAD 失敗計數器
    private val maxFailures = 3 // 最大失敗次數後自動禁用
    
    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }

    override suspend fun initializeVad(): Boolean {
        return try {
            if (useSileroVad) {
                // 使用 Silero VAD 實作
                android.util.Log.i("VadRepository", "嘗試初始化 Silero VAD...")
                val sileroSuccess = sileroVadRepository.initialize()
                val audioSuccess = audioRecorderRepository.initialize()
                
                if (sileroSuccess && audioSuccess) {
                    audioRecorderRepository.setAudioProcessingCallback(this)
                    android.util.Log.i("VadRepository", "Silero VAD 初始化成功")
                    return true
                } else {
                    android.util.Log.w("VadRepository", "Silero VAD 初始化失敗，嘗試使用簡化 VAD")
                    // 如果 Silero VAD 失敗，自動切換到簡化 VAD
                    useSileroVad = false
                    return initializeSimpleVad()
                }
            } else {
                return initializeSimpleVad()
            }
        } catch (e: Exception) {
            android.util.Log.e("VadRepository", "VAD 初始化異常", e)
            // 如果出現異常，嘗試使用簡化 VAD
            useSileroVad = false
            return try {
                initializeSimpleVad()
            } catch (e2: Exception) {
                android.util.Log.e("VadRepository", "簡化 VAD 初始化也失敗", e2)
                false
            }
        }
    }
    
    /**
     * 檢查音頻權限
     */
    private fun checkAudioPermission(): Boolean {
        val hasPermission = ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        android.util.Log.d("VadRepository", "音頻權限狀態: $hasPermission")
        return hasPermission
    }
    
    /**
     * 初始化簡化 VAD（備用方案）
     */
    private fun initializeSimpleVad(): Boolean {
        return try {
            android.util.Log.i("VadRepository", "初始化簡化 VAD...")
            
            // 檢查權限
            if (!checkAudioPermission()) {
                android.util.Log.e("VadRepository", "缺少音頻錄製權限")
                return false
            }
            
            // 清理之前的 AudioRecord
            audioRecord?.let {
                if (it.state == AudioRecord.STATE_INITIALIZED) {
                    it.stop()
                    it.release()
                }
            }
            
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            
            if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                android.util.Log.e("VadRepository", "無效的緩衝區大小: $bufferSize")
                return false
            }
            
            val actualBufferSize = bufferSize * BUFFER_SIZE_MULTIPLIER
            android.util.Log.d("VadRepository", "緩衝區大小: $actualBufferSize")

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                actualBufferSize
            )
            
            val state = audioRecord?.state
            val recordingState = audioRecord?.recordingState
            
            android.util.Log.d("VadRepository", "AudioRecord 狀態: $state, 錄音狀態: $recordingState")
            
            val isInitialized = state == AudioRecord.STATE_INITIALIZED
            
            if (!isInitialized) {
                android.util.Log.e("VadRepository", "AudioRecord 初始化失敗，狀態: $state")
                audioRecord?.release()
                audioRecord = null
            }
            
            android.util.Log.i("VadRepository", "簡化 VAD 初始化結果: $isInitialized")
            isInitialized
            
        } catch (e: Exception) {
            android.util.Log.e("VadRepository", "簡化 VAD 初始化失敗", e)
            audioRecord?.release()
            audioRecord = null
            false
        }
    }

    override fun startAudioDetection(): Flow<AudioState> = callbackFlow {
        android.util.Log.i("VadRepository", "開始音頻檢測，使用 ${if (useSileroVad) "Silero VAD" else "簡化 VAD"}")
        
        if (useSileroVad) {
            // 使用 Silero VAD + AudioRecord 實作
            if (!initializeVad()) {
                android.util.Log.e("VadRepository", "VAD 初始化失敗")
                trySend(AudioState.SILENT)
                close()
                return@callbackFlow
            }
            
            android.util.Log.i("VadRepository", "VAD 初始化成功，現在使用 ${if (useSileroVad) "Silero VAD" else "簡化 VAD"}")
            
            isRecording = true
            val success = audioRecorderRepository.startRecording()
            
            if (!success) {
                trySend(AudioState.SILENT)
                close()
                return@callbackFlow
            }
            
            // Silero VAD 會通過回調發送狀態更新
            // 這裡主要是保持 Flow 活躍
            awaitClose {
                stopAudioDetection()
            }
        } else {
            // 使用簡化 VAD 實作
            if (audioRecord == null || !initializeVad()) {
                android.util.Log.e("VadRepository", "簡化 VAD 初始化失敗")
                trySend(AudioState.SILENT)
                close()
                return@callbackFlow
            }
            
            android.util.Log.i("VadRepository", "簡化 VAD 初始化成功")

            // 檢查 AudioRecord 狀態
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                android.util.Log.e("VadRepository", "AudioRecord 未正確初始化")
                trySend(AudioState.SILENT)
                close()
                return@callbackFlow
            }

            isRecording = true
            
            try {
                audioRecord?.startRecording()
                
                // 檢查錄音狀態
                if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    android.util.Log.e("VadRepository", "無法開始錄音，狀態: ${audioRecord?.recordingState}")
                    trySend(AudioState.SILENT)
                    close()
                    return@callbackFlow
                }
                
                android.util.Log.i("VadRepository", "錄音開始成功")

                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                )
                val audioBuffer = ShortArray(bufferSize)

                Thread {
                    android.util.Log.d("VadRepository", "音頻處理線程開始")
                    try {
                        while (isRecording && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            val readSize = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                            
                            when {
                                readSize > 0 -> {
                                    val audioState = processAudioBuffer(audioBuffer, readSize)
                                    trySend(audioState)
                                }
                                readSize == AudioRecord.ERROR_INVALID_OPERATION -> {
                                    android.util.Log.e("VadRepository", "AudioRecord 無效操作錯誤 (-38)")
                                    break
                                }
                                readSize == AudioRecord.ERROR_BAD_VALUE -> {
                                    android.util.Log.e("VadRepository", "AudioRecord 參數錯誤")
                                    break
                                }
                                readSize < 0 -> {
                                    android.util.Log.e("VadRepository", "AudioRecord 讀取錯誤: $readSize")
                                    break
                                }
                            }
                            
                            // 短暫停避免過度消耗 CPU
                            Thread.sleep(10)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("VadRepository", "音頻處理線程異常", e)
                    } finally {
                        android.util.Log.d("VadRepository", "音頻處理線程結束")
                    }
                }.start()
                
            } catch (e: Exception) {
                android.util.Log.e("VadRepository", "開始錄音失敗", e)
                trySend(AudioState.SILENT)
                close()
                return@callbackFlow
            }

            awaitClose {
                stopAudioDetection()
            }
        }
    }

    override fun stopAudioDetection() {
        android.util.Log.i("VadRepository", "停止音頻檢測")
        isRecording = false
        
        if (useSileroVad) {
            audioRecorderRepository.stopRecording()
            sileroVadRepository.resetStates() // 重置 Silero VAD 狀態
        } else {
            try {
                audioRecord?.let { record ->
                    if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        record.stop()
                        android.util.Log.d("VadRepository", "AudioRecord 停止成功")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VadRepository", "停止 AudioRecord 失敗", e)
            }
        }
    }

    override suspend fun processAudioData(audioData: ByteArray): AudioState {
        if (useSileroVad) {
            // 轉換字節數組為浮點數組（Silero VAD 使用浮點輸入）
            val floatArray = convertBytesToFloat(audioData)
            
            return try {
                val (isVoice, probability) = sileroVadRepository.detectVoiceActivity(floatArray)
                
                when {
                    isVoice && probability > vadThreshold -> AudioState.SPEAKING
                    probability > vadThreshold * 0.3f -> AudioState.NOISE
                    else -> AudioState.SILENT
                }
            } catch (e: Exception) {
                android.util.Log.e("VadRepository", "Silero VAD 處理失敗，使用備用方法", e)
                sileroFailureCount++
                android.util.Log.w("VadRepository", "Silero VAD 失敗次數: $sileroFailureCount/$maxFailures")
                
                // 如果失敗次數過多，自動禁用 Silero VAD
                if (sileroFailureCount >= maxFailures) {
                    android.util.Log.w("VadRepository", "連續失敗 $maxFailures 次，自動禁用 Silero VAD")
                    useSileroVad = false
                }
                // 備用方案：使用簡單的能量檢測
                val shortArray = convertBytesToShort(audioData)
                processAudioBuffer(shortArray, shortArray.size)
            }
        } else {
            // 使用原有的簡單 VAD
            val shortArray = convertBytesToShort(audioData)
            return processAudioBuffer(shortArray, shortArray.size)
        }
    }

    override fun setVadThreshold(threshold: Float) {
        vadThreshold = threshold.coerceIn(0.001f, 1.0f)
    }

    override fun release() {
        android.util.Log.i("VadRepository", "釋放 VAD 資源")
        stopAudioDetection()
        
        if (useSileroVad) {
            audioRecorderRepository.cleanup()
            sileroVadRepository.cleanup()
            whisperNativeRepository.cleanup()
        } else {
            try {
                audioRecord?.let { record ->
                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            record.stop()
                        }
                        record.release()
                        android.util.Log.d("VadRepository", "AudioRecord 資源釋放成功")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VadRepository", "釋放 AudioRecord 資源失敗", e)
            } finally {
                audioRecord = null
            }
        }
    }
    
    // AudioProcessingCallback 接口實現
    
    override fun onAudioFrame(audioData: FloatArray, isVoice: Boolean, probability: Float) {
        // 如果使用 Silero VAD，會在這裡接收音頻幀並進行處理
        if (useSileroVad && audioData.isNotEmpty()) {
            // 異步處理 Silero VAD
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val (sileroIsVoice, sileroProbability) = sileroVadRepository.detectVoiceActivity(audioData)
                    
                    // 可以在這裡發送狀態更新到 Flow
                    // 注意：這需要與 callbackFlow 配合使用
                    android.util.Log.d("VadRepository", 
                        "Silero VAD: isVoice=$sileroIsVoice, probability=$sileroProbability")
                        
                } catch (e: Exception) {
                    android.util.Log.e("VadRepository", "Silero VAD 處理音頻幀失敗", e)
                }
            }
        }
    }
    
    override fun onSpeechDetected(audioData: FloatArray) {
        android.util.Log.d("VadRepository", "檢測到語音開始 (Silero VAD)")
    }
    
    override fun onSpeechEnd(audioData: FloatArray) {
        android.util.Log.d("VadRepository", "語音結束，樣本數: ${audioData.size} (Silero VAD)")
    }
    
    override fun onError(error: String) {
        android.util.Log.e("VadRepository", "音頻處理錯誤: $error")
    }

    // 輔助轉換函數
    
    /**
     * 將字節數組轉換為浮點數組（用於 Silero VAD）
     */
    private fun convertBytesToFloat(audioData: ByteArray): FloatArray {
        val shortArray = convertBytesToShort(audioData)
        val floatArray = FloatArray(shortArray.size)
        
        for (i in shortArray.indices) {
            // 將 16-bit PCM 轉換為 [-1.0, 1.0] 範圍的浮點數
            floatArray[i] = shortArray[i] / 32768.0f
        }
        
        return floatArray
    }
    
    /**
     * 將字節數組轉換為短整型數組
     */
    private fun convertBytesToShort(audioData: ByteArray): ShortArray {
        val shortArray = ShortArray(audioData.size / 2)
        for (i in shortArray.indices) {
            shortArray[i] = ((audioData[i * 2 + 1].toInt() shl 8) or 
                           (audioData[i * 2].toInt() and 0xFF)).toShort()
        }
        return shortArray
    }

    /**
     * 基於能量的簡化VAD演算法（備用方案）
     * 當 Silero VAD 不可用時使用
     */
    private fun processAudioBuffer(buffer: ShortArray, size: Int): AudioState {
        if (size == 0) return AudioState.SILENT

        // 計算音頻能量
        var energy = 0.0
        for (i in 0 until size) {
            energy += (buffer[i] * buffer[i]).toDouble()
        }
        energy /= size

        // 計算音量（分貝）
        val amplitude = kotlin.math.sqrt(energy)
        val db = if (amplitude > 0) {
            20 * log10(amplitude / 32768.0)
        } else {
            -100.0
        }

        // 基於閾值判斷語音活動
        return when {
            db > -30 && amplitude > vadThreshold * 32768 -> AudioState.SPEAKING
            db > -50 -> AudioState.NOISE
            else -> AudioState.SILENT
        }
    }
}
