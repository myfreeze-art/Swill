package com.swill.vpn.jni

import android.content.Context

class XrayNative(private val context: Context) {

    init {
        System.loadLibrary("xray-jni")
    }

    private external fun start(configJson: String, binaryPath: String): Int
    private external fun stop(): Int
    private external fun getStatus(): Int
    private external fun isRunning(): Boolean

    companion object {
        const val STATUS_STOPPED = 0
        const val STATUS_RUNNING = 1
        const val STATUS_ERROR = 2
    }

    fun startVpn(configJson: String, binaryPath: String): Boolean {
        return start(configJson, binaryPath) == 0
    }

    fun stopVpn(): Boolean = stop() == 0

    fun getCurrentStatus(): Int = getStatus()

    fun isVpnRunning(): Boolean = isRunning()

    fun validateConfig(configJson: String): Boolean =
        configJson.isNotEmpty() &&
        configJson.contains("\"inbounds\"") &&
        configJson.contains("\"outbounds\"")
}
