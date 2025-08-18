package com.voiceassistant.app.di

import com.voiceassistant.app.data.api.OpenAiApi
import com.voiceassistant.app.data.api.WhisperApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * 網路相關依賴注入模組
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            // 使用 BASIC 級別避免記錄敏感數據 (API Keys, 音頻內容)
            level = if (com.voiceassistant.app.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request()
                android.util.Log.d("NetworkModule", "Making request to: ${request.url}")
                try {
                    val response = chain.proceed(request)
                    android.util.Log.d("NetworkModule", "Request successful: ${response.code}")
                    response
                } catch (e: Exception) {
                    android.util.Log.e("NetworkModule", "Request failed: ${e.message}")
                    throw e
                }
            }
            .build()
    }

    @Provides
    @Singleton
    @Named("openai")
    fun provideOpenAiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAiApi(@Named("openai") retrofit: Retrofit): OpenAiApi {
        return retrofit.create(OpenAiApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWhisperApi(@Named("openai") retrofit: Retrofit): WhisperApi {
        return retrofit.create(WhisperApi::class.java)
    }
}
