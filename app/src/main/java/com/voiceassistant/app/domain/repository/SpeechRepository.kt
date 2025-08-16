package com.voiceassistant.app.domain.repository

import com.voiceassistant.app.domain.model.VoiceProcessingResult
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * 語音處理倉庫介面（STT + AI對話 + TTS）
 */
interface SpeechRepository {
    
    /**
     * 語音轉文字 (STT)
     */
    suspend fun speechToText(audioFile: File): Result<String>
    
    /**
     * AI對話處理
     */
    suspend fun processAiConversation(userInput: String, conversationHistory: List<String>): Result<String>
    
    /**
     * 文字轉語音 (TTS)
     */
    suspend fun textToSpeech(text: String): Result<Unit>
    
    /**
     * 停止TTS播放
     */
    fun stopTTS()
    
    /**
     * 檢查TTS是否正在播放
     */
    fun isTTSSpeaking(): Boolean
    
    /**
     * 設定TTS語言
     */
    fun setTTSLanguage(language: String)
    
    /**
     * 設定TTS語速
     */
    fun setTTSSpeed(speed: Float)
}
