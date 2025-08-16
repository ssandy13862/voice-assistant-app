package com.voiceassistant.app.domain.model;

/**
 * 语音助理状态枚举
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\b\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\t"}, d2 = {"Lcom/voiceassistant/app/domain/model/VoiceAssistantState;", "", "(Ljava/lang/String;I)V", "IDLE", "DETECTING", "LISTENING", "PROCESSING", "SPEAKING", "ERROR", "app_debug"})
public enum VoiceAssistantState {
    /*public static final*/ IDLE /* = new IDLE() */,
    /*public static final*/ DETECTING /* = new DETECTING() */,
    /*public static final*/ LISTENING /* = new LISTENING() */,
    /*public static final*/ PROCESSING /* = new PROCESSING() */,
    /*public static final*/ SPEAKING /* = new SPEAKING() */,
    /*public static final*/ ERROR /* = new ERROR() */;
    
    VoiceAssistantState() {
    }
    
    @org.jetbrains.annotations.NotNull
    public static kotlin.enums.EnumEntries<com.voiceassistant.app.domain.model.VoiceAssistantState> getEntries() {
        return null;
    }
}