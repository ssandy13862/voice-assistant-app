# 部署指南

## 环境准备

### 开发环境

1. **Android Studio**
   - 版本: Arctic Fox (2020.3.1) 或更高
   - JDK: Oracle JDK 8 或 OpenJDK 8+

2. **Android SDK**
   - Min SDK: API 24 (Android 7.0)
   - Target SDK: API 34 (Android 14)
   - 所需工具: SDK Build Tools 30.0.3+

3. **设备要求**
   - 前置摄像头支持
   - 麦克风权限
   - 网络连接能力

## API 服务配置

### 1. OpenAI API 配置

1. 获取 OpenAI API Key:
   - 访问 https://platform.openai.com/
   - 注册账户并获取 API Key

2. 配置 API Key:
   ```kotlin
   // 在 SpeechRepositoryImpl.kt 中
   private val openAiApiKey = "sk-your-api-key-here"
   ```

   **生产环境建议**:
   ```kotlin
   // 使用环境变量或安全存储
   private val openAiApiKey = BuildConfig.OPENAI_API_KEY
   ```

### 2. Whisper API 部署

#### 方案一: 使用 Docker 部署

1. **拉取 Whisper 镜像**:
   ```bash
   docker pull openai/whisper
   ```

2. **运行服务**:
   ```bash
   docker run -d -p 8000:8000 \
     --name whisper-api \
     openai/whisper:latest
   ```

3. **配置应用端点**:
   ```kotlin
   // 在 NetworkModule.kt 中
   .baseUrl("http://your-server-ip:8000/")
   ```

#### 方案二: 使用云服务部署

1. **部署到云平台** (AWS, GCP, Azure)
2. **配置负载均衡和自动扩展**
3. **设置HTTPS和域名**

### 3. VAD 模型配置

#### 当前实现 (简化VAD)
- 基于音频能量检测
- 无需额外配置

#### 推荐升级 (Silero VAD)

1. **下载模型文件**:
   ```bash
   wget https://github.com/snakers4/silero-vad/releases/download/v3.1/silero_vad.onnx
   ```

2. **集成到应用**:
   ```kotlin
   // 将模型文件放置到 assets 目录
   app/src/main/assets/silero_vad.onnx
   ```

## 构建配置

### 1. Gradle 配置

#### 开发环境 (debug)
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

#### 生产环境 (release)
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

### 2. 签名配置

1. **生成密钥库**:
   ```bash
   keytool -genkey -v -keystore voice-assistant.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias voice-assistant-key
   ```

2. **配置签名**:
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

## 构建和发布

### 1. 本地构建

```bash
# 清理项目
./gradlew clean

# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 运行测试
./gradlew test

# 生成 APK
./gradlew assembleRelease
```

### 2. CI/CD 配置

#### GitHub Actions 示例

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

### 3. 应用商店发布

#### Google Play Store

1. **准备发布包**:
   ```bash
   ./gradlew bundleRelease
   ```

2. **上传 AAB 文件**:
   - 文件位置: `app/build/outputs/bundle/release/app-release.aab`

3. **应用信息配置**:
   - 应用名称: "智慧语音助理"
   - 包名: `com.voiceassistant.app`
   - 版本号: 遵循语义化版本

4. **权限说明**:
   - 相机权限: 用于人脸检测触发语音助理
   - 麦克风权限: 用于语音输入和语音活动检测
   - 网络权限: 用于AI对话和语音识别服务

## 监控和维护

### 1. 应用性能监控

#### Firebase Performance Monitoring

1. **添加依赖**:
   ```gradle
   implementation 'com.google.firebase:firebase-perf-ktx:20.4.1'
   ```

2. **配置监控**:
   ```kotlin
   // 监控关键方法
   val trace = FirebasePerformance.startTrace("face_detection")
   // ... 人脸检测逻辑
   trace.stop()
   ```

### 2. 错误监控

#### Firebase Crashlytics

1. **添加依赖**:
   ```gradle
   implementation 'com.google.firebase:firebase-crashlytics-ktx:18.4.3'
   ```

2. **错误报告**:
   ```kotlin
   try {
       // 语音处理逻辑
   } catch (e: Exception) {
       FirebaseCrashlytics.getInstance().recordException(e)
       // 错误处理
   }
   ```

### 3. 用户分析

#### Firebase Analytics

1. **事件追踪**:
   ```kotlin
   // 记录关键事件
   firebaseAnalytics.logEvent("voice_interaction_started", Bundle().apply {
       putString("trigger_type", "face_detection")
   })
   ```

## 安全考虑

### 1. API Key 保护

```kotlin
// 不要硬编码在代码中
❌ private val apiKey = "sk-1234567890"

// 使用 BuildConfig
✅ private val apiKey = BuildConfig.OPENAI_API_KEY

// 或使用加密存储
✅ private val apiKey = EncryptedSharedPreferences.getString("api_key", "")
```

### 2. 网络安全

```kotlin
// 配置网络安全
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

### 3. 数据保护

```kotlin
// 敏感数据加密存储
val encryptedPrefs = EncryptedSharedPreferences.create(
    "secure_prefs",
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

## 故障排除

### 常见问题

1. **相机权限被拒绝**:
   - 检查 `AndroidManifest.xml` 权限声明
   - 确保运行时权限请求代码正确

2. **网络连接失败**:
   - 检查网络权限
   - 验证 API 端点 URL
   - 检查防火墙和代理设置

3. **人脸检测不工作**:
   - 确保设备有前置摄像头
   - 检查光线条件
   - 验证 ML Kit 依赖是否正确添加

4. **TTS 无声音**:
   - 检查设备音量设置
   - 验证 TTS 引擎是否可用
   - 检查语言设置

### 调试工具

1. **日志查看**:
   ```bash
   adb logcat -s VoiceAssistant
   ```

2. **网络调试**:
   ```kotlin
   val logging = HttpLoggingInterceptor().apply {
       level = HttpLoggingInterceptor.Level.BODY
   }
   ```

3. **性能分析**:
   - 使用 Android Studio Profiler
   - 监控内存使用和 CPU 占用

## 版本更新策略

### 1. 渐进式发布

1. **内测版本** (10% 用户)
2. **公测版本** (50% 用户)  
3. **正式发布** (100% 用户)

### 2. 向后兼容

```kotlin
// API 版本检查
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    // 使用新的权限模型
} else {
    // 使用旧的权限模型
}
```

### 3. 功能开关

```kotlin
// 远程配置功能开关
val isNewFeatureEnabled = FirebaseRemoteConfig.getInstance()
    .getBoolean("enable_emotion_detection")
```

---

这个部署指南提供了从开发到生产的完整部署流程，包括环境配置、构建发布、监控维护等各个方面，确保应用能够稳定可靠地运行在生产环境中。
