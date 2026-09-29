package com.swill.vpn.core

import android.content.Context
import com.swill.vpn.jni.SingBoxNative
import com.swill.vpn.jni.XrayNative
import com.swill.vpn.model.VpnConfig
import com.swill.vpn.vpn.VpnService

class VpnCoreManager(private val context: Context) {

    private val xrayNative: XrayNative = XrayNative(context)
    private val singBoxNative: SingBoxNative = SingBoxNative(context)

    private var currentCore: String = VpnService.CORE_XRAY

    fun startVpn(config: VpnConfig, coreType: String): Boolean {
        currentCore = coreType
        val binaryPath = AssetExtractor.getBinaryPath(context, if (coreType == VpnService.CORE_XRAY) "xray" else "sing-box")

        if (binaryPath == null) {
            AssetExtractor.extractAsset(context, if (coreType == VpnService.CORE_XRAY) "xray" else "sing-box", 
                if (coreType == VpnService.CORE_XRAY) "xray" else "sing-box")
        }

        val finalBinaryPath = AssetExtractor.getBinaryPath(context, if (coreType == VpnService.CORE_XRAY) "xray" else "sing-box") ?: return false
        val configJson = if (coreType == VpnService.CORE_SINGBOX) config.toSingBoxJson() else config.toJson()

        return when (coreType) {
            VpnService.CORE_XRAY -> xrayNative.startVpn(configJson, finalBinaryPath)
            VpnService.CORE_SINGBOX -> singBoxNative.startVpn(configJson, finalBinaryPath)
            else -> xrayNative.startVpn(configJson, finalBinaryPath)
        }
    }

    fun stopVpn(): Boolean {
        return when (currentCore) {
            VpnService.CORE_XRAY -> xrayNative.stopVpn()
            VpnService.CORE_SINGBOX -> singBoxNative.stopVpn()
            else -> xrayNative.stopVpn()
        }
    }

    fun isRunning(): Boolean {
        return when (currentCore) {
            VpnService.CORE_XRAY -> xrayNative.isVpnRunning()
            VpnService.CORE_SINGBOX -> singBoxNative.isVpnRunning()
            else -> xrayNative.isVpnRunning()
        }
    }

    fun getStatus(): Int {
        return when (currentCore) {
            VpnService.CORE_XRAY -> xrayNative.getCurrentStatus()
            VpnService.CORE_SINGBOX -> singBoxNative.getCurrentStatus()
            else -> xrayNative.getCurrentStatus()
        }
    }

    fun validateConfig(config: VpnConfig, coreType: String): Boolean {
        val configJson = if (coreType == VpnService.CORE_SINGBOX) config.toSingBoxJson() else config.toJson()
        return when (coreType) {
            VpnService.CORE_XRAY -> xrayNative.validateConfig(configJson)
            VpnService.CORE_SINGBOX -> singBoxNative.validateConfig(configJson)
            else -> xrayNative.validateConfig(configJson)
        }
    }
}
