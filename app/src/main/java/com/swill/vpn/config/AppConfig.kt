package com.swill.vpn.config

import android.content.Context
import android.content.SharedPreferences
import com.swill.vpn.core.SplitTunnelManager
import com.swill.vpn.model.SplitTunnelApp
import com.swill.vpn.model.Subscription
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
        private const val KEY_SUBSCRIPTIONS_LIST = "subscriptions_list"
        private const val KEY_SPLIT_TUNNEL_MODE = "split_tunnel_mode"
        private const val KEY_SPLIT_TUNNEL_APPS = "split_tunnel_apps"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLastConfig(config: VpnConfig, coreType: String) {
        prefs.edit().apply {
            putString(KEY_LAST_CONFIG, config.id)
            putString(KEY_LAST_CORE, coreType)
            apply()
        }
    }

    fun getLastConfig(): Pair<VpnConfig?, String> {
        val configId = prefs.getString(KEY_LAST_CONFIG, null)
        val coreType = prefs.getString(KEY_LAST_CORE, VpnService.CORE_XRAY)

        configId?.let {
            val config = getConfigById(it)
            return Pair(config, coreType ?: VpnService.CORE_XRAY)
        }

        return Pair(null, coreType ?: VpnService.CORE_XRAY)
    }

    fun saveConfig(config: VpnConfig): Boolean {
        val configs = getAllConfigs().toMutableList()

        val existingIndex = configs.indexOfFirst { it.id == config.id }

        if (existingIndex >= 0) {
            configs[existingIndex] = config
        } else {
            configs.add(config)
        }

        val configsJson = configs.joinToString(",") { it.id }
        prefs.edit().putString(KEY_CONFIGS_LIST, configsJson).apply()

        saveConfigToStorage(config)

        return true
    }

    fun getConfigById(id: String): VpnConfig? = loadConfigFromStorage(id)

    fun getAllConfigs(): List<VpnConfig> {
        val configsJson = prefs.getString(KEY_CONFIGS_LIST, "")
        val configIds = configsJson?.split(",") ?: emptyList()

        return configIds.mapNotNull { loadConfigFromStorage(it) }
    }

    fun deleteConfig(id: String): Boolean {
        val configs = getAllConfigs().toMutableList()
        configs.removeAll { it.id == id }

        val configsJson = configs.joinToString(",") { it.id }
        prefs.edit().putString(KEY_CONFIGS_LIST, configsJson).apply()

        deleteConfigFromStorage(id)

        return true
    }

    fun setAutoConnectOnBoot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply()
    }

    fun isAutoConnectOnBoot(): Boolean = prefs.getBoolean(KEY_AUTO_CONNECT, false)

    fun setDefaultCore(coreType: String) {
        prefs.edit().putString(KEY_DEFAULT_CORE, coreType).apply()
    }

    fun getDefaultCore(): String = prefs.getString(KEY_DEFAULT_CORE, VpnService.CORE_XRAY) ?: VpnService.CORE_XRAY

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
            putString("hysteria2AuthPassword", config.hysteria2AuthPassword)
            putString("hysteria2Obfs", config.hysteria2Obfs)
            putString("hysteria2ObfsPassword", config.hysteria2ObfsPassword)
            putBoolean("bypassEnabled", config.bypassEnabled)
            putString("bypassDomains", config.bypassDomains)
            putString("bypassIps", config.bypassIps)
            putString("bypassGeoip", config.bypassGeoip)
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
                coreType = prefs.getString("coreType", VpnService.CORE_XRAY) ?: VpnService.CORE_XRAY,
                hysteria2AuthPassword = prefs.getString("hysteria2AuthPassword", null),
                hysteria2Obfs = prefs.getString("hysteria2Obfs", null),
                hysteria2ObfsPassword = prefs.getString("hysteria2ObfsPassword", null),
                bypassEnabled = prefs.getBoolean("bypassEnabled", false),
                bypassDomains = prefs.getString("bypassDomains", null),
                bypassIps = prefs.getString("bypassIps", null),
                bypassGeoip = prefs.getString("bypassGeoip", null)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteConfigFromStorage(id: String) {
        val prefs = context.getSharedPreferences("config_$id", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun saveSubscription(subscription: Subscription): Boolean {
        val subscriptions = getAllSubscriptions().toMutableList()

        val existingIndex = subscriptions.indexOfFirst { it.id == subscription.id }

        if (existingIndex >= 0) {
            subscriptions[existingIndex] = subscription
        } else {
            subscriptions.add(subscription)
        }

        val subscriptionsJson = subscriptions.joinToString(",") { it.id }
        prefs.edit().putString(KEY_SUBSCRIPTIONS_LIST, subscriptionsJson).apply()

        saveSubscriptionToStorage(subscription)

        return true
    }

    fun getAllSubscriptions(): List<Subscription> {
        val subscriptionsJson = prefs.getString(KEY_SUBSCRIPTIONS_LIST, "")
        val subscriptionIds = subscriptionsJson?.split(",") ?: emptyList()

        return subscriptionIds.mapNotNull { loadSubscriptionFromStorage(it) }
    }

    fun deleteSubscription(id: String): Boolean {
        val subscriptions = getAllSubscriptions().toMutableList()
        subscriptions.removeAll { it.id == id }

        val subscriptionsJson = subscriptions.joinToString(",") { it.id }
        prefs.edit().putString(KEY_SUBSCRIPTIONS_LIST, subscriptionsJson).apply()

        deleteSubscriptionFromStorage(id)

        return true
    }

    private fun saveSubscriptionToStorage(subscription: Subscription) {
        val prefs = context.getSharedPreferences("subscription_${subscription.id}", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("name", subscription.name)
            putString("url", subscription.url)
            putLong("lastUpdated", subscription.lastUpdated)
            putBoolean("autoUpdate", subscription.autoUpdate)
            putLong("updateInterval", subscription.updateInterval)
            apply()
        }
    }

    private fun loadSubscriptionFromStorage(id: String): Subscription? {
        val prefs = context.getSharedPreferences("subscription_$id", Context.MODE_PRIVATE)

        return try {
            Subscription(
                id = id,
                name = prefs.getString("name", "Untitled") ?: "Untitled",
                url = prefs.getString("url", "") ?: "",
                lastUpdated = prefs.getLong("lastUpdated", 0),
                autoUpdate = prefs.getBoolean("autoUpdate", false),
                updateInterval = prefs.getLong("updateInterval", 24 * 60 * 60 * 1000)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteSubscriptionFromStorage(id: String) {
        val prefs = context.getSharedPreferences("subscription_$id", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun setSplitTunnelMode(mode: String) {
        prefs.edit().putString(KEY_SPLIT_TUNNEL_MODE, mode).apply()
    }

    fun getSplitTunnelMode(): String = prefs.getString(KEY_SPLIT_TUNNEL_MODE, SplitTunnelManager.SplitTunnelMode.ALL_THROUGH_VPN.name) ?: SplitTunnelManager.SplitTunnelMode.ALL_THROUGH_VPN.name

    fun saveSplitTunnelApps(apps: List<SplitTunnelApp>) {
        val appsJson = apps.joinToString(";") { "${it.packageName},${it.appName},${it.enabled},${it.routeThroughVpn}" }
        prefs.edit().putString(KEY_SPLIT_TUNNEL_APPS, appsJson).apply()
    }

    fun getSplitTunnelApps(): List<SplitTunnelApp> {
        val appsJson = prefs.getString(KEY_SPLIT_TUNNEL_APPS, "")
        if (appsJson.isNullOrEmpty()) return emptyList()

        return appsJson.split(";").mapNotNull { part ->
            val parts = part.split(",")
            if (parts.size >= 4) {
                SplitTunnelApp(
                    packageName = parts[0],
                    appName = parts[1],
                    enabled = parts[2].toBoolean(),
                    routeThroughVpn = parts[3].toBoolean()
                )
            } else {
                null
            }
        }
    }
}
