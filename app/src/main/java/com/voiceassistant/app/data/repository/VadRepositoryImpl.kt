package com.voiceassistant.app.data.repository

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.voiceassistant.app.domain.model.AudioState
import com.voiceassistant.app.domain.repository.VadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.log10

/**
 * 語音活動檢測實作
 * 使用本地 VAD + AudioRecord 實現
 * 整合了原生 C++ VAD 算法
 */
@Singleton
class VadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whisperNativeRepository: WhisperNativeRepository,
    private val audioRecorderRepository: AudioRecorderRepository
) : VadRepository, AudioRecorderRepository.AudioProcessingCallback {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var vadThreshold = 0.01f // 預設閾值
    private var useNativeVad = true // 是否使用原生 VAD
    
    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }

    override suspend fun initializeVad(): Boolean {
        return try {
            if (useNativeVad) {
                // 使用本地 VAD 實現
                val vadSuccess = whisperNativeRepository.initializeVAD()
                val audioSuccess = audioRecorderRepository.initialize()
                
                if (vadSuccess && audioSuccess) {
                    audioRecorderRepository.setAudioProcessingCallback(this)
                    return true
                }
                return false
            } else {
                // 使用原有的 AudioRecord 實現
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                ) * BUFFER_SIZE_MULTIPLIER

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
                
                audioRecord?.state == AudioRecord.STATE_INITIALIZED
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun startAudioDetection(): Flow<AudioState> = callbackFlow {
        if (useNativeVad) {
            // 使用本地 VAD + AudioRecord 實現
            if (!initializeVad()) {
                trySend(AudioState.SILENT)
                close()
                return@callbackFlow
            }
            
            isRecording = true
            val success = audioRecorderRepository.startRecording()
            
            if (!success) {
                trySend(AudioState.SILENT)
                close()
                return@callbackFlow
            }
            
            // 本地 VAD 會通過回調發送狀態更新
            // 這裡主要是保持 Flow 活躍
            awaitClose {
                stopAudioDetection()
            }
        } else {
            // 使用原有的實現
            if (audioRecord == null || !initializeVad()) {
                trySend(AudioState.SILENT)
                close()
                return@callbackFlow
            }

            isRecording = true
            audioRecord?.startRecording()

            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            val audioBuffer = ShortArray(bufferSize)

            Thread {
                while (isRecording) {
                    val readSize = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                    if (readSize > 0) {
                        val audioState = processAudioBuffer(audioBuffer, readSize)
                        trySend(audioState)
                    }
                }
            }.start()

            awaitClose {
                stopAudioDetection()
            }
        }
    }

    override fun stopAudioDetection() {
        isRecording = false
        
        if (useNativeVad) {
            audioRecorderRepository.stopRecording()
        } else {
            audioRecord?.stop()
        }
    }

    override suspend fun processAudioData(audioData: ByteArray): AudioState {
        // 将字节数组转换为短整型数组
        val shortArray = ShortArray(audioData.size / 2)
        for (i in shortArray.indices) {
            shortArray[i] = ((audioData[i * 2 + 1].toInt() shl 8) or 
                           (audioData[i * 2].toInt() and 0xFF)).toShort()
        }
        
        return processAudioBuffer(shortArray, shortArray.size)
    }

    override fun setVadThreshold(threshold: Float) {
        vadThreshold = threshold.coerceIn(0.001f, 1.0f)
    }

    override fun release() {
        stopAudioDetection()
        
        if (useNativeVad) {
            audioRecorderRepository.cleanup()
            whisperNativeRepository.cleanup()
        } else {
            audioRecord?.release()
            audioRecord = null
        }
    }
    
    // AudioProcessingCallback 接口實現（用於本地 VAD）
    
    override fun onAudioFrame(audioData: FloatArray, isVoice: Boolean, probability: Float) {
        // 這個方法會在音頻處理線程中被調用
        // 暫時不在這裡更新狀態，因為 callbackFlow 需要特殊處理
    }
    
    override fun onSpeechDetected(audioData: FloatArray) {
        // 語音開始
        android.util.Log.d("VadRepository", "檢測到語音開始")
    }
    
    override fun onSpeechEnd(audioData: FloatArray) {
        // 語音結束
        android.util.Log.d("VadRepository", "語音結束，樣本數: ${audioData.size}")
    }
    
    override fun onError(error: String) {
        android.util.Log.e("VadRepository", "音頻處理錯誤: $error")
    }

    /**
     * 基於能量的簡化VAD演算法
     * 在實際專案中應該使用更先進的VAD演算法
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
