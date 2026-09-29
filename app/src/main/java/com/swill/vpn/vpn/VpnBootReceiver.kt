package com.swill.vpn.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class VpnBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_QUICKBOOT_POWERON -> {

                val prefs = context.getSharedPreferences("SwillPrefs", Context.MODE_PRIVATE)
                val autoConnect = prefs.getBoolean("auto_connect_on_boot", false)

                if (autoConnect) {
                    val lastConfigJson = prefs.getString("last_config", null)
                    val lastCore = prefs.getString("last_core", VpnService.CORE_XRAY)

                    if (!lastConfigJson.isNullOrEmpty()) {
                        val serviceIntent = Intent(context, VpnService::class.java).apply {
                            action = VpnService.ACTION_START
                            putExtra(VpnService.EXTRA_CORE_TYPE, lastCore)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                }
            }
        }
    }
}
