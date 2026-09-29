package com.swill.vpn.jni

object NativeUtils {

    init {
        System.loadLibrary("native-lib")
    }

    external fun getVersion(): String

    fun getNativeVersion(): String = try {
        getVersion()
    } catch (_: Exception) {
        "unknown"
    }

    fun areNativeLibrariesAvailable(): Boolean = try {
        getVersion()
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }
}
