#ifndef SINGBOX_JNI_H
#define SINGBOX_JNI_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_com_swill_vpn_jni_SingBoxNative_start(JNIEnv*, jobject, jstring, jstring);
JNIEXPORT jint JNICALL Java_com_swill_vpn_jni_SingBoxNative_stop(JNIEnv*, jobject);
JNIEXPORT jint JNICALL Java_com_swill_vpn_jni_SingBoxNative_getStatus(JNIEnv*, jobject);
JNIEXPORT jboolean JNICALL Java_com_swill_vpn_jni_SingBoxNative_isRunning(JNIEnv*, jobject);

#ifdef __cplusplus
}
#endif

#endif
