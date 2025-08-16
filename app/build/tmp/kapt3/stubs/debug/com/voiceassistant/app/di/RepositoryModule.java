package com.voiceassistant.app.di;

/**
 * 仓库接口绑定模块
 */
@dagger.Module
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\'J\u0010\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\'\u00a8\u0006\u000f"}, d2 = {"Lcom/voiceassistant/app/di/RepositoryModule;", "", "()V", "bindFaceDetectionRepository", "Lcom/voiceassistant/app/domain/repository/FaceDetectionRepository;", "faceDetectionRepositoryImpl", "Lcom/voiceassistant/app/data/repository/FaceDetectionRepositoryImpl;", "bindSpeechRepository", "Lcom/voiceassistant/app/domain/repository/SpeechRepository;", "speechRepositoryImpl", "Lcom/voiceassistant/app/data/repository/SpeechRepositoryImpl;", "bindVadRepository", "Lcom/voiceassistant/app/domain/repository/VadRepository;", "vadRepositoryImpl", "Lcom/voiceassistant/app/data/repository/VadRepositoryImpl;", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public abstract class RepositoryModule {
    
    public RepositoryModule() {
        super();
    }
    
    @dagger.Binds
    @javax.inject.Singleton
    @org.jetbrains.annotations.NotNull
    public abstract com.voiceassistant.app.domain.repository.FaceDetectionRepository bindFaceDetectionRepository(@org.jetbrains.annotations.NotNull
    com.voiceassistant.app.data.repository.FaceDetectionRepositoryImpl faceDetectionRepositoryImpl);
    
    @dagger.Binds
    @javax.inject.Singleton
    @org.jetbrains.annotations.NotNull
    public abstract com.voiceassistant.app.domain.repository.VadRepository bindVadRepository(@org.jetbrains.annotations.NotNull
    com.voiceassistant.app.data.repository.VadRepositoryImpl vadRepositoryImpl);
    
    @dagger.Binds
    @javax.inject.Singleton
    @org.jetbrains.annotations.NotNull
    public abstract com.voiceassistant.app.domain.repository.SpeechRepository bindSpeechRepository(@org.jetbrains.annotations.NotNull
    com.voiceassistant.app.data.repository.SpeechRepositoryImpl speechRepositoryImpl);
}