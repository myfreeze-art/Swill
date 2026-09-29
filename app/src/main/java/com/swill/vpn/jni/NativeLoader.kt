package com.swill.vpn.jni

import android.content.Context
import android.util.Log

object NativeLoader {
    
    private const val TAG = "NativeLoader"
    
    // Native library names
    private const val LIB_NATIVE = "native-lib"
    private const val LIB_XRAY = "xray-jni"
    private const val LIB_SINGBOX = "singbox-jni"
    
    // Track loaded libraries
    private val loadedLibraries = mutableSetOf<String>()
    
    fun load(context: Context) {
        try {
            // Load main native library
            System.loadLibrary(LIB_NATIVE)
            loadedLibraries.add(LIB_NATIVE)
            Log.d(TAG, "Loaded: $LIB_NATIVE")
            
            // Try to load X-ray library
            try {
                System.loadLibrary(LIB_XRAY)
                loadedLibraries.add(LIB_XRAY)
                Log.d(TAG, "Loaded: $LIB_XRAY")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Failed to load $LIB_XRAY: ${e.message}")
            }
            
            // Try to load Sing-box library
            try {
                System.loadLibrary(LIB_SINGBOX)
                loadedLibraries.add(LIB_SINGBOX)
                Log.d(TAG, "Loaded: $LIB_SINGBOX")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Failed to load $LIB_SINGBOX: ${e.message}")
            }
            
            Log.d(TAG, "Native libraries loaded: $loadedLibraries")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load native libraries", e)
        }
    }
    
    fun isLoaded(libName: String): Boolean {
        return loadedLibraries.contains(libName)
    }
    
    fun getLoadedLibraries(): Set<String> {
        return loadedLibraries.toSet()
    }
}
