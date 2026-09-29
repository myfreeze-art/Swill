package com.swill.vpn.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class VpnBootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "VpnBootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received boot completed intent")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_QUICKBOOT_POWERON -> {
                
                // Check if auto-connect is enabled
                val prefs = context.getSharedPreferences("SwillPrefs", Context.MODE_PRIVATE)
                val autoConnect = prefs.getBoolean("auto_connect_on_boot", false)
                
                if (autoConnect) {
                    Log.d(TAG, "Auto-connect is enabled, starting VPN")
                    
                    // Get last used config
                    val lastConfigJson = prefs.getString("last_config", null)
                    val lastCore = prefs.getString("last_core", VpnService.CORE_XRAY)
                    
                    if (!lastConfigJson.isNullOrEmpty()) {
                        // For simplicity, we'll just start the service
                        // The actual config loading should be handled by the service
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
