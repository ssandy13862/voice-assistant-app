package com.voiceassistant.app.domain.repository;

/**
 * 语音处理仓库接口（STT + AI对话 + TTS）
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J8\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\u0006\u0010\u0007\u001a\u00020\u00062\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00060\tH\u00a6@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00f8\u0001\u0002\u00f8\u0001\u0002\u00a2\u0006\u0004\b\n\u0010\u000bJ\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u0006H&J\u0010\u0010\u000f\u001a\u00020\r2\u0006\u0010\u0010\u001a\u00020\u0011H&J*\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\u0006\u0010\u0013\u001a\u00020\u0014H\u00a6@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00f8\u0001\u0002\u00f8\u0001\u0002\u00a2\u0006\u0004\b\u0015\u0010\u0016J\b\u0010\u0017\u001a\u00020\rH&J*\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\r0\u00052\u0006\u0010\u0019\u001a\u00020\u0006H\u00a6@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00f8\u0001\u0002\u00f8\u0001\u0002\u00a2\u0006\u0004\b\u001a\u0010\u001b\u0082\u0002\u000f\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\n\u0002\b\u0019\u00a8\u0006\u001c"}, d2 = {"Lcom/voiceassistant/app/domain/repository/SpeechRepository;", "", "isTTSSpeaking", "", "processAiConversation", "Lkotlin/Result;", "", "userInput", "conversationHistory", "", "processAiConversation-0E7RQCE", "(Ljava/lang/String;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setTTSLanguage", "", "language", "setTTSSpeed", "speed", "", "speechToText", "audioFile", "Ljava/io/File;", "speechToText-gIAlu-s", "(Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "stopTTS", "textToSpeech", "text", "textToSpeech-gIAlu-s", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface SpeechRepository {
    
    /**
     * 停止TTS播放
     */
    public abstract void stopTTS();
    
    /**
     * 检查TTS是否正在播放
     */
    public abstract boolean isTTSSpeaking();
    
    /**
     * 设置TTS语言
     */
    public abstract void setTTSLanguage(@org.jetbrains.annotations.NotNull
    java.lang.String language);
    
    /**
     * 设置TTS语速
     */
    public abstract void setTTSSpeed(float speed);
}