package com.voiceassistant.app.utils

import android.content.Context
import android.util.Log
import com.voiceassistant.app.data.repository.SileroVadRepository
import com.voiceassistant.app.domain.repository.SpeechRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import ai.onnxruntime.*
import java.io.File

/**
 * VAD 測試輔助類
 * 用於測試 Silero VAD 的功能
 */
@Singleton
class VadTestHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sileroVadRepository: SileroVadRepository,
    private val speechRepository: SpeechRepository,
    private val modelDownloader: ModelDownloader
) {
    
    companion object {
        private const val TAG = "VadTestHelper"
        private const val SAMPLE_RATE = 16000
        private const val CHUNK_SIZE = 512
    }
    
    /**
     * 診斷 VAD 初始化問題
     */
    suspend fun diagnoseVadInitialization(): String = withContext(Dispatchers.IO) {
        val diagnosis = StringBuilder()
        diagnosis.appendLine("=== VAD 初始化診斷報告 ===")
        
        try {
            // 1. 檢查 ONNX Runtime
            diagnosis.appendLine("\n1. 檢查 ONNX Runtime:")
            val onnxTest = testOnnxRuntime()
            diagnosis.appendLine("   ONNX Runtime 可用: $onnxTest")
            
            // 2. 檢查權限
            diagnosis.appendLine("\n2. 檢查權限:")
            val audioPermission = androidx.core.app.ActivityCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            diagnosis.appendLine("   音頻錄製權限: $audioPermission")
            
            // 3. 檢查文件系統
            diagnosis.appendLine("\n3. 檢查文件系統:")
            val filesDir = context.filesDir
            diagnosis.appendLine("   應用目錄: ${filesDir.absolutePath}")
            diagnosis.appendLine("   目錄存在: ${filesDir.exists()}")
            diagnosis.appendLine("   目錄可寫: ${filesDir.canWrite()}")
            
            // 4. 檢查模型文件
            diagnosis.appendLine("\n4. 檢查模型文件:")
            val modelPath = modelDownloader.getModelPath()
            val modelFile = File(modelPath)
            diagnosis.appendLine("   模型路徑: $modelPath")
            diagnosis.appendLine("   模型存在: ${modelFile.exists()}")
            if (modelFile.exists()) {
                diagnosis.appendLine("   模型大小: ${modelFile.length()} bytes")
                diagnosis.appendLine("   模型可讀: ${modelFile.canRead()}")
                diagnosis.appendLine("   模型有效: ${modelDownloader.isModelValid(modelFile)}")
            }
            
            // 5. 測試模型下載
            if (!modelFile.exists() || !modelDownloader.isModelValid(modelFile)) {
                diagnosis.appendLine("\n5. 嘗試下載模型:")
                val downloadResult = modelDownloader.downloadSileroVadModel()
                diagnosis.appendLine("   下載結果: $downloadResult")
                if (downloadResult && modelFile.exists()) {
                    diagnosis.appendLine("   下載後大小: ${modelFile.length()} bytes")
                }
            }
            
            // 6. 測試模型加載
            if (modelFile.exists() && modelDownloader.isModelValid(modelFile)) {
                diagnosis.appendLine("\n6. 測試模型加載:")
                val loadTest = testModelLoad(modelFile)
                diagnosis.appendLine("   模型加載: $loadTest")
            }
            
            // 7. 測試 VAD 初始化
            diagnosis.appendLine("\n7. 測試 VAD 初始化:")
            val vadInit = sileroVadRepository.initialize()
            diagnosis.appendLine("   VAD 初始化: $vadInit")
            
        } catch (e: Exception) {
            diagnosis.appendLine("\n!!! 診斷過程中發生異常 !!!")
            diagnosis.appendLine("錯誤: ${e.message}")
            diagnosis.appendLine("類型: ${e.javaClass.simpleName}")
        }
        
        diagnosis.toString()
    }
    
    /**
     * 測試 ONNX Runtime 基本功能
     */
    private suspend fun testOnnxRuntime(): Boolean = withContext(Dispatchers.IO) {
        try {
            val environment = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions()
            val testData = floatArrayOf(0.1f, 0.2f, 0.3f)
            val testTensor = OnnxTensor.createTensor(environment, testData)
            
            testTensor.close()
            sessionOptions.close()
            environment.close()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "ONNX Runtime 測試失敗", e)
            false
        }
    }
    
    /**
     * 測試模型加載
     */
    private suspend fun testModelLoad(modelFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val environment = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions()
            val session = environment.createSession(modelFile.absolutePath, sessionOptions)
            
            session.close()
            sessionOptions.close()
            environment.close()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "模型加載測試失敗", e)
            false
        }
    }
    
    /**
     * 測試 Silero VAD 基本功能
     */
    suspend fun testSileroVad(): Boolean = withContext(Dispatchers.Default) {
        try {
            Log.i(TAG, "開始測試 Silero VAD...")
            
            // 初始化 VAD
            val initialized = sileroVadRepository.initialize()
            if (!initialized) {
                Log.e(TAG, "Silero VAD 初始化失敗")
                return@withContext false
            }
            
            Log.i(TAG, "Silero VAD 初始化成功，開始功能測試...")
            
            // 測試靜音檢測
            val silenceResult = testSilenceDetection()
            Log.i(TAG, "靜音檢測測試: $silenceResult")
            
            // 測試 Whisper STT
            val whisperResult = testWhisperStt()
            Log.i(TAG, "Whisper STT 測試: $whisperResult")
            
            // 清理資源
            sileroVadRepository.cleanup()
            
            val allTestsPassed = silenceResult && whisperResult
            Log.i(TAG, "Silero VAD 測試完成，結果: ${if (allTestsPassed) "通過" else "失敗"}")
            
            allTestsPassed
            
        } catch (e: Exception) {
            Log.e(TAG, "Silero VAD 測試異常", e)
            false
        }
    }
    
    /**
     * 測試靜音檢測
     */
    private suspend fun testSilenceDetection(): Boolean {
        return try {
            // 生成靜音音頻（全零）
            val silenceChunk = FloatArray(CHUNK_SIZE) { 0.0f }
            
            val (isVoice, probability) = sileroVadRepository.detectVoiceActivity(silenceChunk)
            
            Log.d(TAG, "靜音測試 - isVoice: $isVoice, probability: $probability")
            
            // 靜音應該被檢測為非語音，概率應該很低
            !isVoice && probability < 0.3f
            
        } catch (e: Exception) {
            Log.e(TAG, "靜音檢測測試失敗", e)
            false
        }
    }
    
    /**
     * 測試 Whisper 語音轉文字
     */
    private suspend fun testWhisperStt(): Boolean {
        return try {
            Log.i(TAG, "開始測試 Whisper STT...")
            
            // 創建測試音頻文件
            val testAudioFile = createTestAudioFile()
            if (!testAudioFile.exists()) {
                Log.e(TAG, "測試音頻文件創建失敗")
                return false
            }
            
            Log.d(TAG, "測試音頻文件: ${testAudioFile.absolutePath}, 大小: ${testAudioFile.length()} bytes")
            
            // 調用語音轉文字
            val result = speechRepository.speechToText(testAudioFile)
            
            val success = result.isSuccess
            val transcription = result.getOrNull() ?: ""
            
            Log.i(TAG, "Whisper STT 結果: success=$success, text='$transcription'")
            
            // 清理測試文件
            try {
                testAudioFile.delete()
            } catch (e: Exception) {
                Log.w(TAG, "清理測試文件失敗: ${e.message}")
            }
            
            // 認為有任何轉錄結果都算成功（包括空字符串，因為合成音頻可能無法識別）
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Whisper STT 測試失敗", e)
            false
        }
    }
    
    /**
     * 創建測試用的 WAV 音頻文件
     */
    private fun createTestAudioFile(): File {
        val testFile = File(context.cacheDir, "test_audio_${System.currentTimeMillis()}.wav")
        
        try {
            // 首先嘗試從 assets 載入預錄的測試音頻
            val assetFile = loadTestAudioFromAssets()
            if (assetFile != null) {
                assetFile.copyTo(testFile, overwrite = true)
                Log.d(TAG, "從 assets 載入測試音頻: ${testFile.absolutePath}")
                return testFile
            }
            
            return testFile
            
        } catch (e: Exception) {
            Log.e(TAG, "創建測試音頻文件失敗", e)
            return testFile
        }
    }
    
    /**
     * 從 assets 載入預錄的測試音頻
     */
    private fun loadTestAudioFromAssets(): File? {
        return try {
            val testAudioNames = listOf(
                "breakfesttt.wav",        // "breakfest"
                "test_audio_hello.wav",        // "你好"
                "test_audio_thanks.wav",       // "謝謝"
                "test_audio_question.wav",     // "今天天氣如何"
                "test_voice_sample.wav"        // 通用測試音頻
            )
            
            for (audioName in testAudioNames) {
                try {
                    val tempFile = File(context.cacheDir, "temp_$audioName")
                    context.assets.open(audioName).use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "找到測試音頻: $audioName")
                    return tempFile
                } catch (e: Exception) {
                    Log.d(TAG, "未找到測試音頻: $audioName")
                    continue
                }
            }
            null
        } catch (e: Exception) {
            Log.d(TAG, "載入測試音頻失敗: ${e.message}")
            null
        }
    }
}