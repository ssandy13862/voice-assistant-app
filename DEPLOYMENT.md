# 部署指南

## 環境準備

### 開發環境

1. **Android Studio**
   - 版本: Arctic Fox (2020.3.1) 或更高
   - JDK: Oracle JDK 8 或 OpenJDK 8+

2. **Android SDK**
   - Min SDK: API 24 (Android 7.0)
   - Target SDK: API 34 (Android 14)
   - 所需工具: SDK Build Tools 30.0.3+

3. **設備要求**
   - 前置攝影機支援
   - 麥克風權限
   - 網路連線能力

## API 服務配置

### 1. OpenAI API 配置

1. 獲取 OpenAI API Key:
   - 造訪 https://platform.openai.com/
   - 註冊帳戶並獲取 API Key

2. 配置 API Key:
   ```kotlin
   // 在 SpeechRepositoryImpl.kt 中
   private val openAiApiKey = "sk-your-api-key-here"
   ```

   **生產環境建議**:
   ```kotlin
   // 使用環境變數或安全儲存
   private val openAiApiKey = BuildConfig.OPENAI_API_KEY
   ```

### 2. Whisper API 部署

#### 方案一: 使用 Docker 部署

1. **拉取 Whisper 映像檔**:
   ```bash
   docker pull openai/whisper
   ```

2. **運行服務**:
   ```bash
   docker run -d -p 8000:8000 \
     --name whisper-api \
     openai/whisper:latest
   ```

3. **配置應用端點**:
   ```kotlin
   // 在 NetworkModule.kt 中
   .baseUrl("http://your-server-ip:8000/")
   ```

#### 方案二: 使用雲端服務部署

1. **部署到雲端平台** (AWS, GCP, Azure)
2. **配置負載平衡和自動擴展**
3. **設定HTTPS和網域名稱**

### 3. VAD 模型配置

#### 目前實作 (簡化VAD)
- 基於音頻能量檢測
- 無需額外配置

#### 推薦升級 (Silero VAD)

1. **下載模型檔案**:
   ```bash
   wget https://github.com/snakers4/silero-vad/releases/download/v3.1/silero_vad.onnx
   ```

2. **整合到應用程式**:
   ```kotlin
   // 將模型檔案放置到 assets 目錄
   app/src/main/assets/silero_vad.onnx
   ```

## 建構配置

### 1. Gradle 配置

#### 開發環境 (debug)
```gradle
android {
    buildTypes {
        debug {
            applicationIdSuffix ".debug"
            debuggable true
            minifyEnabled false
            buildConfigField "String", "API_BASE_URL", "\"http://10.0.2.2:8000/\""
            buildConfigField "String", "OPENAI_API_KEY", "\"sk-dev-key\""
        }
    }
}
```

#### 生產環境 (release)
```gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            buildConfigField "String", "API_BASE_URL", "\"https://your-whisper-api.com/\""
            buildConfigField "String", "OPENAI_API_KEY", "\"${System.getenv('OPENAI_API_KEY')}\""
        }
    }
}
```

### 2. 簽名配置

1. **產生金鑰庫**:
   ```bash
   keytool -genkey -v -keystore voice-assistant.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias voice-assistant-key
   ```

2. **配置簽名**:
   ```gradle
   android {
       signingConfigs {
           release {
               keyAlias 'voice-assistant-key'
               keyPassword 'your-key-password'
               storeFile file('voice-assistant.jks')
               storePassword 'your-store-password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
           }
       }
   }
   ```

## 建構和發布

### 1. 本地建構

```bash
# 清理專案
./gradlew clean

# Debug 建構
./gradlew assembleDebug

# Release 建構
./gradlew assembleRelease

# 運行測試
./gradlew test

# 產生 APK
./gradlew assembleRelease
```

### 2. CI/CD 配置

#### GitHub Actions 範例

```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'adopt'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run tests
      run: ./gradlew test
      
    - name: Build Debug APK
      run: ./gradlew assembleDebug
      
    - name: Build Release APK
      run: ./gradlew assembleRelease
      env:
        OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
        
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

### 3. 應用程式商店發布

#### Google Play Store

1. **準備發布包**:
   ```bash
   ./gradlew bundleRelease
   ```

2. **上傳 AAB 檔案**:
   - 檔案位置: `app/build/outputs/bundle/release/app-release.aab`

3. **應用程式資訊配置**:
   - 應用程式名稱: "智慧語音助理"
   - 套件名稱: `com.voiceassistant.app`
   - 版本號: 遵循語意化版本

4. **權限說明**:
   - 相機權限: 用於人臉檢測觸發語音助理
   - 麥克風權限: 用於語音輸入和語音活動檢測
   - 網路權限: 用於AI對話和語音識別服務

## 監控和維護

### 1. 應用程式效能監控

#### Firebase Performance Monitoring

1. **新增依賴**:
   ```gradle
   implementation 'com.google.firebase:firebase-perf-ktx:20.4.1'
   ```

2. **配置監控**:
   ```kotlin
   // 監控關鍵方法
   val trace = FirebasePerformance.startTrace("face_detection")
   // ... 人臉檢測邏輯
   trace.stop()
   ```

### 2. 錯誤監控

#### Firebase Crashlytics

1. **新增依賴**:
   ```gradle
   implementation 'com.google.firebase:firebase-crashlytics-ktx:18.4.3'
   ```

2. **錯誤報告**:
   ```kotlin
   try {
       // 語音處理邏輯
   } catch (e: Exception) {
       FirebaseCrashlytics.getInstance().recordException(e)
       // 錯誤處理
   }
   ```

### 3. 使用者分析

#### Firebase Analytics

1. **事件追蹤**:
   ```kotlin
   // 記錄關鍵事件
   firebaseAnalytics.logEvent("voice_interaction_started", Bundle().apply {
       putString("trigger_type", "face_detection")
   })
   ```

## 安全考量

### 1. API Key 保護

```kotlin
// 不要硬編碼在程式碼中
❌ private val apiKey = "sk-1234567890"

// 使用 BuildConfig
✅ private val apiKey = BuildConfig.OPENAI_API_KEY

// 或使用加密儲存
✅ private val apiKey = EncryptedSharedPreferences.getString("api_key", "")
```

### 2. 網路安全

```kotlin
// 配置網路安全
android {
    networkSecurityConfig = "@xml/network_security_config"
}
```

```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system"/>
        </trust-anchors>
    </base-config>
</network-security-config>
```

### 3. 資料保護

```kotlin
// 敏感資料加密儲存
val encryptedPrefs = EncryptedSharedPreferences.create(
    "secure_prefs",
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

## 故障排除

### 常見問題

1. **相機權限被拒絕**:
   - 檢查 `AndroidManifest.xml` 權限聲明
   - 確保執行時權限請求程式碼正確

2. **網路連線失敗**:
   - 檢查網路權限
   - 驗證 API 端點 URL
   - 檢查防火牆和代理設定

3. **人臉檢測不運作**:
   - 確保設備有前置攝影機
   - 檢查光線條件
   - 驗證 ML Kit 依賴是否正確新增

4. **TTS 無聲音**:
   - 檢查設備音量設定
   - 驗證 TTS 引擎是否可用
   - 檢查語言設定

### 除錯工具

1. **日誌查看**:
   ```bash
   adb logcat -s VoiceAssistant
   ```

2. **網路除錯**:
   ```kotlin
   val logging = HttpLoggingInterceptor().apply {
       level = HttpLoggingInterceptor.Level.BODY
   }
   ```

3. **效能分析**:
   - 使用 Android Studio Profiler
   - 監控記憶體使用和 CPU 佔用

## 版本更新策略

### 1. 漸進式發布

1. **內測版本** (10% 使用者)
2. **公測版本** (50% 使用者)  
3. **正式發布** (100% 使用者)

### 2. 向後相容

```kotlin
// API 版本檢查
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    // 使用新的權限模型
} else {
    // 使用舊的權限模型
}
```

### 3. 功能開關

```kotlin
// 遠端配置功能開關
val isNewFeatureEnabled = FirebaseRemoteConfig.getInstance()
    .getBoolean("enable_emotion_detection")
```

---

這個部署指南提供了從開發到生產的完整部署流程，包括環境配置、建構發布、監控維護等各個方面，確保應用程式能夠穩定可靠地運行在生產環境中。
