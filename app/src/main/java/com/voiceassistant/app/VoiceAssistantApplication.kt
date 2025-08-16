package com.voiceassistant.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 語音助理應用程式類別
 * 使用Hilt進行依賴注入
 */
@HiltAndroidApp
class VoiceAssistantApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 應用程式初始化邏輯
    }
}
