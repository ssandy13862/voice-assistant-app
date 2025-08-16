package com.voiceassistant.app.di

import com.voiceassistant.app.data.repository.AudioRecorderRepository
import com.voiceassistant.app.data.repository.FaceDetectionRepositoryImpl
import com.voiceassistant.app.data.repository.SpeechRepositoryImpl
import com.voiceassistant.app.data.repository.VadRepositoryImpl
import com.voiceassistant.app.data.repository.WhisperNativeRepository
import com.voiceassistant.app.domain.repository.FaceDetectionRepository
import com.voiceassistant.app.domain.repository.SpeechRepository
import com.voiceassistant.app.domain.repository.VadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 倉庫介面綁定模組
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFaceDetectionRepository(
        faceDetectionRepositoryImpl: FaceDetectionRepositoryImpl
    ): FaceDetectionRepository

    @Binds
    @Singleton
    abstract fun bindVadRepository(
        vadRepositoryImpl: VadRepositoryImpl
    ): VadRepository

    @Binds
    @Singleton
    abstract fun bindSpeechRepository(
        speechRepositoryImpl: SpeechRepositoryImpl
    ): SpeechRepository
}
