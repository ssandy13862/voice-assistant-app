package com.voiceassistant.app.data.repository

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
    private val whisperApi: WhisperApi
) : SpeechRepository {

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isSpeaking = false
    
    // OpenAI API配置
    private val openAiApiKey = "your_openai_api_key_here" // 需要從配置檔讀取
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
        return try {
            val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            
            val response = whisperApi.transcribeAudio(audioFile)
            
            if (response.isSuccessful) {
                val transcription = response.body()?.text ?: ""
                Result.success(transcription)
            } else {
                Result.failure(Exception("STT API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun processAiConversation(
        userInput: String, 
        conversationHistory: List<String>
    ): Result<String> {
        return try {
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
                Result.failure(Exception("OpenAI API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
