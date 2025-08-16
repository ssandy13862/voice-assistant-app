package com.voiceassistant.app.data.api;

/**
 * OpenAI API接口
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J+\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\bH\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\t\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\n"}, d2 = {"Lcom/voiceassistant/app/data/api/OpenAiApi;", "", "createChatCompletion", "Lretrofit2/Response;", "Lcom/voiceassistant/app/data/api/ChatCompletionResponse;", "authorization", "", "request", "Lcom/voiceassistant/app/data/api/ChatCompletionRequest;", "(Ljava/lang/String;Lcom/voiceassistant/app/data/api/ChatCompletionRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface OpenAiApi {
    
    @retrofit2.http.POST(value = "chat/completions")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object createChatCompletion(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull
    java.lang.String authorization, @retrofit2.http.Body
    @org.jetbrains.annotations.NotNull
    com.voiceassistant.app.data.api.ChatCompletionRequest request, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.voiceassistant.app.data.api.ChatCompletionResponse>> $completion);
}