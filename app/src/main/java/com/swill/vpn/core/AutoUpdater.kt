package com.swill.vpn.core

import android.content.Context
import com.swill.vpn.config.AppConfig
import com.swill.vpn.model.VpnConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoUpdater(private val context: Context) {

    private val appConfig: AppConfig = AppConfig(context)
    private val subscriptionUpdater: SubscriptionUpdater = SubscriptionUpdater(context)
    private val serverPinger: ServerPinger = ServerPinger(context)

    fun updateSubscriptionsOnStartup() {
        if (!appConfig.isAutoUpdateSubscriptions()) return

        CoroutineScope(Dispatchers.Main).launch {
            val subscriptions = withContext(Dispatchers.IO) { appConfig.getAllSubscriptions() }

            for (subscription in subscriptions.filter { it.autoUpdate }) {
                val result = withContext(Dispatchers.IO) {
                    subscriptionUpdater.updateSubscription(subscription)
                }

                when (result) {
                    is SubscriptionUpdater.UpdateResult.Success -> {
                        for (config in result.configs) {
                            appConfig.saveConfig(config)
                        }
                        val updatedSubscription = subscription.copy(lastUpdated = System.currentTimeMillis())
                        appConfig.saveSubscription(updatedSubscription)
                    }
                    else -> {}
                }
            }
        }
    }

    fun pingServersOnStartup(
        servers: List<VpnConfig>,
        onComplete: (List<VpnConfig>) -> Unit
    ) {
        if (!appConfig.isAutoPingServers()) {
            onComplete(servers)
            return
        }

        val pingMethod = appConfig.getPingMethod()

        CoroutineScope(Dispatchers.Main).launch {
            val updatedServers = mutableListOf<VpnConfig>()

            for (server in servers) {
                val result = withContext(Dispatchers.IO) {
                    serverPinger.pingServer(
                        server.serverAddress,
                        server.serverPort,
                        listOf(pingMethod)
                    ).firstOrNull()
                }

                updatedServers.add(server.copy(pingResult = result))
            }

            onComplete(updatedServers)
        }
    }

    suspend fun updateAllSubscriptions(): Int = withContext(Dispatchers.IO) {
        val subscriptions = appConfig.getAllSubscriptions()
        var totalImported = 0

        for (subscription in subscriptions) {
            val result = subscriptionUpdater.updateSubscription(subscription)
            when (result) {
                is SubscriptionUpdater.UpdateResult.Success -> {
                    for (config in result.configs) {
                        appConfig.saveConfig(config)
                    }
                    totalImported += result.count
                    val updatedSubscription = subscription.copy(lastUpdated = System.currentTimeMillis())
                    appConfig.saveSubscription(updatedSubscription)
                }
                else -> {}
            }
        }

        totalImported
    }

    suspend fun pingAllServers(servers: List<VpnConfig>): List<VpnConfig> = withContext(Dispatchers.IO) {
        val pingMethod = appConfig.getPingMethod()
        val updatedServers = mutableListOf<VpnConfig>()

        for (server in servers) {
            val result = serverPinger.pingServer(
                server.serverAddress,
                server.serverPort,
                listOf(pingMethod)
            ).firstOrNull()

            updatedServers.add(server.copy(pingResult = result))
        }

        updatedServers
    }
}
