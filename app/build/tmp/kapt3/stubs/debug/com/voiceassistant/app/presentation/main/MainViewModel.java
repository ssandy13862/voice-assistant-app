package com.voiceassistant.app.presentation.main;

/**
 * 主界面ViewModel
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010 \u001a\u00020!J\u000e\u0010\"\u001a\u00020#2\u0006\u0010$\u001a\u00020\u0010J\u000e\u0010%\u001a\u00020\u00072\u0006\u0010$\u001a\u00020\u0010J\u0006\u0010&\u001a\u00020!J\b\u0010\'\u001a\u00020!H\u0014J\u000e\u0010(\u001a\u00020!2\u0006\u0010)\u001a\u00020*J\u000e\u0010+\u001a\u00020!2\u0006\u0010,\u001a\u00020\fJ\u0006\u0010-\u001a\u00020!J\u0006\u0010.\u001a\u00020!R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\b\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\n0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\f0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u001d\u0010\u0013\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00150\u00140\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0012R\u0017\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00070\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0019\u0010\u001b\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\n0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0012R\u0017\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\f0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0012R\u0017\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\f0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0012R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006/"}, d2 = {"Lcom/voiceassistant/app/presentation/main/MainViewModel;", "Landroidx/lifecycle/ViewModel;", "voiceAssistantUseCase", "Lcom/voiceassistant/app/domain/usecase/VoiceAssistantUseCase;", "(Lcom/voiceassistant/app/domain/usecase/VoiceAssistantUseCase;)V", "_errorMessage", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "", "_faceDetectionResult", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/voiceassistant/app/domain/model/FaceDetectionResult;", "_isFreeMode", "", "_permissionsGranted", "assistantState", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/voiceassistant/app/domain/model/VoiceAssistantState;", "getAssistantState", "()Lkotlinx/coroutines/flow/StateFlow;", "conversationHistory", "", "Lcom/voiceassistant/app/domain/model/ConversationItem;", "getConversationHistory", "errorMessage", "Lkotlinx/coroutines/flow/SharedFlow;", "getErrorMessage", "()Lkotlinx/coroutines/flow/SharedFlow;", "faceDetectionResult", "getFaceDetectionResult", "isFreeMode", "permissionsGranted", "getPermissionsGranted", "clearConversationHistory", "", "getStateColor", "", "state", "getStateDisplayText", "interruptSpeaking", "onCleared", "processCameraFrame", "imageProxy", "Landroidx/camera/core/ImageProxy;", "setPermissionsGranted", "granted", "startManualVoiceInput", "toggleFreeMode", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel
public final class MainViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull
    private final com.voiceassistant.app.domain.usecase.VoiceAssistantUseCase voiceAssistantUseCase = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<com.voiceassistant.app.domain.model.VoiceAssistantState> assistantState = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.voiceassistant.app.domain.model.ConversationItem>> conversationHistory = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<com.voiceassistant.app.domain.model.FaceDetectionResult> _faceDetectionResult = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<com.voiceassistant.app.domain.model.FaceDetectionResult> faceDetectionResult = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isFreeMode = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isFreeMode = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableSharedFlow<java.lang.String> _errorMessage = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.SharedFlow<java.lang.String> errorMessage = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _permissionsGranted = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> permissionsGranted = null;
    
    @javax.inject.Inject
    public MainViewModel(@org.jetbrains.annotations.NotNull
    com.voiceassistant.app.domain.usecase.VoiceAssistantUseCase voiceAssistantUseCase) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.voiceassistant.app.domain.model.VoiceAssistantState> getAssistantState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.voiceassistant.app.domain.model.ConversationItem>> getConversationHistory() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.voiceassistant.app.domain.model.FaceDetectionResult> getFaceDetectionResult() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isFreeMode() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.SharedFlow<java.lang.String> getErrorMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getPermissionsGranted() {
        return null;
    }
    
    /**
     * 处理相机图像帧进行人脸检测
     */
    public final void processCameraFrame(@org.jetbrains.annotations.NotNull
    androidx.camera.core.ImageProxy imageProxy) {
    }
    
    /**
     * 切换自由模式
     */
    public final void toggleFreeMode() {
    }
    
    /**
     * 手动触发语音输入（用于测试）
     */
    public final void startManualVoiceInput() {
    }
    
    /**
     * 中断TTS播放
     */
    public final void interruptSpeaking() {
    }
    
    /**
     * 清除对话历史
     */
    public final void clearConversationHistory() {
    }
    
    /**
     * 设置权限状态
     */
    public final void setPermissionsGranted(boolean granted) {
    }
    
    /**
     * 获取状态显示文本
     */
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getStateDisplayText(@org.jetbrains.annotations.NotNull
    com.voiceassistant.app.domain.model.VoiceAssistantState state) {
        return null;
    }
    
    /**
     * 获取状态颜色资源ID
     */
    public final int getStateColor(@org.jetbrains.annotations.NotNull
    com.voiceassistant.app.domain.model.VoiceAssistantState state) {
        return 0;
    }
    
    @java.lang.Override
    protected void onCleared() {
    }
}