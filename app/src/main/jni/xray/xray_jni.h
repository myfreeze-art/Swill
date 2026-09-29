#ifndef XRAY_JNI_H
#define XRAY_JNI_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_com_swill_vpn_jni_XrayNative_start(JNIEnv*, jobject, jstring, jstring);
JNIEXPORT jint JNICALL Java_com_swill_vpn_jni_XrayNative_stop(JNIEnv*, jobject);
JNIEXPORT jint JNICALL Java_com_swill_vpn_jni_XrayNative_getStatus(JNIEnv*, jobject);
JNIEXPORT jboolean JNICALL Java_com_swill_vpn_jni_XrayNative_isRunning(JNIEnv*, jobject);

#ifdef __cplusplus
}
#endif

#endif
