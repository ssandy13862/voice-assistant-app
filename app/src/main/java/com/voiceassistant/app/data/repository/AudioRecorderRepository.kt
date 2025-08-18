package com.voiceassistant.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 音頻錄製倉庫 - 使用 AudioRecord 進行實時音頻擷取
 */
@Singleton
class AudioRecorderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whisperNativeRepository: WhisperNativeRepository
) {
    
    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 16000 // Whisper 所需的採樣率
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_SIZE = 512 // VAD 幀大小
        private const val BUFFER_SIZE_MULTIPLIER = 4
    }
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    
    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
    ) * BUFFER_SIZE_MULTIPLIER
    
    // 狀態管理
    private val _isRecording = MutableStateFlow(false)
    val isRecordingState: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _audioLevel = MutableStateFlow(0.0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()
    
    private val _voiceDetected = MutableStateFlow(false)
    val voiceDetected: StateFlow<Boolean> = _voiceDetected.asStateFlow()
    
    // 音頻處理回調
    private var audioProcessingCallback: AudioProcessingCallback? = null
    
    interface AudioProcessingCallback {
        fun onAudioFrame(audioData: FloatArray, isVoice: Boolean, probability: Float)
        fun onSpeechDetected(audioData: FloatArray)
        fun onSpeechEnd(audioData: FloatArray)
        fun onError(error: String)
    }
    
    /**
     * 設置音頻處理回調
     */
    fun setAudioProcessingCallback(callback: AudioProcessingCallback?) {
        audioProcessingCallback = callback
    }
    
    /**
     * 檢查音頻錄製權限
     */
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 初始化音頻錄製器
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun initialize(): Boolean {
        if (!hasAudioPermission()) {
            Log.e(TAG, "沒有音頻錄製權限")
            return false
        }
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            val state = audioRecord?.state
            if (state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord 初始化失敗: state=$state")
                return false
            }
            
            Log.i(TAG, "AudioRecord 初始化成功")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord 初始化異常", e)
            return false
        }
    }
    
    /**
     * 開始錄製
     */
    fun startRecording(): Boolean {
        if (isRecording) {
            Log.w(TAG, "已經在錄製中")
            return true
        }
        
        if (audioRecord == null && !initialize()) {
            return false
        }
        
        return try {
            audioRecord?.startRecording()
            isRecording = true
            _isRecording.value = true
            
            // 啟動錄製線程
            recordingThread = Thread(::recordingLoop, "AudioRecordingThread")
            recordingThread?.start()
            
            Log.i(TAG, "開始錄製音頻")
            true
        } catch (e: Exception) {
            Log.e(TAG, "開始錄製失敗", e)
            false
        }
    }
    
    /**
     * 停止錄製
     */
    fun stopRecording() {
        if (!isRecording) {
            return
        }
        
        isRecording = false
        _isRecording.value = false
        
        try {
            audioRecord?.stop()
            recordingThread?.join(1000) // 等待線程結束
            recordingThread = null
            
            Log.i(TAG, "停止錄製音頻")
        } catch (e: Exception) {
            Log.e(TAG, "停止錄製異常", e)
        }
    }
    
    /**
     * 錄製循環
     */
    private fun recordingLoop() {
        val buffer = ByteArray(bufferSize)
        val floatBuffer = FloatArray(FRAME_SIZE)
        var speechBuffer = mutableListOf<Float>()
        var speechDetected = false
        var silenceFrameCount = 0
        
        Log.d(TAG, "開始錄製循環")
        
        while (isRecording) {
            try {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (bytesRead > 0) {
                    // 轉換為 float 數組 (16-bit PCM to float)
                    val floatData = convertBytesToFloat(buffer, bytesRead)
                    
                    // 計算音頻級別
                    val level = calculateAudioLevel(floatData)
                    _audioLevel.value = level
                    
                    // 以幀為單位處理 VAD
                    for (i in floatData.indices step FRAME_SIZE) {
                        val frameEnd = minOf(i + FRAME_SIZE, floatData.size)
                        val frameSize = frameEnd - i
                        
                        if (frameSize == FRAME_SIZE) {
                            // 複製幀數據
                            System.arraycopy(floatData, i, floatBuffer, 0, FRAME_SIZE)
                            
                            // VAD 檢測
                            val isVoice = whisperNativeRepository.detectVoiceActivity(floatBuffer)
                            val probability = whisperNativeRepository.getVoiceActivityProbability(floatBuffer)
                            
                            _voiceDetected.value = isVoice
                            
                            // 回調音頻幀
                            audioProcessingCallback?.onAudioFrame(floatBuffer, isVoice, probability)
                            
                            // 語音片段管理
                            if (isVoice) {
                                if (!speechDetected) {
                                    // 開始檢測到語音
                                    speechDetected = true
                                    speechBuffer.clear()
                                    audioProcessingCallback?.onSpeechDetected(floatBuffer)
                                    Log.d(TAG, "檢測到語音開始")
                                }
                                
                                // 添加到語音緩衝區
                                speechBuffer.addAll(floatBuffer.toList())
                                silenceFrameCount = 0
                            } else {
                                if (speechDetected) {
                                    silenceFrameCount++
                                    
                                    // 連續靜音幀數達到閾值，認為語音結束
                                    if (silenceFrameCount >= 10) { // 約 0.32 秒的靜音
                                        speechDetected = false
                                        
                                        if (speechBuffer.isNotEmpty()) {
                                            val speechArray = speechBuffer.toFloatArray()
                                            audioProcessingCallback?.onSpeechEnd(speechArray)
                                            Log.d(TAG, "語音結束，長度: ${speechArray.size} 樣本")
                                        }
                                        
                                        speechBuffer.clear()
                                        silenceFrameCount = 0
                                    }
                                }
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "錄製循環異常", e)
                audioProcessingCallback?.onError("錄製錯誤: ${e.message}")
                break
            }
        }
        
        Log.d(TAG, "錄製循環結束")
    }
    
    /**
     * 將字節數組轉換為 float 數組
     */
    private fun convertBytesToFloat(bytes: ByteArray, length: Int): FloatArray {
        val shorts = ShortArray(length / 2)
        ByteBuffer.wrap(bytes, 0, length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(shorts)
        
        return FloatArray(shorts.size) { shorts[it] / 32768.0f }
    }
    
    /**
     * 計算音頻級別 (RMS)
     */
    private fun calculateAudioLevel(audioData: FloatArray): Float {
        var sum = 0.0
        for (sample in audioData) {
            sum += (sample * sample).toDouble()
        }
        return kotlin.math.sqrt(sum / audioData.size).toFloat()
    }
    
    /**
     * 保存音頻到文件（用於調試）
     */
    fun saveAudioToFile(audioData: FloatArray, filename: String): File? {
        return try {
            val file = File(context.cacheDir, filename)
            val fos = FileOutputStream(file)
            
            // 轉換 float 到 16-bit PCM
            val shorts = ShortArray(audioData.size)
            for (i in audioData.indices) {
                shorts[i] = (audioData[i] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
            }
            
            val byteBuffer = ByteBuffer.allocate(shorts.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (short in shorts) {
                byteBuffer.putShort(short)
            }
            
            fos.write(byteBuffer.array())
            fos.close()
            
            Log.i(TAG, "音頻已保存到: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "保存音頻文件失敗", e)
            null
        }
    }
    
    /**
     * 清理資源
     */
    fun cleanup() {
        stopRecording()
        
        try {
            audioRecord?.release()
            audioRecord = null
            Log.i(TAG, "AudioRecord 資源已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理 AudioRecord 資源異常", e)
        }
    }
}
