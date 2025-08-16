package com.voiceassistant.app.data.api;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\bJ\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0005H\u00c6\u0003J\u000b\u0010\u0011\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003J)\u0010\u0012\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0007H\u00c6\u0001J\u0013\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0016\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u0017\u001a\u00020\u0007H\u00d6\u0001R\u0018\u0010\u0006\u001a\u0004\u0018\u00010\u00078\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0016\u0010\u0004\u001a\u00020\u00058\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u0018"}, d2 = {"Lcom/voiceassistant/app/data/api/Choice;", "", "index", "", "message", "Lcom/voiceassistant/app/data/api/ChatMessage;", "finishReason", "", "(ILcom/voiceassistant/app/data/api/ChatMessage;Ljava/lang/String;)V", "getFinishReason", "()Ljava/lang/String;", "getIndex", "()I", "getMessage", "()Lcom/voiceassistant/app/data/api/ChatMessage;", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
public final class Choice {
    @com.google.gson.annotations.SerializedName(value = "index")
    private final int index = 0;
    @com.google.gson.annotations.SerializedName(value = "message")
    @org.jetbrains.annotations.NotNull
    private final com.voiceassistant.app.data.api.ChatMessage message = null;
    @com.google.gson.annotations.SerializedName(value = "finish_reason")
    @org.jetbrains.annotations.Nullable
    private final java.lang.String finishReason = null;
    
    public Choice(int index, @org.jetbrains.annotations.NotNull
    com.voiceassistant.app.data.api.ChatMessage message, @org.jetbrains.annotations.Nullable
    java.lang.String finishReason) {
        super();
    }
    
    public final int getIndex() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.voiceassistant.app.data.api.ChatMessage getMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String getFinishReason() {
        return null;
    }
    
    public final int component1() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.voiceassistant.app.data.api.ChatMessage component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.voiceassistant.app.data.api.Choice copy(int index, @org.jetbrains.annotations.NotNull
    com.voiceassistant.app.data.api.ChatMessage message, @org.jetbrains.annotations.Nullable
    java.lang.String finishReason) {
        return null;
    }
    
    @java.lang.Override
    public boolean equals(@org.jetbrains.annotations.Nullable
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public java.lang.String toString() {
        return null;
    }
}