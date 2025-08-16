package com.voiceassistant.app.domain.repository;

/**
 * 语音活动检测(VAD)仓库接口
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0011\u0010\u0002\u001a\u00020\u0003H\u00a6@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0004J\u0019\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u00a6@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\tJ\b\u0010\n\u001a\u00020\u000bH&J\u0010\u0010\f\u001a\u00020\u000b2\u0006\u0010\r\u001a\u00020\u000eH&J\u000e\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00060\u0010H&J\b\u0010\u0011\u001a\u00020\u000bH&\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u0012"}, d2 = {"Lcom/voiceassistant/app/domain/repository/VadRepository;", "", "initializeVad", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "processAudioData", "Lcom/voiceassistant/app/domain/model/AudioState;", "audioData", "", "([BLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "release", "", "setVadThreshold", "threshold", "", "startAudioDetection", "Lkotlinx/coroutines/flow/Flow;", "stopAudioDetection", "app_debug"})
public abstract interface VadRepository {
    
    /**
     * 初始化VAD模型
     */
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object initializeVad(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
    
    /**
     * 开始音频检测
     */
    @org.jetbrains.annotations.NotNull
    public abstract kotlinx.coroutines.flow.Flow<com.voiceassistant.app.domain.model.AudioState> startAudioDetection();
    
    /**
     * 停止音频检测
     */
    public abstract void stopAudioDetection();
    
    /**
     * 处理音频数据并返回检测结果
     */
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object processAudioData(@org.jetbrains.annotations.NotNull
    byte[] audioData, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.voiceassistant.app.domain.model.AudioState> $completion);
    
    /**
     * 设置检测阈值
     */
    public abstract void setVadThreshold(float threshold);
    
    /**
     * 释放资源
     */
    public abstract void release();
}