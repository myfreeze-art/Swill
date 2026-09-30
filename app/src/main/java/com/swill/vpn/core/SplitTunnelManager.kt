package com.swill.vpn.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.swill.vpn.model.SplitTunnelApp

class SplitTunnelManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun getInstalledApps(): List<SplitTunnelApp> {
        val apps = mutableListOf<SplitTunnelApp>()

        try {
            val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            for (packageInfo in packages) {
                if (packageInfo.applicationInfo != null) {
                    val appInfo = packageInfo.applicationInfo
                    if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                        val appName = appInfo.loadLabel(packageManager).toString()
                        apps.add(
                            SplitTunnelApp(
                                packageName = packageInfo.packageName,
                                appName = appName,
                                enabled = false,
                                routeThroughVpn = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed apps: ${e.message}")
        }

        return apps.sortedBy { it.appName }
    }

    fun getAppInfo(packageName: String): SplitTunnelApp? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            SplitTunnelApp(
                packageName = packageName,
                appName = appInfo.loadLabel(packageManager).toString(),
                enabled = false,
                routeThroughVpn = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app info: ${e.message}")
            null
        }
    }

    fun generateRoutingRules(
        apps: List<SplitTunnelApp>,
        mode: SplitTunnelMode
    ): Map<String, List<String>> {
        val vpnApps = mutableListOf<String>()
        val bypassApps = mutableListOf<String>()

        for (app in apps) {
            if (app.enabled) {
                if (app.routeThroughVpn) {
                    vpnApps.add(app.packageName)
                } else {
                    bypassApps.add(app.packageName)
                }
            }
        }

        return when (mode) {
            SplitTunnelMode.ALL_THROUGH_VPN -> {
                mapOf("vpn" to vpnApps, "bypass" to bypassApps)
            }
            SplitTunnelMode.ALL_BYPASS -> {
                mapOf("vpn" to bypassApps, "bypass" to vpnApps)
            }
            SplitTunnelMode.CUSTOM -> {
                mapOf("vpn" to vpnApps, "bypass" to bypassApps)
            }
        }
    }

    enum class SplitTunnelMode {
        ALL_THROUGH_VPN,
        ALL_BYPASS,
        CUSTOM
    }

    companion object {
        const val TAG = "SplitTunnelManager"
    }
}
