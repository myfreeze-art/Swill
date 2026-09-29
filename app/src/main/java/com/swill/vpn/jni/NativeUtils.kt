package com.swill.vpn.jni

import android.util.Log

object NativeUtils {
    
    private const val TAG = "NativeUtils"
    
    init {
        System.loadLibrary("native-lib")
    }
    
    external fun getVersion(): String
    
    fun getNativeVersion(): String {
        return try {
            getVersion()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get native version", e)
            "unknown"
        }
    }
    
    // Check if native libraries are available
    fun areNativeLibrariesAvailable(): Boolean {
        return try {
            getVersion()
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }
}
