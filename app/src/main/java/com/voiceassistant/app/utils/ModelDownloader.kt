package com.voiceassistant.app.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 模型下載輔助類
 * 用於下載和管理 Silero VAD 模型
 */
@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "ModelDownloader"
        private const val MODEL_NAME = "silero_vad.onnx"
        
        // Silero VAD 模型最小大小檢查（防止下載到錯誤檔案或不完整檔案）
        private const val MIN_MODEL_SIZE = 800000L   // 最小 0.8MB（允許壓縮或舊版本）
        
        // 備用 URL 列表
        private val SILERO_VAD_URLS = listOf(
            "https://github.com/snakers4/silero-vad/raw/master/src/silero_vad/data/silero_vad.onnx",
            "https://raw.githubusercontent.com/snakers4/silero-vad/master/src/silero_vad/data/silero_vad.onnx",
            "https://huggingface.co/silero/silero-vad/resolve/main/silero_vad.onnx"
        )
    }
    
    /**
     * 下載 Silero VAD 模型
     */
    suspend fun downloadSileroVadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, MODEL_NAME)
            Log.d(TAG, "模型目標路徑: ${modelFile.absolutePath}")
            
            // 檢查模型是否已存在且有效
            if (isModelValid(modelFile)) {
                Log.i(TAG, "Silero VAD 模型已存在且有效，大小: ${modelFile.length()} bytes")
                return@withContext true
            }
            
            Log.i(TAG, "開始下載 Silero VAD 模型...")
            
            // 確保目錄存在
            if (!context.filesDir.exists()) {
                context.filesDir.mkdirs()
                Log.d(TAG, "建立目錄: ${context.filesDir.absolutePath}")
            }
            
            // 嘗試多個 URL 下載
            var downloadSuccessful = false
            var lastException: Exception? = null
            
            for ((index, urlString) in SILERO_VAD_URLS.withIndex()) {
                try {
                    Log.d(TAG, "嘗試 URL ${index + 1}/${SILERO_VAD_URLS.size}: $urlString")
                    
                    val url = URL(urlString)
                    val connection = url.openConnection().apply {
                        connectTimeout = 30000 // 30秒連線超時
                        readTimeout = 120000   // 120秒讀取超時（模型檔案較大）
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                        setRequestProperty("Accept", "*/*")
                    }
                    
                    Log.d(TAG, "正在連線到伺服器...")
                    connection.connect()
                    
                    val responseCode = if (connection is java.net.HttpURLConnection) {
                        connection.responseCode
                    } else 200
                    
                    Log.d(TAG, "伺服器回應碼: $responseCode")
                    
                    if (responseCode == 200 || responseCode == 302) {
                        connection.getInputStream().use { inputStream ->
                            FileOutputStream(modelFile).use { outputStream ->
                                val buffer = ByteArray(8192)
                                var totalBytes = 0L
                                var bytesRead: Int
                                
                                Log.d(TAG, "開始下載資料...")
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    totalBytes += bytesRead
                                    
                                    // 每512KB記錄一次進度
                                    if (totalBytes % 524288 == 0L) {
                                        Log.d(TAG, "已下載: ${totalBytes / 1024}KB")
                                    }
                                }
                                
                                outputStream.flush()
                                Log.i(TAG, "模型下載完成，總大小: ${totalBytes}bytes")
                                downloadSuccessful = true
                            }
                        }
                        
                        if (downloadSuccessful) break
                    } else {
                        Log.w(TAG, "URL $urlString 回應碼不正確: $responseCode")
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "URL $urlString 下載失敗: ${e.message}")
                    lastException = e
                    
                    // 如果檔案部分下載，刪除它
                    if (modelFile.exists()) {
                        modelFile.delete()
                        Log.d(TAG, "刪除部分下載的檔案")
                    }
                }
            }
            
            if (!downloadSuccessful) {
                throw lastException ?: Exception("所有下載 URL 都失敗了")
            }
            
            // 驗證下載的模型
            Log.d(TAG, "驗證下載的模型...")
            val isValid = isModelValid(modelFile)
            Log.d(TAG, "模型驗證結果: $isValid")
            
            if (isValid) {
                Log.i(TAG, "Silero VAD 模型下載並驗證成功")
            } else {
                Log.e(TAG, "下載的模型驗證失敗，刪除檔案")
                if (modelFile.exists()) {
                    modelFile.delete()
                }
            }
            
            isValid
            
        } catch (e: Exception) {
            Log.e(TAG, "下載 Silero VAD 模型失敗", e)
            Log.e(TAG, "錯誤詳情: ${e.message}")
            Log.e(TAG, "錯誤類型: ${e.javaClass.simpleName}")
            false
        }
    }
    
    /**
     * 檢查模型是否有效
     */
    fun isModelValid(modelFile: File): Boolean {
        return try {
            Log.d(TAG, "檢查模型檔案: ${modelFile.absolutePath}")
            Log.d(TAG, "檔案存在: ${modelFile.exists()}")
            
            if (!modelFile.exists()) {
                Log.d(TAG, "模型檔案不存在")
                return false
            }
            
            val fileSize = modelFile.length()
            Log.d(TAG, "模型檔案大小: $fileSize bytes (${fileSize / 1024}KB)")
            Log.d(TAG, "最小要求大小: ${MIN_MODEL_SIZE / 1024}KB")
            
            val isValidSize = fileSize > 0 && fileSize >= MIN_MODEL_SIZE
            
            Log.d(TAG, "模型大小有效: $isValidSize")
            
            val canRead = modelFile.canRead()
            Log.d(TAG, "可讀取: $canRead")
            
            val isValid = isValidSize && canRead
            Log.d(TAG, "模型有效性: $isValid")
            
            isValid
            
        } catch (e: Exception) {
            Log.e(TAG, "檢查模型檔案失敗", e)
            false
        }
    }
    
    /**
     * 取得模型檔案路徑
     */
    fun getModelPath(): String {
        return File(context.filesDir, MODEL_NAME).absolutePath
    }
    
    /**
     * 刪除模型檔案
     */
    fun deleteModel(): Boolean {
        return try {
            val modelFile = File(context.filesDir, MODEL_NAME)
            if (modelFile.exists()) {
                val deleted = modelFile.delete()
                Log.i(TAG, if (deleted) "模型檔案已刪除" else "模型檔案刪除失敗")
                deleted
            } else {
                Log.i(TAG, "模型檔案不存在")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "刪除模型檔案失敗", e)
            false
        }
    }
    
    /**
     * 取得模型檔案大小
     */
    fun getModelSize(): Long {
        return try {
            val modelFile = File(context.filesDir, MODEL_NAME)
            if (modelFile.exists()) modelFile.length() else 0L
        } catch (e: Exception) {
            Log.e(TAG, "取得模型大小失敗", e)
            0L
        }
    }
}