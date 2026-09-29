package com.swill.vpn.jni

import android.content.Context
import android.util.Log

class SingBoxNative(private val context: Context) {
    
    companion object {
        private const val TAG = "SingBoxNative"
        
        init {
            System.loadLibrary("singbox-jni")
        }
    }
    
    // Native methods
    private external fun start(configJson: String): Int
    private external fun stop(): Int
    private external fun getStatus(): Int
    private external fun isRunning(): Boolean
    
    // Status constants
    companion object {
        const val STATUS_STOPPED = 0
        const val STATUS_RUNNING = 1
        const val STATUS_ERROR = 2
    }
    
    private var currentConfig: String = ""
    
    fun startVpn(configJson: String): Boolean {
        Log.d(TAG, "Starting Sing-box with config")
        currentConfig = configJson
        
        val result = start(configJson)
        if (result == 0) {
            Log.d(TAG, "Sing-box started successfully")
            return true
        } else {
            Log.e(TAG, "Failed to start Sing-box, error code: $result")
            return false
        }
    }
    
    fun stopVpn(): Boolean {
        Log.d(TAG, "Stopping Sing-box")
        val result = stop()
        if (result == 0) {
            Log.d(TAG, "Sing-box stopped successfully")
            currentConfig = ""
            return true
        } else {
            Log.e(TAG, "Failed to stop Sing-box, error code: $result")
            return false
        }
    }
    
    fun getCurrentStatus(): Int {
        return getStatus()
    }
    
    fun isVpnRunning(): Boolean {
        return isRunning()
    }
    
    fun getCurrentConfig(): String {
        return currentConfig
    }
    
    // Additional utility methods
    fun validateConfig(configJson: String): Boolean {
        // Basic JSON validation
        return configJson.isNotEmpty() && 
               configJson.contains("\"inbounds\"") &&
               configJson.contains("\"outbounds\"")
    }
}
