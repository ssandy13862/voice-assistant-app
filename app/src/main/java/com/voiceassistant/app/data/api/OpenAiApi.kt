package com.voiceassistant.app.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*
import java.io.File

/**
 * OpenAI API介面
 */
interface OpenAiApi {
    
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}

/**
 * Whisper API介面（自架服務）
 */
interface WhisperApi {
    
    @Multipart
    @POST("transcribe")
    suspend fun transcribeAudio(
        @Part("file") audioFile: File,
        @Part("model") model: String = "whisper-1",
        @Part("language") language: String = "zh"
    ): Response<TranscriptionResponse>
}

// OpenAI API資料模型
data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    @SerializedName("temperature") val temperature: Float? = null
)

data class ChatMessage(
    @SerializedName("role") val role: String, // "system", "user", "assistant"
    @SerializedName("content") val content: String
)

data class ChatCompletionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("object") val objectType: String,
    @SerializedName("created") val created: Long,
    @SerializedName("model") val model: String,
    @SerializedName("choices") val choices: List<Choice>,
    @SerializedName("usage") val usage: Usage?
)

data class Choice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

// Whisper API資料模型
data class TranscriptionResponse(
    @SerializedName("text") val text: String,
    @SerializedName("language") val language: String? = null,
    @SerializedName("duration") val duration: Float? = null
)
