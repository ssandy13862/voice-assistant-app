package com.voiceassistant.app.data.repository

import android.content.Context
import android.util.Log
import com.voiceassistant.app.utils.ModelDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import ai.onnxruntime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Silero VAD 實作
 * 使用 ONNX Runtime 執行 Silero VAD 模型
 */
@Singleton
class SileroVadRepository @Inject constructor(
    private val modelDownloader: ModelDownloader
) {
    
    companion object {
        private const val TAG = "SileroVAD"
        private const val MODEL_NAME = "silero_vad.onnx"
        private const val SAMPLE_RATE = 16000
        private const val CHUNK_SIZE = 512 // Silero VAD 期望的批次大小
    }
    
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false
    
    // Silero VAD 狀態 - 嘗試不同的格式
    private var state: FloatArray? = null // 嘗試平坦的 128 維陣列
    private var sr: LongArray = longArrayOf(SAMPLE_RATE.toLong())
    
    /**
     * 初始化 Silero VAD
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "正在初始化 Silero VAD...")
            
            // 步驟 1: 初始化 ONNX Runtime 環境
            Log.d(TAG, "步驟 1: 初始化 ONNX Runtime 環境")
            ortEnvironment = OrtEnvironment.getEnvironment()
            Log.d(TAG, "ONNX Runtime 環境初始化成功")
            
            // 步驟 2: 檢查並下載模型檔案
            Log.d(TAG, "步驟 2: 檢查模型檔案")
            val modelPath = modelDownloader.getModelPath()
            Log.d(TAG, "模型路徑: $modelPath")
            val modelFile = File(modelPath)
            
            if (!modelFile.exists()) {
                Log.i(TAG, "模型檔案不存在，開始下載...")
                val downloaded = modelDownloader.downloadSileroVadModel()
                if (!downloaded) {
                    Log.e(TAG, "Silero VAD 模型下載失敗")
                    return@withContext false
                }
                Log.d(TAG, "模型下載完成")
            } else {
                Log.d(TAG, "模型檔案已存在，檢查有效性")
                if (!modelDownloader.isModelValid(modelFile)) {
                    Log.w(TAG, "模型檔案無效，重新下載")
                    val downloaded = modelDownloader.downloadSileroVadModel()
                    if (!downloaded) {
                        Log.e(TAG, "Silero VAD 模型重新下載失敗")
                        return@withContext false
                    }
                }
            }
            
            // 步驟 3: 建立會話選項
            Log.d(TAG, "步驟 3: 建立 ONNX 會話選項")
            val sessionOptions = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                setSessionLogLevel(OrtLoggingLevel.ORT_LOGGING_LEVEL_WARNING)
            }
            Log.d(TAG, "會話選項建立成功")
            
            // 步驟 4: 建立推理會話
            Log.d(TAG, "步驟 4: 建立 ONNX 推理會話，模型路徑: ${modelFile.absolutePath}")
            Log.d(TAG, "模型檔案大小: ${modelFile.length()} bytes")
            
            if (!modelFile.canRead()) {
                Log.e(TAG, "無法讀取模型檔案")
                return@withContext false
            }
            
            ortSession = ortEnvironment?.createSession(
                modelFile.absolutePath,
                sessionOptions
            )
            Log.d(TAG, "ONNX 推理會話建立成功")
            
            // 步驟 4.5: 檢查模型的輸入輸出信息
            logModelInputOutputInfo()
            
            // 步驟 5: 初始化 LSTM 狀態
            Log.d(TAG, "步驟 5: 初始化 LSTM 狀態")
            initializeLSTMStates()
            Log.d(TAG, "LSTM 狀態初始化完成")
            
            isInitialized = true
            Log.i(TAG, "Silero VAD 初始化成功")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Silero VAD 初始化失敗", e)
            Log.e(TAG, "錯誤詳情: ${e.message}")
            Log.e(TAG, "錯誤類型: ${e.javaClass.simpleName}")
            if (e.cause != null) {
                Log.e(TAG, "根本原因: ${e.cause?.message}")
            }
            false
        }
    }
    
    /**
     * 檢測語音活動
     */
    suspend fun detectVoiceActivity(audioChunk: FloatArray): Pair<Boolean, Float> = withContext(Dispatchers.Default) {
        if (!isInitialized || ortSession == null || ortEnvironment == null) {
            Log.w(TAG, "VAD 未初始化")
            return@withContext Pair(false, 0.0f)
        }
        
        try {
            // 確保音頻塊大小正確
            val processedChunk = if (audioChunk.size != CHUNK_SIZE) {
                resizeAudioChunk(audioChunk, CHUNK_SIZE)
            } else {
                audioChunk
            }
            
            // 準備輸入張量 (Silero VAD 正確格式)
            val inputTensor = createInputTensor(processedChunk)
            val stateTensor = createStateTensor(state!!)
//            val srTensor = OnnxTensor.createTensor(ortEnvironment!!, sr)
            val srTensor = OnnxTensor.createTensor(
                ortEnvironment!!,
                LongBuffer.wrap(longArrayOf(SAMPLE_RATE.toLong())),
                longArrayOf() // 0-D scalar
            )

            // 記錄張量維度信息
            Log.d(TAG, "輸入張量維度:")
            Log.d(TAG, "  input: ${inputTensor.info.shape.contentToString()}")
            Log.d(TAG, "  state: ${stateTensor.info.shape.contentToString()}")
            Log.d(TAG, "  sr: ${srTensor.info.shape.contentToString()}")
            
            // 建立輸入映射 (3個輸入：input, state, sr)
            val inputs = mapOf(
                "input" to inputTensor,
                "state" to stateTensor,
                "sr" to srTensor
            )
            
            // 執行推理
            val results = ortSession!!.run(inputs)
            
            // 獲取輸出
            val outputTensor = results.get("output").get() as OnnxTensor? ?: throw IllegalStateException("無法獲取輸出張量")
            val newStateTensor = results.get("stateN").get() as OnnxTensor? ?: throw IllegalStateException("無法獲取狀態張量")
            
            // 提取語音概率
            val outputData = outputTensor.floatBuffer.array()
//            val outputData = ((outputTensor.value as Array<FloatArray>)[0][0])
            val voiceProbability = outputData[0]
            
            // 更新 LSTM 狀態
            updateLSTMStates(newStateTensor)
            
            // 關閉張量
            inputTensor.close()
            stateTensor.close()
            srTensor.close()
            results.close()
            
            // 判斷是否為語音（閾值可調整）
            val isVoice = voiceProbability > 0.5f
            
            Log.d(TAG, "語音概率: $voiceProbability, 是否為語音: $isVoice")
            
            Pair(isVoice, voiceProbability)
            
        } catch (e: Exception) {
            Log.e(TAG, "VAD 檢測異常", e)
            Log.e(TAG, "VAD 錯誤詳情: ${e.message}")
            Log.e(TAG, "VAD 錯誤類型: ${e.javaClass.simpleName}")
            if (e.cause != null) {
                Log.e(TAG, "VAD 根本原因: ${e.cause?.message}")
            }
            // 返回安全預設值
            Pair(false, 0.0f)
        }
    }
    
    /**
     * 批量處理音頻
     */
    suspend fun processAudioBatch(audioData: FloatArray): List<Pair<Boolean, Float>> = withContext(Dispatchers.Default) {
        val results = mutableListOf<Pair<Boolean, Float>>()
        
        // 將音訊分割成固定大小的區塊
        for (i in audioData.indices step CHUNK_SIZE) {
            val chunk = audioData.sliceArray(i until minOf(i + CHUNK_SIZE, audioData.size))
            val result = detectVoiceActivity(chunk)
            results.add(result)
        }
        
        results
    }
    
    /**
     * 重置 VAD 狀態
     */
    fun resetStates() {
        initializeLSTMStates()
        Log.d(TAG, "VAD 狀態已重置")
    }
    
    /**
     * 釋放資源
     */
    fun cleanup() {
        try {
            ortSession?.close()
            ortEnvironment?.close()
            isInitialized = false
            Log.i(TAG, "Silero VAD 資源已釋放")
        } catch (e: Exception) {
            Log.e(TAG, "釋放 VAD 資源失敗", e)
        }
    }
    
    /**
     * 檢查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
    
    // 私有輔助方法
    
    /**
     * 記錄模型的輸入輸出信息
     */
    private fun logModelInputOutputInfo() {
        try {
            ortSession?.let { session ->
                Log.d(TAG, "=== 模型輸入信息 ===")
                val inputCount = session.inputNames?.size ?: 0
                Log.d(TAG, "輸入數量: $inputCount")
                
                session.inputNames?.forEachIndexed { index, name ->
                    Log.d(TAG, "輸入 $index: $name")
                    try {
                        val inputInfo = session.inputInfo?.get(name)
                        Log.d(TAG, "  信息: $inputInfo")
                    } catch (e: Exception) {
                        Log.w(TAG, "無法獲取輸入 $name 的詳細信息: ${e.message}")
                    }
                }
                
                Log.d(TAG, "=== 模型輸出信息 ===")
                val outputCount = session.outputNames?.size ?: 0
                Log.d(TAG, "輸出數量: $outputCount")
                
                session.outputNames?.forEachIndexed { index, name ->
                    Log.d(TAG, "輸出 $index: $name")
                    try {
                        val outputInfo = session.outputInfo?.get(name)
                        Log.d(TAG, "  信息: $outputInfo")
                    } catch (e: Exception) {
                        Log.w(TAG, "無法獲取輸出 $name 的詳細信息: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "記錄模型信息失敗", e)
        }
    }
    
    private fun initializeLSTMStates() {
        // 模型期望 [2, 1, 128] = 256 個元素
        state = FloatArray(256) { 0.0f }
        Log.d(TAG, "初始化 LSTM 狀態，大小: ${state?.size}")
    }
    
    private fun createInputTensor(audioChunk: FloatArray): OnnxTensor {
        // 模型期望 2 維輸入: [batch_size, sequence_length] = [1, chunk_size]
        val reshapedData = Array(1) { audioChunk }
        Log.d(TAG, "建立 2D 輸入張量: [1, ${audioChunk.size}]")
        return OnnxTensor.createTensor(ortEnvironment!!, reshapedData)
    }
    
    private fun createStateTensor(state: FloatArray): OnnxTensor {
        // 模型期望 3 維狀態張量: [2, 1, 128]
        if (state.size == 256) {
            // 將 256 個元素重塑為 [2, 1, 128]
            val reshapedState = Array(2) { layerIndex ->
                Array(1) {
                    FloatArray(128) { elementIndex ->
                        state[layerIndex * 128 + elementIndex]
                    }
                }
            }
            Log.d(TAG, "建立 3D 狀態張量: [2, 1, 128]")
            return OnnxTensor.createTensor(ortEnvironment!!, reshapedState)
        } else {
            // 如果不是 256 個元素，嘗試直接使用
            Log.d(TAG, "建立 1D 狀態張量: [${state.size}]")
            return OnnxTensor.createTensor(ortEnvironment!!, state)
        }
    }
    
    private fun updateLSTMStates(newStateTensor: OnnxTensor) {
        try {
            val newStateData = newStateTensor.floatBuffer
            val stateSize = state?.size ?: 0
            
            Log.d(TAG, "更新 LSTM 狀態，當前狀態大小: $stateSize")
            Log.d(TAG, "返回的狀態張量形狀: ${newStateTensor.info.shape.contentToString()}")
            
            newStateData.rewind()
            val actualDataSize = newStateData.remaining()
            Log.d(TAG, "新狀態資料大小: $actualDataSize")
            
            // 更新狀態，但不超過現有陣列大小
            val updateSize = minOf(stateSize, actualDataSize)
            for (i in 0 until updateSize) {
                if (newStateData.hasRemaining()) {
                    state!![i] = newStateData.get()
                }
            }
            
            Log.d(TAG, "LSTM 狀態已更新，更新了 $updateSize 個元素")
        } catch (e: Exception) {
            Log.e(TAG, "更新 LSTM 狀態失敗", e)
            Log.e(TAG, "更新狀態錯誤詳情: ${e.message}")
        }
    }
    
    private fun resizeAudioChunk(audioChunk: FloatArray, targetSize: Int): FloatArray {
        return when {
            audioChunk.size == targetSize -> audioChunk
            audioChunk.size > targetSize -> audioChunk.sliceArray(0 until targetSize)
            else -> {
                // 用零填充
                val paddedChunk = FloatArray(targetSize)
                audioChunk.copyInto(paddedChunk, 0, 0, audioChunk.size)
                paddedChunk
            }
        }
    }
}