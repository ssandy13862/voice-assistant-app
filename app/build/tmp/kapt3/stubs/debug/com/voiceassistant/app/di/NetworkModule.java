package com.voiceassistant.app.di;

/**
 * 网络相关依赖注入模块
 */
@dagger.Module
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0007J\u0010\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u0004H\u0007J\u0012\u0010\b\u001a\u00020\t2\b\b\u0001\u0010\n\u001a\u00020\u000bH\u0007J\u0010\u0010\f\u001a\u00020\u000b2\u0006\u0010\r\u001a\u00020\u0006H\u0007J\u0012\u0010\u000e\u001a\u00020\u000f2\b\b\u0001\u0010\n\u001a\u00020\u000bH\u0007J\u0010\u0010\u0010\u001a\u00020\u000b2\u0006\u0010\r\u001a\u00020\u0006H\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/voiceassistant/app/di/NetworkModule;", "", "()V", "provideHttpLoggingInterceptor", "Lokhttp3/logging/HttpLoggingInterceptor;", "provideOkHttpClient", "Lokhttp3/OkHttpClient;", "loggingInterceptor", "provideOpenAiApi", "Lcom/voiceassistant/app/data/api/OpenAiApi;", "retrofit", "Lretrofit2/Retrofit;", "provideOpenAiRetrofit", "okHttpClient", "provideWhisperApi", "Lcom/voiceassistant/app/data/api/WhisperApi;", "provideWhisperRetrofit", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class NetworkModule {
    @org.jetbrains.annotations.NotNull
    public static final com.voiceassistant.app.di.NetworkModule INSTANCE = null;
    
    private NetworkModule() {
        super();
    }
    
    @dagger.Provides
    @javax.inject.Singleton
    @org.jetbrains.annotations.NotNull
    public final okhttp3.logging.HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        return null;
    }
    
    @dagger.Provides
    @javax.inject.Singleton
    @org.jetbrains.annotations.NotNull
    public final okhttp3.OkHttpClient provideOkHttpClient(@org.jetbrains.annotations.NotNull
    okhttp3.logging.HttpLoggingInterceptor loggingInterceptor) {
        return null;
    }
    
    @dagger.Provides
    @javax.inject.Singleton
    @javax.inject.Named(value = "openai")
    @org.jetbrains.annotations.NotNull
    public final retrofit2.Retrofit provideOpenAiRetrofit(@org.jetbrains.annotations.NotNull
    okhttp3.OkHttpClient okHttpClient) {
        return null;
    }
    
    @dagger.Provides
    @javax.inject.Singleton
    @javax.inject.Named(value = "whisper")
    @org.jetbrains.annotations.NotNull
    public final retrofit2.Retrofit provideWhisperRetrofit(@org.jetbrains.annotations.NotNull
    okhttp3.OkHttpClient okHttpClient) {
        return null;
    }
    
    @dagger.Provides
    @javax.inject.Singleton
    @org.jetbrains.annotations.NotNull
    public final com.voiceassistant.app.data.api.OpenAiApi provideOpenAiApi(@javax.inject.Named(value = "openai")
    @org.jetbrains.annotations.NotNull
    retrofit2.Retrofit retrofit) {
        return null;
    }
    
    @dagger.Provides
    @javax.inject.Singleton
    @org.jetbrains.annotations.NotNull
    public final com.voiceassistant.app.data.api.WhisperApi provideWhisperApi(@javax.inject.Named(value = "whisper")
    @org.jetbrains.annotations.NotNull
    retrofit2.Retrofit retrofit) {
        return null;
    }
}