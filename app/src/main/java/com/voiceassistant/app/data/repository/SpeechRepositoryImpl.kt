package com.voiceassistant.app.data.repository

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.voiceassistant.app.data.api.ChatCompletionRequest
import com.voiceassistant.app.data.api.ChatMessage
import com.voiceassistant.app.data.api.OpenAiApi
import com.voiceassistant.app.data.api.WhisperApi
import com.voiceassistant.app.domain.repository.SpeechRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 語音處理倉庫實作
 */
@Singleton
class SpeechRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openAiApi: OpenAiApi,
    private val whisperApi: WhisperApi,
    private val whisperNativeRepository: WhisperNativeRepository
) : SpeechRepository {

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isSpeaking = false
    private var useLocalWhisper = false // 預設使用遠端 Whisper，需要檢查本地模型後再決定
    private var isWhisperInitialized = false
    
    // OpenAI API配置
    private val openAiApiKey = com.voiceassistant.app.BuildConfig.OPENAI_API_KEY
    private val isApiKeyValid = openAiApiKey != "your_openai_api_key_here" && openAiApiKey.isNotBlank()
    private val systemPrompt = """
        你是一個智慧語音助理，需要：
        1. 用自然、友好的語調回應
        2. 回答要簡潔明瞭，適合語音交流
        3. 可以進行日常對話、回答問題、提供幫助
        4. 保持對話的連貫性和上下文
        5. 用中文回答，除非使用者明確要求使用其他語言
    """.trimIndent()

    init {
        initializeTTS()
        initializeWhisper()
    }
    
    /**
     * 初始化 Whisper（檢查本地模型是否可用）
     */
    private fun initializeWhisper() {
        try {
            // 首先嘗試安裝模型
            android.util.Log.d("SpeechRepository", "檢查並安裝 Whisper 模型...")
            val modelInstalled = whisperNativeRepository.installModel()
            
            if (modelInstalled && whisperNativeRepository.isModelAvailable()) {
                android.util.Log.i("SpeechRepository", "本地 Whisper 模型可用，嘗試初始化...")
                val modelPath = whisperNativeRepository.getModelPath()
                isWhisperInitialized = whisperNativeRepository.initializeWhisper(modelPath)
                
                if (isWhisperInitialized) {
                    useLocalWhisper = true
                    android.util.Log.i("SpeechRepository", "本地 Whisper 初始化成功，使用本地識別")
                } else {
                    android.util.Log.w("SpeechRepository", "本地 Whisper 初始化失敗，使用遠端 API")
                }
            } else {
                android.util.Log.i("SpeechRepository", "本地 Whisper 模型不可用，使用遠端 API")
                android.util.Log.i("SpeechRepository", "提示：請參考 assets/download_whisper_model.md 手動安裝模型")
            }
        } catch (e: Exception) {
            android.util.Log.e("SpeechRepository", "Whisper 初始化異常", e)
            useLocalWhisper = false
        }
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.CHINESE
                isTtsInitialized = true
            }
        }
    }

    override suspend fun speechToText(audioFile: File): Result<String> {

        // 初始化 Whisper
        Log.d("SpeechRepository", "初始化 Whisper...")
        initializeWhisper()

        android.util.Log.i("SpeechRepository", "開始語音識別，使用 ${if (useLocalWhisper) "本地 Whisper" else "遠端 API"}")
        android.util.Log.d("SpeechRepository", "音頻文件: ${audioFile.absolutePath}, 大小: ${audioFile.length()} bytes")
        
        return if (useLocalWhisper && isWhisperInitialized) {
            speechToTextLocal(audioFile)
        } else {
            if (useLocalWhisper && !isWhisperInitialized) {
                android.util.Log.w("SpeechRepository", "本地 Whisper 未初始化，切換到遠端 API")
            }
            speechToTextRemote(audioFile)
        }
    }
    
    /**
     * 使用本地 Whisper 進行語音識別
     */
    private suspend fun speechToTextLocal(audioFile: File): Result<String> {
        return try {
            // 讀取音頻文件並轉換為 FloatArray
            val audioData = readAudioFile(audioFile)
            
            if (audioData.isEmpty()) {
                return Result.failure(Exception("音頻文件為空或格式不支持"))
            }
            
            android.util.Log.d("SpeechRepository", "使用本地 Whisper 識別，樣本數: ${audioData.size}")
            
            // 調用本地 Whisper
            val transcription = whisperNativeRepository.transcribeAudio(audioData)
            
            if (transcription.isNotBlank()) {
                android.util.Log.d("SpeechRepository", "本地識別結果: $transcription")
                Result.success(transcription)
            } else {
                Result.failure(Exception("本地 Whisper 識別失敗或返回空結果"))
            }
        } catch (e: Exception) {
            android.util.Log.e("SpeechRepository", "本地 Whisper 異常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 使用遠程 Whisper API 進行語音識別
     */
    private suspend fun speechToTextRemote(audioFile: File): Result<String> {
        return try {
            if (!isApiKeyValid) {
                android.util.Log.e("SpeechRepository", "OpenAI API Key 無效或未設置")
                return Result.failure(Exception("需要設置有效的 OpenAI API Key"))
            }
            
            if (!audioFile.exists() || audioFile.length() == 0L) {
                android.util.Log.e("SpeechRepository", "音頻文件不存在或為空")
                return Result.failure(Exception("音頻文件無效"))
            }
            
            android.util.Log.d("SpeechRepository", "使用遠端 Whisper API，文件大小: ${audioFile.length()} bytes")
            
            // 創建音頻文件的 RequestBody
            val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            
            // 創建模型和語言參數
            val modelPart = "whisper-1".toRequestBody("text/plain".toMediaTypeOrNull())
            val languagePart = "zh".toRequestBody("text/plain".toMediaTypeOrNull())
            
            // 調用 Whisper API
            val response = whisperApi.transcribeAudio(
                authorization = "Bearer $openAiApiKey",
                file = filePart,
                model = modelPart,
                language = languagePart
            )
            
            if (response.isSuccessful) {
                val transcription = response.body()?.text ?: ""
                Result.success(transcription)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Whisper API error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 讀取音頻文件並轉換為 FloatArray
     */
    private fun readAudioFile(audioFile: File): FloatArray {
        return try {
            if (!audioFile.exists()) {
                android.util.Log.e("SpeechRepository", "音頻文件不存在: ${audioFile.absolutePath}")
                return floatArrayOf()
            }
            
            // 簡化的音頻讀取（假設為 16-bit PCM）
            val bytes = audioFile.readBytes()
            val floats = FloatArray(bytes.size / 2)
            
            for (i in floats.indices) {
                val index = i * 2
                if (index + 1 < bytes.size) {
                    // Little-endian 16-bit PCM to float
                    val sample = ((bytes[index + 1].toInt() shl 8) or (bytes[index].toInt() and 0xFF)).toShort()
                    floats[i] = sample / 32768.0f
                }
            }
            
            android.util.Log.d("SpeechRepository", "音頻文件讀取完成: ${floats.size} 樣本")
            floats
        } catch (e: Exception) {
            android.util.Log.e("SpeechRepository", "讀取音頻文件失敗", e)
            floatArrayOf()
        }
    }

    override suspend fun processAiConversation(
        userInput: String, 
        conversationHistory: List<String>
    ): Result<String> {
        return try {
            // 添加網路連線診斷
            android.util.Log.d("SpeechRepository", "開始 OpenAI API 調用...")
            
            // 暫時跳過真實 API，直接使用 Mock（用於測試）
            // TODO: 恢復真實 API 調用
//            android.util.Log.w("SpeechRepository", "暫時使用 Mock 模式以避免網路問題")
//            return Result.success(getMockResponse(userInput))
            
            // 首先嘗試調用真實的OpenAI API
            val messages = mutableListOf<ChatMessage>()
            
            // 新增系統提示
            messages.add(ChatMessage("system", systemPrompt))
            
            // 新增對話歷史
            conversationHistory.forEachIndexed { index, message ->
                val role = if (index % 2 == 0) "user" else "assistant"
                messages.add(ChatMessage(role, message))
            }
            
            // 新增目前使用者輸入
            messages.add(ChatMessage("user", userInput))
            
            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = messages,
                maxTokens = 150,
                temperature = 0.7f
            )
            
            val response = openAiApi.createChatCompletion(
                authorization = "Bearer $openAiApiKey",
                request = request
            )
            
            if (response.isSuccessful) {
                val aiResponse = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                Result.success(aiResponse)
            } else {
                // API調用失敗，使用Mock回應作為備份
                android.util.Log.w("SpeechRepository", "OpenAI API失敗，使用Mock回應: ${response.code()}")
                Result.success(getMockResponse(userInput))
            }
        } catch (e: Exception) {
            // 網路或其他異常，使用Mock回應作為備份
            android.util.Log.w("SpeechRepository", "API調用異常，使用Mock回應: ${e.message}")
            Result.success(getMockResponse(userInput))
        }
    }

    /**
     * 獲取Mock回應（用於測試和API失敗時的備份）
     */
    private fun getMockResponse(userInput: String): String {
        val mockResponses = mapOf(
            "你好，今天天氣怎麼樣？" to "你好！我是智慧語音助理，很高興為您服務。關於天氣，我建議您查看天氣應用獲取準確資訊。☀️",
            "請告訴我一個笑話" to "為什麼程式設計師喜歡暗光？因為 Light attracts bugs！哈哈！😄 還需要其他笑話嗎？",
            "現在幾點了？" to "抱歉，我無法獲取當前時間，請查看您的設備時鐘。⏰ 有什麼其他我可以幫助您的嗎？",
            "你可以做什麼？" to "我可以進行對話、回答問題、提供幫助和建議。我是您的智慧語音助理！🤖 試試問我任何問題吧！",
            "謝謝你的幫助" to "不客氣！很高興能夠幫助您。😊 有任何問題都可以隨時問我！",
            "播放音樂" to "抱歉，我無法直接播放音樂，但我建議您使用音樂應用程式如 Spotify 或 Apple Music。🎵",
            "設定鬧鐘明天早上7點" to "很抱歉，我無法直接設定鬧鐘。請使用您手機的時鐘應用程式來設定明天早上7點的鬧鐘。⏰",
            "今天的新聞有什麼？" to "我無法獲取實時新聞，建議您查看新聞應用程式或網站獲取最新資訊。📰",
            "幫我查詢天氣預報" to "我無法直接查詢天氣預報，請使用天氣應用程式或詢問語音助理如 Siri 獲取準確的天氣資訊。🌤️",
            "我想學習新的知識" to "太棒了！學習是很好的習慣。您想學習什麼主題呢？我可以給您一些建議和學習方向。📚"
        )
        
        // 如果有完全匹配的回應，使用它
        mockResponses[userInput]?.let { return it }
        
        // 如果沒有完全匹配，基於關鍵詞提供回應
        return when {
            userInput.contains("你好") || userInput.contains("嗨") -> 
                "你好！很高興和您聊天！😊 有什麼我可以幫助您的嗎？"
            userInput.contains("謝謝") || userInput.contains("感謝") -> 
                "不客氣！很高興能幫助您！😊"
            userInput.contains("再見") || userInput.contains("拜拜") -> 
                "再見！期待下次和您聊天！👋"
            userInput.contains("時間") || userInput.contains("幾點") -> 
                "請查看您的設備時鐘獲取當前時間。⏰"
            userInput.contains("天氣") -> 
                "請使用天氣應用程式獲取準確的天氣資訊。🌤️"
            userInput.contains("音樂") -> 
                "建議您使用專門的音樂應用程式來播放音樂。🎵"
            userInput.contains("笑話") -> 
                "這裡有一個程式設計師笑話：為什麼 Java 工程師要戴眼鏡？因為他們看不見 C！😄"
            else -> 
                "感謝您的提問：「$userInput」。我正在不斷學習中，希望未來能更好地回答您的問題！🤖"
        }
    }

    override suspend fun textToSpeech(text: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            if (!isTtsInitialized || textToSpeech == null) {
                continuation.resume(Result.failure(Exception("TTS not initialized")))
                return@suspendCancellableCoroutine
            }
            
            val utteranceId = UUID.randomUUID().toString()
            
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }
                
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    if (continuation.isActive) {
                        continuation.resume(Result.success(Unit))
                    }
                }
                
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("TTS error")))
                    }
                }
            })
            
            val result = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            
            if (result != TextToSpeech.SUCCESS) {
                continuation.resume(Result.failure(Exception("TTS speak failed")))
            }
            
            continuation.invokeOnCancellation {
                stopTTS()
            }
        }
    }

    override fun stopTTS() {
        textToSpeech?.stop()
        isSpeaking = false
    }

    override fun isTTSSpeaking(): Boolean {
        return isSpeaking
    }

    override fun setTTSLanguage(language: String) {
        val locale = when (language) {
            "zh" -> Locale.CHINESE
            "en" -> Locale.ENGLISH
            else -> Locale.CHINESE
        }
        textToSpeech?.language = locale
    }

    override fun setTTSSpeed(speed: Float) {
        textToSpeech?.setSpeechRate(speed)
    }
}
