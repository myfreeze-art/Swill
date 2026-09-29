package com.swill.vpn.config

import android.content.Context
import android.content.SharedPreferences
import com.swill.vpn.model.VpnConfig
import com.swill.vpn.vpn.VpnService

class AppConfig(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "SwillPrefs"
        private const val KEY_LAST_CONFIG = "last_config"
        private const val KEY_LAST_CORE = "last_core"
        private const val KEY_AUTO_CONNECT = "auto_connect_on_boot"
        private const val KEY_DEFAULT_CORE = "default_core"
        private const val KEY_CONFIGS_LIST = "configs_list"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Save last used configuration
    fun saveLastConfig(config: VpnConfig, coreType: String) {
        prefs.edit().apply {
            putString(KEY_LAST_CONFIG, config.id)
            putString(KEY_LAST_CORE, coreType)
            apply()
        }
    }
    
    // Get last used configuration
    fun getLastConfig(): Pair<VpnConfig?, String> {
        val configId = prefs.getString(KEY_LAST_CONFIG, null)
        val coreType = prefs.getString(KEY_LAST_CORE, VpnService.CORE_XRAY)
        
        configId?.let {
            val config = getConfigById(it)
            return Pair(config, coreType ?: VpnService.CORE_XRAY)
        }
        
        return Pair(null, coreType ?: VpnService.CORE_XRAY)
    }
    
    // Save a configuration
    fun saveConfig(config: VpnConfig): Boolean {
        val configs = getAllConfigs().toMutableList()
        
        // Check if config with same ID exists
        val existingIndex = configs.indexOfFirst { it.id == config.id }
        
        if (existingIndex >= 0) {
            configs[existingIndex] = config
        } else {
            configs.add(config)
        }
        
        // Save to preferences (simple approach, for many configs use Room database)
        val configsJson = configs.joinToString(",") { it.id }
        prefs.edit().putString(KEY_CONFIGS_LIST, configsJson).apply()
        
        // Save individual config (in production, use proper serialization)
        saveConfigToStorage(config)
        
        return true
    }
    
    // Get configuration by ID
    fun getConfigById(id: String): VpnConfig? {
        return loadConfigFromStorage(id)
    }
    
    // Get all configurations
    fun getAllConfigs(): List<VpnConfig> {
        val configsJson = prefs.getString(KEY_CONFIGS_LIST, "")
        val configIds = configsJson?.split(",") ?: emptyList()
        
        return configIds.mapNotNull { loadConfigFromStorage(it) }
    }
    
    // Delete configuration
    fun deleteConfig(id: String): Boolean {
        val configs = getAllConfigs().toMutableList()
        configs.removeAll { it.id == id }
        
        val configsJson = configs.joinToString(",") { it.id }
        prefs.edit().putString(KEY_CONFIGS_LIST, configsJson).apply()
        
        // Delete from storage
        deleteConfigFromStorage(id)
        
        return true
    }
    
    // Auto-connect settings
    fun setAutoConnectOnBoot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply()
    }
    
    fun isAutoConnectOnBoot(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CONNECT, false)
    }
    
    // Default core settings
    fun setDefaultCore(coreType: String) {
        prefs.edit().putString(KEY_DEFAULT_CORE, coreType).apply()
    }
    
    fun getDefaultCore(): String {
        return prefs.getString(KEY_DEFAULT_CORE, VpnService.CORE_XRAY) ?: VpnService.CORE_XRAY
    }
    
    // Private methods for storage
    private fun saveConfigToStorage(config: VpnConfig) {
        val prefs = context.getSharedPreferences("config_${config.id}", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("name", config.name)
            putString("serverAddress", config.serverAddress)
            putInt("serverPort", config.serverPort)
            putString("protocol", config.protocol)
            putString("uuid", config.uuid)
            putInt("alterId", config.alterId ?: 0)
            putString("security", config.security)
            putString("network", config.network)
            putString("headerType", config.headerType)
            putString("requestHost", config.requestHost)
            putString("path", config.path)
            putString("sni", config.sni)
            putBoolean("allowInsecure", config.allowInsecure)
            putString("coreType", config.coreType)
            apply()
        }
    }
    
    private fun loadConfigFromStorage(id: String): VpnConfig? {
        val prefs = context.getSharedPreferences("config_$id", Context.MODE_PRIVATE)
        
        return try {
            VpnConfig(
                id = id,
                name = prefs.getString("name", "Untitled") ?: "Untitled",
                serverAddress = prefs.getString("serverAddress", "") ?: "",
                serverPort = prefs.getInt("serverPort", 0),
                protocol = prefs.getString("protocol", "vless") ?: "vless",
                uuid = prefs.getString("uuid", "") ?: "",
                alterId = if (prefs.contains("alterId")) prefs.getInt("alterId", 0) else null,
                security = prefs.getString("security", null),
                network = prefs.getString("network", null),
                headerType = prefs.getString("headerType", null),
                requestHost = prefs.getString("requestHost", null),
                path = prefs.getString("path", null),
                sni = prefs.getString("sni", null),
                allowInsecure = prefs.getBoolean("allowInsecure", false),
                coreType = prefs.getString("coreType", VpnService.CORE_XRAY) ?: VpnService.CORE_XRAY
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun deleteConfigFromStorage(id: String) {
        val prefs = context.getSharedPreferences("config_$id", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
