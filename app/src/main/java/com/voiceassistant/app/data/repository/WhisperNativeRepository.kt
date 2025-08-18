package com.voiceassistant.app.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地 Whisper 和 VAD 的 JNI 接口
 */
@Singleton
class WhisperNativeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "WhisperNative"
        
        init {
            try {
                System.loadLibrary("voiceassistant")
                Log.i(TAG, "本地庫加載成功")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "本地庫加載失敗", e)
            }
        }
    }
    
    private var whisperHandle: Long = 0
    private var vadHandle: Long = 0
    private var isInitialized = false
    
    /**
     * 初始化 Whisper 模型
     */
    fun initializeWhisper(modelPath: String): Boolean {
        return try {
            whisperHandle = initWhisper(modelPath)
            val success = whisperHandle != 0L
            if (success) {
                Log.i(TAG, "Whisper 初始化成功")
            } else {
                Log.e(TAG, "Whisper 初始化失敗")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Whisper 初始化異常", e)
            false
        }
    }
    
    /**
     * 初始化 VAD
     */
    fun initializeVAD(sampleRate: Int = 16000, frameLength: Int = 512): Boolean {
        return try {
            vadHandle = initVAD(sampleRate, frameLength)
            val success = vadHandle != 0L
            if (success) {
                isInitialized = true
                Log.i(TAG, "VAD 初始化成功")
            } else {
                Log.e(TAG, "VAD 初始化失敗")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "VAD 初始化異常", e)
            false
        }
    }
    
    /**
     * 音頻轉文字
     */
    fun transcribeAudio(audioData: FloatArray): String {
        return if (whisperHandle != 0L) {
            try {
                val result = nativeTranscribeAudio(audioData)
                Log.d(TAG, "轉錄結果: $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "音頻轉錄異常", e)
                ""
            }
        } else {
            Log.w(TAG, "Whisper 未初始化")
            ""
        }
    }
    
    /**
     * 檢測語音活動
     */
    fun detectVoiceActivity(audioFrame: FloatArray): Boolean {
        return if (vadHandle != 0L) {
            try {
                nativeDetectVoiceActivity(audioFrame)
            } catch (e: Exception) {
                Log.e(TAG, "VAD 檢測異常", e)
                false
            }
        } else {
            Log.w(TAG, "VAD 未初始化")
            false
        }
    }
    
    /**
     * 獲取語音活動概率
     */
    fun getVoiceActivityProbability(audioFrame: FloatArray): Float {
        return if (vadHandle != 0L) {
            try {
                nativeGetVoiceProbability(audioFrame)
            } catch (e: Exception) {
                Log.e(TAG, "VAD 概率計算異常", e)
                0.0f
            }
        } else {
            Log.w(TAG, "VAD 未初始化")
            0.0f
        }
    }
    
    /**
     * 檢查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * 清理資源
     */
    fun cleanup() {
        try {
            nativeCleanup()
            whisperHandle = 0
            vadHandle = 0
            isInitialized = false
            Log.i(TAG, "本地資源清理完成")
        } catch (e: Exception) {
            Log.e(TAG, "清理資源異常", e)
        }
    }
    
    /**
     * 獲取模型文件路徑
     */
    fun getModelPath(): String {
        val modelDir = File(context.filesDir, "whisper_models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        return File(modelDir, "ggml-small.bin").absolutePath
    }
    
    /**
     * 檢查模型文件是否存在
     */
    fun isModelAvailable(): Boolean {
        val modelPath = getModelPath()
        val modelFile = File(modelPath)
        return modelFile.exists() && modelFile.length() > 100_000_000L // 至少 100MB
    }
    
    /**
     * 從 assets 或外部存儲安裝模型
     */
    fun installModel(): Boolean {
        return try {
            val modelPath = getModelPath()
            val modelFile = File(modelPath)
            
            if (modelFile.exists()) {
                Log.i(TAG, "Whisper 模型已存在: ${modelFile.absolutePath}")
                return true
            }
            
            // 嘗試從 assets 複製
            Log.d(TAG, "嘗試從 assets 安裝 Whisper 模型...")
            val assetsInstalled = installFromAssets()
            if (assetsInstalled) {
                Log.i(TAG, "從 assets 安裝模型成功")
                return true
            }
            
            // 嘗試從外部存儲複製
            Log.d(TAG, "嘗試從外部存儲安裝 Whisper 模型...")
            val externalInstalled = installFromExternalStorage()
            if (externalInstalled) {
                Log.i(TAG, "從外部存儲安裝模型成功")
                return true
            }
            
            Log.w(TAG, "未找到 Whisper 模型文件，請參考 download_whisper_model.md")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "安裝 Whisper 模型失敗", e)
            false
        }
    }
    
    private fun installFromAssets(): Boolean {
        return try {
            val modelPath = getModelPath()
            context.assets.open("ggml-small.bin").use { input ->
                File(modelPath).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "從 assets 複製模型完成")
            true
        } catch (e: Exception) {
            Log.d(TAG, "assets 中未找到模型: ${e.message}")
            false
        }
    }
    
    private fun installFromExternalStorage(): Boolean {
        return try {
            val externalPaths = listOf(
                "/sdcard/ggml-small.bin",
                "/sdcard/Download/ggml-small.bin",
                "/sdcard/Android/data/${context.packageName}/files/ggml-small.bin"
            )
            
            for (externalPath in externalPaths) {
                val externalFile = File(externalPath)
                if (externalFile.exists() && externalFile.length() > 100_000_000L) {
                    Log.d(TAG, "找到外部模型: $externalPath")
                    val modelPath = getModelPath()
                    externalFile.copyTo(File(modelPath), overwrite = true)
                    Log.i(TAG, "從 $externalPath 複製模型完成")
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.d(TAG, "從外部存儲複製失敗: ${e.message}")
            false
        }
    }
    
    // JNI 方法聲明
    private external fun initWhisper(modelPath: String): Long
    private external fun initVAD(sampleRate: Int, frameLength: Int): Long
    private external fun nativeTranscribeAudio(audioData: FloatArray): String
    private external fun nativeDetectVoiceActivity(audioFrame: FloatArray): Boolean
    private external fun nativeGetVoiceProbability(audioFrame: FloatArray): Float
    private external fun nativeCleanup()
}
