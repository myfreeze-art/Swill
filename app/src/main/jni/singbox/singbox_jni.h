// Sing-box JNI header
#ifndef SINGBOX_JNI_H
#define SINGBOX_JNI_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// JNI functions for Sing-box core
JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_start(
    JNIEnv* env,
    jobject thiz,
    jstring config_json
);

JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_stop(
    JNIEnv* env,
    jobject thiz
);

JNIEXPORT jint JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_getStatus(
    JNIEnv* env,
    jobject thiz
);

JNIEXPORT jboolean JNICALL 
Java_com_swill_vpn_jni_SingBoxNative_isRunning(
    JNIEnv* env,
    jobject thiz
);

#ifdef __cplusplus
}
#endif

#endif // SINGBOX_JNI_H
