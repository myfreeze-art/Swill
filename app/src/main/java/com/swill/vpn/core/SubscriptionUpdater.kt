package com.swill.vpn.core

import android.content.Context
import android.util.Log
import com.swill.vpn.model.Subscription
import com.swill.vpn.model.VpnConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SubscriptionUpdater(private val context: Context) {

    sealed class UpdateResult {
        data class Success(val configs: List<VpnConfig>, val count: Int) : UpdateResult()
        data class Failure(val error: String) : UpdateResult()
        data class Empty(val message: String) : UpdateResult()
    }

    suspend fun updateSubscription(subscription: Subscription): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(subscription.url)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "SwillVPN/1.0")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateResult.Failure("HTTP error: $responseCode")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()

            val configs = parseSubscriptionResponse(response, subscription.url)
            if (configs.isEmpty()) {
                return@withContext UpdateResult.Empty("No valid configs found in subscription")
            }

            UpdateResult.Success(configs, configs.size)
        } catch (e: Exception) {
            UpdateResult.Failure(e.message ?: "Unknown error")
        }
    }

    private fun parseSubscriptionResponse(content: String, sourceUrl: String): List<VpnConfig> {
        val configs = mutableListOf<VpnConfig>()

        try {
            if (content.startsWith("{") || content.startsWith("[")) {
                configs.addAll(parseJsonSubscription(content))
            } else {
                configs.addAll(parseTextSubscription(content))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing subscription: ${e.message}")
        }

        return configs
    }

    private fun parseJsonSubscription(content: String): List<VpnConfig> {
        val configs = mutableListOf<VpnConfig>()

        try {
            val json = JSONObject(content)
            if (json.has("servers") && json.get("servers") is List<*>) {
                val servers = json.getJSONArray("servers")
                for (i in 0 until servers.length()) {
                    val serverJson = servers.getJSONObject(i)
                    val config = parseServerJson(serverJson)
                    config?.let { configs.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON subscription: ${e.message}")
        }

        return configs
    }

    private fun parseTextSubscription(content: String): List<VpnConfig> {
        val configs = mutableListOf<VpnConfig>()
        val lines = content.split("\n")

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            when {
                trimmedLine.startsWith("vmess://") -> {
                    parseVmessUri(trimmedLine)?.let { configs.add(it) }
                }
                trimmedLine.startsWith("vless://") -> {
                    parseVlessUri(trimmedLine)?.let { configs.add(it) }
                }
                trimmedLine.startsWith("trojan://") -> {
                    parseTrojanUri(trimmedLine)?.let { configs.add(it) }
                }
                trimmedLine.startsWith("ss://") -> {
                    parseShadowsocksUri(trimmedLine)?.let { configs.add(it) }
                }
            }
        }

        return configs
    }

    private fun parseServerJson(serverJson: JSONObject): VpnConfig? {
        return try {
            val protocol = serverJson.optString("protocol", "vless")
            val address = serverJson.optString("address", "")
            val port = serverJson.optInt("port", 0)
            val name = serverJson.optString("name", "Untitled")
            val uuid = serverJson.optString("uuid", "")
            val alterId = serverJson.optInt("alterId", 0).takeIf { it != 0 }
            val security = serverJson.optString("security", null)
            val network = serverJson.optString("network", null)
            val path = serverJson.optString("path", null)
            val host = serverJson.optString("host", null)
            val sni = serverJson.optString("sni", null)
            val allowInsecure = serverJson.optBoolean("allowInsecure", false)

            VpnConfig(
                name = name,
                serverAddress = address,
                serverPort = port,
                protocol = protocol,
                uuid = uuid,
                alterId = alterId,
                security = security,
                network = network,
                requestHost = host,
                path = path,
                sni = sni,
                allowInsecure = allowInsecure
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server JSON: ${e.message}")
            null
        }
    }

    private fun parseVmessUri(uri: String): VpnConfig? {
        return try {
            val encoded = uri.substringAfter("vmess://")
            val decoded = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
            val parts = String(decoded).split("&")

            var address = ""
            var port = 0
            var uuid = ""
            var alterId = 0
            var security = "auto"
            var network = ""
            var path = ""
            var host = ""
            var sni = ""

            for (part in parts) {
                when {
                    part.startsWith("add=") -> address = part.substringAfter("add=")
                    part.startsWith("port=") -> port = part.substringAfter("port=").toIntOrNull() ?: 0
                    part.startsWith("id=") -> uuid = part.substringAfter("id=")
                    part.startsWith("aid=") -> alterId = part.substringAfter("aid=").toIntOrNull() ?: 0
                    part.startsWith("scy=") -> security = part.substringAfter("scy=")
                    part.startsWith("net=") -> network = part.substringAfter("net=")
                    part.startsWith("path=") -> path = part.substringAfter("path=")
                    part.startsWith("host=") -> host = part.substringAfter("host=")
                    part.startsWith("sni=") -> sni = part.substringAfter("sni=")
                }
            }

            VpnConfig(
                name = address,
                serverAddress = address,
                serverPort = port,
                protocol = VpnConfig.PROTOCOL_VMESS,
                uuid = uuid,
                alterId = alterId,
                security = security,
                network = network,
                requestHost = host,
                path = path,
                sni = sni
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing VMESS URI: ${e.message}")
            null
        }
    }

    private fun parseVlessUri(uri: String): VpnConfig? {
        return try {
            val encoded = uri.substringAfter("vless://")
            val decoded = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
            val parts = String(decoded).split("&")

            var address = ""
            var port = 0
            var uuid = ""
            var security = "none"
            var network = ""
            var path = ""
            var host = ""
            var sni = ""
            var flow = ""

            for (part in parts) {
                when {
                    part.startsWith("add=") -> address = part.substringAfter("add=")
                    part.startsWith("port=") -> port = part.substringAfter("port=").toIntOrNull() ?: 0
                    part.startsWith("id=") -> uuid = part.substringAfter("id=")
                    part.startsWith("scy=") -> security = part.substringAfter("scy=")
                    part.startsWith("net=") -> network = part.substringAfter("net=")
                    part.startsWith("path=") -> path = part.substringAfter("path=")
                    part.startsWith("host=") -> host = part.substringAfter("host=")
                    part.startsWith("sni=") -> sni = part.substringAfter("sni=")
                    part.startsWith("flow=") -> flow = part.substringAfter("flow=")
                }
            }

            VpnConfig(
                name = address,
                serverAddress = address,
                serverPort = port,
                protocol = VpnConfig.PROTOCOL_VLESS,
                uuid = uuid,
                security = security,
                network = network,
                requestHost = host,
                path = path,
                sni = sni
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing VLESS URI: ${e.message}")
            null
        }
    }

    private fun parseTrojanUri(uri: String): VpnConfig? {
        return try {
            val encoded = uri.substringAfter("trojan://")
            val decoded = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
            val parts = String(decoded).split("&")

            var address = ""
            var port = 0
            var password = ""
            var security = "tls"
            var host = ""
            var sni = ""

            for (part in parts) {
                when {
                    part.startsWith("add=") -> address = part.substringAfter("add=")
                    part.startsWith("port=") -> port = part.substringAfter("port=").toIntOrNull() ?: 0
                    part.startsWith("password=") -> password = part.substringAfter("password=")
                    part.startsWith("scy=") -> security = part.substringAfter("scy=")
                    part.startsWith("host=") -> host = part.substringAfter("host=")
                    part.startsWith("sni=") -> sni = part.substringAfter("sni=")
                }
            }

            VpnConfig(
                name = address,
                serverAddress = address,
                serverPort = port,
                protocol = VpnConfig.PROTOCOL_TROJAN,
                uuid = password,
                security = security,
                requestHost = host,
                sni = sni
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Trojan URI: ${e.message}")
            null
        }
    }

    private fun parseShadowsocksUri(uri: String): VpnConfig? {
        return try {
            val encoded = uri.substringAfter("ss://")
            val decoded = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
            val parts = String(decoded).split("&")

            var address = ""
            var port = 0
            var password = ""
            var method = "aes-256-gcm"

            for (part in parts) {
                when {
                    part.startsWith("add=") -> address = part.substringAfter("add=")
                    part.startsWith("port=") -> port = part.substringAfter("port=").toIntOrNull() ?: 0
                    part.startsWith("password=") -> password = part.substringAfter("password=")
                    part.startsWith("method=") -> method = part.substringAfter("method=")
                }
            }

            VpnConfig(
                name = address,
                serverAddress = address,
                serverPort = port,
                protocol = VpnConfig.PROTOCOL_SHADOWSOCKS,
                uuid = password,
                security = method
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Shadowsocks URI: ${e.message}")
            null
        }
    }

    companion object {
        const val TAG = "SubscriptionUpdater"
    }
}
