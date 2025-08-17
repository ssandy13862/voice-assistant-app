#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "audio_processor.h"
#include "vad/vad.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static AudioProcessor* g_audio_processor = nullptr;
static VAD* g_vad = nullptr;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_voiceassistant_app_data_repository_WhisperNativeRepository_initWhisper(
    JNIEnv *env, jobject thiz, jstring model_path) {
    
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("初始化 Whisper 模型: %s", path);
    
    if (g_audio_processor) {
        delete g_audio_processor;
    }
    
    g_audio_processor = new AudioProcessor();
    bool success = g_audio_processor->initWhisper(path);
    
    env->ReleaseStringUTFChars(model_path, path);
    
    if (success) {
        LOGI("Whisper 模型初始化成功");
        return reinterpret_cast<jlong>(g_audio_processor);
    } else {
        LOGE("Whisper 模型初始化失敗");
        delete g_audio_processor;
        g_audio_processor = nullptr;
        return 0;
    }
}

JNIEXPORT jlong JNICALL
Java_com_voiceassistant_app_data_repository_WhisperNativeRepository_initVAD(
    JNIEnv *env, jobject thiz, jint sample_rate, jint frame_length) {
    
    LOGI("初始化 VAD: sample_rate=%d, frame_length=%d", sample_rate, frame_length);
    
    if (g_vad) {
        delete g_vad;
    }
    
    g_vad = new VAD(sample_rate, frame_length);
    bool success = g_vad->init();
    
    if (success) {
        LOGI("VAD 初始化成功");
        return reinterpret_cast<jlong>(g_vad);
    } else {
        LOGE("VAD 初始化失敗");
        delete g_vad;
        g_vad = nullptr;
        return 0;
    }
}

JNIEXPORT jstring JNICALL
Java_com_voiceassistant_app_data_repository_WhisperNativeRepository_nativeTranscribeAudio(
    JNIEnv *env, jobject thiz, jfloatArray audio_data) {
    
    if (!g_audio_processor) {
        LOGE("Whisper 未初始化");
        return env->NewStringUTF("");
    }
    
    jsize length = env->GetArrayLength(audio_data);
    jfloat* audio_buffer = env->GetFloatArrayElements(audio_data, nullptr);
    
    LOGI("開始轉錄音頻，長度: %d", length);
    
    std::vector<float> audio_vector(audio_buffer, audio_buffer + length);
    std::string result = g_audio_processor->transcribe(audio_vector);
    
    env->ReleaseFloatArrayElements(audio_data, audio_buffer, JNI_ABORT);
    
    LOGI("轉錄結果: %s", result.c_str());
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_voiceassistant_app_data_repository_WhisperNativeRepository_nativeDetectVoiceActivity(
    JNIEnv *env, jobject thiz, jfloatArray audio_frame) {
    
    if (!g_vad) {
        LOGE("VAD 未初始化");
        return JNI_FALSE;
    }
    
    jsize length = env->GetArrayLength(audio_frame);
    jfloat* frame_buffer = env->GetFloatArrayElements(audio_frame, nullptr);
    
    std::vector<float> frame_vector(frame_buffer, frame_buffer + length);
    bool is_voice = g_vad->detect(frame_vector);
    
    env->ReleaseFloatArrayElements(audio_frame, frame_buffer, JNI_ABORT);
    
    return is_voice ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jfloat JNICALL
Java_com_voiceassistant_app_data_repository_WhisperNativeRepository_nativeGetVoiceProbability(
    JNIEnv *env, jobject thiz, jfloatArray audio_frame) {
    
    if (!g_vad) {
        LOGE("VAD 未初始化");
        return 0.0f;
    }
    
    jsize length = env->GetArrayLength(audio_frame);
    jfloat* frame_buffer = env->GetFloatArrayElements(audio_frame, nullptr);
    
    std::vector<float> frame_vector(frame_buffer, frame_buffer + length);
    float probability = g_vad->getVoiceProbability(frame_vector);
    
    env->ReleaseFloatArrayElements(audio_frame, frame_buffer, JNI_ABORT);
    
    return probability;
}

JNIEXPORT void JNICALL
Java_com_voiceassistant_app_data_repository_WhisperNativeRepository_nativeCleanup(
    JNIEnv *env, jobject thiz) {
    
    LOGI("清理本地資源");
    
    if (g_audio_processor) {
        delete g_audio_processor;
        g_audio_processor = nullptr;
    }
    
    if (g_vad) {
        delete g_vad;
        g_vad = nullptr;
    }
}

} // extern "C"
