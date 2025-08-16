package com.voiceassistant.app.data.repository;

/**
 * Google ML Kit人脸检测实现
 */
@javax.inject.Singleton
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u0019\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0096@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\rJ\u0010\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0004H\u0016J\u000e\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\n0\u0012H\u0016J\b\u0010\u0013\u001a\u00020\u000fH\u0016R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u0014"}, d2 = {"Lcom/voiceassistant/app/data/repository/FaceDetectionRepositoryImpl;", "Lcom/voiceassistant/app/domain/repository/FaceDetectionRepository;", "()V", "detectionSensitivity", "", "faceDetector", "Lcom/google/mlkit/vision/face/FaceDetector;", "faceDetectorOptions", "Lcom/google/mlkit/vision/face/FaceDetectorOptions;", "detectFaces", "Lcom/voiceassistant/app/domain/model/FaceDetectionResult;", "imageProxy", "Landroidx/camera/core/ImageProxy;", "(Landroidx/camera/core/ImageProxy;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setDetectionSensitivity", "", "sensitivity", "startFaceDetection", "Lkotlinx/coroutines/flow/Flow;", "stopFaceDetection", "app_debug"})
public final class FaceDetectionRepositoryImpl implements com.voiceassistant.app.domain.repository.FaceDetectionRepository {
    @org.jetbrains.annotations.NotNull
    private final com.google.mlkit.vision.face.FaceDetectorOptions faceDetectorOptions = null;
    @org.jetbrains.annotations.NotNull
    private final com.google.mlkit.vision.face.FaceDetector faceDetector = null;
    private float detectionSensitivity = 0.5F;
    
    @javax.inject.Inject
    public FaceDetectionRepositoryImpl() {
        super();
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.Nullable
    public java.lang.Object detectFaces(@org.jetbrains.annotations.NotNull
    androidx.camera.core.ImageProxy imageProxy, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.voiceassistant.app.domain.model.FaceDetectionResult> $completion) {
        return null;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public kotlinx.coroutines.flow.Flow<com.voiceassistant.app.domain.model.FaceDetectionResult> startFaceDetection() {
        return null;
    }
    
    @java.lang.Override
    public void stopFaceDetection() {
    }
    
    @java.lang.Override
    public void setDetectionSensitivity(float sensitivity) {
    }
}