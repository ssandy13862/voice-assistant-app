package com.voiceassistant.app.domain.usecase;

/**
 * 语音助理核心业务逻辑
 */
@javax.inject.Singleton
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000b\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010!\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0018\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u00152\u0006\u0010\u001d\u001a\u00020\u0015H\u0002J\u0006\u0010\u001e\u001a\u00020\u001bJ\u0010\u0010\u001f\u001a\u00020\u001b2\u0006\u0010 \u001a\u00020\u0015H\u0002J\u0011\u0010!\u001a\u00020\u001bH\u0082@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\"J\u0006\u0010#\u001a\u00020\u001bJ\u0019\u0010$\u001a\u00020%2\u0006\u0010&\u001a\u00020\'H\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010(J\u0019\u0010)\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u0015H\u0082@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010*J\u0006\u0010+\u001a\u00020\u001bJ\u0019\u0010,\u001a\u00020\u001b2\u0006\u0010-\u001a\u00020\u0015H\u0082@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010*J\u0011\u0010.\u001a\u00020\u001bH\u0082@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\"J\b\u0010/\u001a\u00020\u001bH\u0002J\u0006\u00100\u001a\u00020\u001bR\u001a\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000e0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0014\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0017X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0012R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u00061"}, d2 = {"Lcom/voiceassistant/app/domain/usecase/VoiceAssistantUseCase;", "", "faceDetectionRepository", "Lcom/voiceassistant/app/domain/repository/FaceDetectionRepository;", "vadRepository", "Lcom/voiceassistant/app/domain/repository/VadRepository;", "speechRepository", "Lcom/voiceassistant/app/domain/repository/SpeechRepository;", "(Lcom/voiceassistant/app/domain/repository/FaceDetectionRepository;Lcom/voiceassistant/app/domain/repository/VadRepository;Lcom/voiceassistant/app/domain/repository/SpeechRepository;)V", "_conversationHistory", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/voiceassistant/app/domain/model/ConversationItem;", "_state", "Lcom/voiceassistant/app/domain/model/VoiceAssistantState;", "conversationHistory", "Lkotlinx/coroutines/flow/StateFlow;", "getConversationHistory", "()Lkotlinx/coroutines/flow/StateFlow;", "conversationMessages", "", "", "isFreeMode", "", "state", "getState", "addConversationItem", "", "userInput", "aiResponse", "clearConversationHistory", "handleError", "errorMessage", "handleSpeechDetected", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "interruptSpeaking", "processFaceDetection", "Lcom/voiceassistant/app/domain/model/FaceDetectionResult;", "imageProxy", "Landroidx/camera/core/ImageProxy;", "(Landroidx/camera/core/ImageProxy;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "processUserInput", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "release", "speakResponse", "response", "startListening", "stopListening", "toggleFreeMode", "app_debug"})
public final class VoiceAssistantUseCase {
    @org.jetbrains.annotations.NotNull
    private final com.voiceassistant.app.domain.repository.FaceDetectionRepository faceDetectionRepository = null;
    @org.jetbrains.annotations.NotNull
    private final com.voiceassistant.app.domain.repository.VadRepository vadRepository = null;
    @org.jetbrains.annotations.NotNull
    private final com.voiceassistant.app.domain.repository.SpeechRepository speechRepository = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<com.voiceassistant.app.domain.model.VoiceAssistantState> _state = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<com.voiceassistant.app.domain.model.VoiceAssistantState> state = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.voiceassistant.app.domain.model.ConversationItem>> _conversationHistory = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.voiceassistant.app.domain.model.ConversationItem>> conversationHistory = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.List<java.lang.String> conversationMessages = null;
    private boolean isFreeMode = false;
    
    @javax.inject.Inject
    public VoiceAssistantUseCase(@org.jetbrains.annotations.NotNull
    com.voiceassistant.app.domain.repository.FaceDetectionRepository faceDetectionRepository, @org.jetbrains.annotations.NotNull
    com.voiceassistant.app.domain.repository.VadRepository vadRepository, @org.jetbrains.annotations.NotNull
    com.voiceassistant.app.domain.repository.SpeechRepository speechRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.voiceassistant.app.domain.model.VoiceAssistantState> getState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.voiceassistant.app.domain.model.ConversationItem>> getConversationHistory() {
        return null;
    }
    
    /**
     * 处理人脸检测结果
     */
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object processFaceDetection(@org.jetbrains.annotations.NotNull
    androidx.camera.core.ImageProxy imageProxy, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.voiceassistant.app.domain.model.FaceDetectionResult> $completion) {
        return null;
    }
    
    /**
     * 开始语音监听
     */
    private final java.lang.Object startListening(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 停止语音监听
     */
    private final void stopListening() {
    }
    
    /**
     * 处理检测到的语音
     */
    private final java.lang.Object handleSpeechDetected(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 处理用户输入
     */
    private final java.lang.Object processUserInput(java.lang.String userInput, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 语音合成并播放AI回应
     */
    private final java.lang.Object speakResponse(java.lang.String response, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 添加对话项到历史记录
     */
    private final void addConversationItem(java.lang.String userInput, java.lang.String aiResponse) {
    }
    
    /**
     * 切换自由模式
     */
    public final void toggleFreeMode() {
    }
    
    /**
     * 手动停止TTS（用于插话打断）
     */
    public final void interruptSpeaking() {
    }
    
    /**
     * 清除对话历史
     */
    public final void clearConversationHistory() {
    }
    
    /**
     * 处理错误
     */
    private final void handleError(java.lang.String errorMessage) {
    }
    
    /**
     * 释放资源
     */
    public final void release() {
    }
}