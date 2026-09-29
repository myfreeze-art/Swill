// Native library entry point
// This is the main JNI library that will be loaded by Android

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "SwillNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// X-ray JNI functions (will be implemented in xray_jni.cpp)
extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_start(
    JNIEnv* env,
    jobject thiz,
    jstring config_json
);

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_stop(
    JNIEnv* env,
    jobject thiz
);

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_getStatus(
    JNIEnv* env,
    jobject thiz
);

// Sing-box JNI functions (will be implemented in singbox_jni.cpp)
extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_start(
    JNIEnv* env,
    jobject thiz,
    jstring config_json
);

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_stop(
    JNIEnv* env,
    jobject thiz
);

extern "C" JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_getStatus(
    JNIEnv* env,
    jobject thiz
);

// Utility function
extern "C" JNIEXPORT jstring JNICALL 
Java_com_swill_vpn_jni_NativeUtils_getVersion(
    JNIEnv* env,
    jobject thiz
) {
    return env->NewStringUTF("1.0.0");
}

// JNI onLoad function
jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return JNI_ERR;
    }
    
    LOGD("Swill Native Library loaded successfully");
    return JNI_VERSION_1_6;
}
