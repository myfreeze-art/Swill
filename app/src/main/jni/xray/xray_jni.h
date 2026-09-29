// X-ray JNI header
#ifndef XRAY_JNI_H
#define XRAY_JNI_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// JNI functions for X-ray core
JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_start(
    JNIEnv* env,
    jobject thiz,
    jstring config_json
);

JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_stop(
    JNIEnv* env,
    jobject thiz
);

JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_XrayNative_getStatus(
    JNIEnv* env,
    jobject thiz
);

JNIEXPORT jboolean JNICALL 
Java_com_swill_vpn_jni_XrayNative_isRunning(
    JNIEnv* env,
    jobject thiz
);

#ifdef __cplusplus
}
#endif

#endif // XRAY_JNI_H
