package com.swill.vpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.swill.vpn.R
import com.swill.vpn.jni.SingBoxNative
import com.swill.vpn.jni.XrayNative
import com.swill.vpn.model.VpnConfig
import com.swill.vpn.ui.MainActivity

class VpnService : VpnService() {
    
    companion object {
        private const val TAG = "VpnService"
        const val ACTION_START = "com.swill.vpn.ACTION_START"
        const val ACTION_STOP = "com.swill.vpn.ACTION_STOP"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_CORE_TYPE = "core_type"
        const val NOTIFICATION_CHANNEL_ID = "vpn_service_channel"
        const val NOTIFICATION_ID = 1001
        
        // Core types
        const val CORE_XRAY = "xray"
        const val CORE_SINGBOX = "singbox"
    }
    
    // Binder for client-server communication
    private val binder = VpnBinder()
    
    // Core instances
    private lateinit var xrayNative: XrayNative
    private lateinit var singBoxNative: SingBoxNative
    
    // Current state
    private var currentConfig: VpnConfig? = null
    private var currentCoreType: String = CORE_XRAY
    private var isRunning: Boolean = false
    private var vpnFileDescriptor: ParcelFileDescriptor? = null
    
    // Notification
    private var notificationManager: NotificationManager? = null
    
    inner class VpnBinder : Binder() {
        fun getService(): VpnService = this@VpnService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VpnService created")
        
        // Initialize native cores
        xrayNative = XrayNative(this)
        singBoxNative = SingBoxNative(this)
        
        // Create notification channel
        createNotificationChannel()
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification("Swill VPN", "Ready to connect"))
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VpnService destroyed")
        stopVpn()
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_START -> {
                val config = intent.getParcelableExtra<VpnConfig>(EXTRA_CONFIG)
                val coreType = intent.getStringExtra(EXTRA_CORE_TYPE) ?: CORE_XRAY
                
                config?.let { startVpn(it, coreType) }
            }
            ACTION_STOP -> {
                stopVpn()
            }
        }
    }
    
    fun startVpn(config: VpnConfig, coreType: String = CORE_XRAY): Boolean {
        Log.d(TAG, "Starting VPN with config: ${config.name}, core: $coreType")
        
        if (isRunning) {
            Log.w(TAG, "VPN is already running")
            return false
        }
        
        currentConfig = config
        currentCoreType = coreType
        
        try {
            // Setup VPN
            val builder = Builder()
            
            // Configure based on protocol
            when (config.protocol) {
                "vless", "vmess", "trojan" -> {
                    builder.setSession(config.name)
                        .addAddress("172.19.0.1", 24)
                        .addDnsServer("8.8.8.8")
                        .addDnsServer("1.1.1.1")
                        .setMtu(1500)
                }
                else -> {
                    builder.setSession(config.name)
                        .addAddress("172.19.0.1", 24)
                        .addDnsServer("8.8.8.8")
                }
            }
            
            // Allow all traffic
            builder.addDisallowedApplication(packageName)
            
            // Establish VPN
            vpnFileDescriptor = builder.establish()
            
            // Start the selected core
            val configJson = config.toJson()
            val started = when (coreType) {
                CORE_XRAY -> xrayNative.startVpn(configJson)
                CORE_SINGBOX -> singBoxNative.startVpn(configJson)
                else -> false
            }
            
            if (started) {
                isRunning = true
                updateNotification("Swill VPN", "Connected: ${config.name}")
                Log.d(TAG, "VPN started successfully")
                return true
            } else {
                vpnFileDescriptor?.close()
                vpnFileDescriptor = null
                Log.e(TAG, "Failed to start VPN core")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            vpnFileDescriptor?.close()
            vpnFileDescriptor = null
            return false
        }
    }
    
    fun stopVpn(): Boolean {
        Log.d(TAG, "Stopping VPN")
        
        if (!isRunning) {
            Log.w(TAG, "VPN is not running")
            return false
        }
        
        try {
            // Stop the current core
            when (currentCoreType) {
                CORE_XRAY -> xrayNative.stopVpn()
                CORE_SINGBOX -> singBoxNative.stopVpn()
            }
            
            // Close VPN file descriptor
            vpnFileDescriptor?.close()
            vpnFileDescriptor = null
            
            isRunning = false
            currentConfig = null
            
            updateNotification("Swill VPN", "Disconnected")
            Log.d(TAG, "VPN stopped successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop VPN", e)
            return false
        }
    }
    
    fun getStatus(): VpnStatus {
        return VpnStatus(
            isRunning = isRunning,
            currentConfig = currentConfig,
            currentCore = currentCoreType
        )
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Swill VPN Service"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            
            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(title: String, text: String) {
        notificationManager?.notify(NOTIFICATION_ID, createNotification(title, text))
    }
    
    data class VpnStatus(
        val isRunning: Boolean,
        val currentConfig: VpnConfig?,
        val currentCore: String
    )
}
