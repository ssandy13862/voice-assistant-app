package com.voiceassistant.app.domain.repository;

/**
 * 人脸检测仓库接口
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0019\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH&J\u000e\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00030\fH&J\b\u0010\r\u001a\u00020\bH&\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u000e"}, d2 = {"Lcom/voiceassistant/app/domain/repository/FaceDetectionRepository;", "", "detectFaces", "Lcom/voiceassistant/app/domain/model/FaceDetectionResult;", "imageProxy", "Landroidx/camera/core/ImageProxy;", "(Landroidx/camera/core/ImageProxy;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setDetectionSensitivity", "", "sensitivity", "", "startFaceDetection", "Lkotlinx/coroutines/flow/Flow;", "stopFaceDetection", "app_debug"})
public abstract interface FaceDetectionRepository {
    
    /**
     * 检测图像中的人脸
     */
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object detectFaces(@org.jetbrains.annotations.NotNull
    androidx.camera.core.ImageProxy imageProxy, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.voiceassistant.app.domain.model.FaceDetectionResult> $completion);
    
    /**
     * 开始人脸检测流
     */
    @org.jetbrains.annotations.NotNull
    public abstract kotlinx.coroutines.flow.Flow<com.voiceassistant.app.domain.model.FaceDetectionResult> startFaceDetection();
    
    /**
     * 停止人脸检测
     */
    public abstract void stopFaceDetection();
    
    /**
     * 设置检测灵敏度
     */
    public abstract void setDetectionSensitivity(float sensitivity);
}