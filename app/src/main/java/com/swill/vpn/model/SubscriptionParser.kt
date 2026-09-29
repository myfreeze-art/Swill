package com.swill.vpn.model

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

object SubscriptionParser {

    fun parseSubscription(url: String): List<VpnConfig> {
        return try {
            val decoded = decodeSubscriptionUrl(url)
            parseSubscriptionContent(decoded)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseSubscriptionContent(content: String): List<VpnConfig> {
        return try {
            if (content.startsWith("[") && content.endsWith("]")) {
                parseJsonArray(content)
            } else if (content.startsWith("{") && content.endsWith("}")) {
                parseVmessJson(content)
            } else {
                parseVmessContent(content)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun decodeSubscriptionUrl(url: String): String {
        val base64Content = if (url.contains("://")) {
            val connection = URL(url).openConnection()
            connection.connect()
            val input = connection.getInputStream()
            val bytes = input.readBytes()
            input.close()
            String(bytes)
        } else {
            url
        }

        return if (base64Content.startsWith("vmess://")) {
            val encoded = base64Content.substringAfter("vmess://")
            String(Base64.decode(encoded, Base64.DEFAULT))
        } else {
            base64Content
        }
    }

    private fun parseJsonArray(content: String): List<VpnConfig> {
        val jsonArray = JSONArray(content)
        val configs = mutableListOf<VpnConfig>()

        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            val config = parseServerJson(json)
            config?.let { configs.add(it) }
        }

        return configs
    }

    private fun parseVmessJson(content: String): List<VpnConfig> {
        val json = JSONObject(content)
        val configs = mutableListOf<VpnConfig>()

        if (json.has("servers")) {
            val servers = json.getJSONArray("servers")
            for (i in 0 until servers.length()) {
                val serverJson = servers.getJSONObject(i)
                val config = parseServerJson(serverJson)
                config?.let { configs.add(it) }
            }
        }

        return configs
    }

    private fun parseVmessContent(content: String): List<VpnConfig> {
        val configs = mutableListOf<VpnConfig>()
        val lines = content.split("\n")

        for (line in lines) {
            if (line.isBlank()) continue

            if (line.startsWith("vmess://")) {
                val encoded = line.substringAfter("vmess://")
                val decoded = String(Base64.decode(encoded, Base64.DEFAULT))
                val json = JSONObject(decoded)
                val config = parseVmessJson(json)
                config?.let { configs.add(it) }
            } else if (line.startsWith("vless://")) {
                val encoded = line.substringAfter("vless://")
                val decoded = String(Base64.decode(encoded, Base64.DEFAULT))
                val json = JSONObject(decoded)
                val config = parseVlessJson(json)
                config?.let { configs.add(it) }
            } else if (line.startsWith("trojan://")) {
                val encoded = line.substringAfter("trojan://")
                val decoded = String(Base64.decode(encoded, Base64.DEFAULT))
                val json = JSONObject(decoded)
                val config = parseTrojanJson(json)
                config?.let { configs.add(it) }
            } else if (line.startsWith("ss://")) {
                val encoded = line.substringAfter("ss://")
                val decoded = String(Base64.decode(encoded, Base64.DEFAULT))
                val config = parseShadowsocksJson(decoded)
                config?.let { configs.add(it) }
            }
        }

        return configs
    }

    private fun parseServerJson(json: JSONObject): VpnConfig? {
        return try {
            when (json.optString("type", "").lowercase()) {
                "vmess" -> parseVmessJson(json)
                "vless" -> parseVlessJson(json)
                "trojan" -> parseTrojanJson(json)
                "shadowsocks" -> parseShadowsocksJson(json)
                "ss" -> parseShadowsocksJson(json)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseVmessJson(json: JSONObject): VpnConfig? {
        return try {
            VpnConfig(
                name = json.optString("remarks", "VMESS Server"),
                serverAddress = json.optString("add", ""),
                serverPort = json.optInt("port", 0),
                protocol = VpnConfig.PROTOCOL_VMESS,
                uuid = json.optString("id", ""),
                alterId = json.optInt("aid", 0),
                security = json.optString("security", "auto"),
                network = json.optString("net", VpnConfig.NETWORK_TCP),
                headerType = json.optString("type", "none"),
                requestHost = json.optString("host", null),
                path = json.optString("path", null),
                sni = json.optString("sni", null),
                allowInsecure = json.optBoolean("allowInsecure", false),
                coreType = json.optString("core", VpnService.CORE_XRAY)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseVlessJson(json: JSONObject): VpnConfig? {
        return try {
            VpnConfig(
                name = json.optString("remarks", "VLESS Server"),
                serverAddress = json.optString("add", ""),
                serverPort = json.optInt("port", 0),
                protocol = VpnConfig.PROTOCOL_VLESS,
                uuid = json.optString("id", ""),
                alterId = json.optInt("aid", 0),
                security = json.optString("security", VpnConfig.SECURITY_NONE),
                network = json.optString("net", VpnConfig.NETWORK_TCP),
                headerType = json.optString("type", "none"),
                requestHost = json.optString("host", null),
                path = json.optString("path", null),
                sni = json.optString("sni", null),
                allowInsecure = json.optBoolean("allowInsecure", false),
                coreType = json.optString("core", VpnService.CORE_XRAY)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTrojanJson(json: JSONObject): VpnConfig? {
        return try {
            VpnConfig(
                name = json.optString("remarks", "Trojan Server"),
                serverAddress = json.optString("add", ""),
                serverPort = json.optInt("port", 0),
                protocol = VpnConfig.PROTOCOL_TROJAN,
                uuid = json.optString("password", ""),
                security = json.optString("security", VpnConfig.SECURITY_TLS),
                network = json.optString("net", VpnConfig.NETWORK_TCP),
                headerType = json.optString("type", "none"),
                requestHost = json.optString("host", null),
                path = json.optString("path", null),
                sni = json.optString("sni", null),
                allowInsecure = json.optBoolean("allowInsecure", false),
                coreType = json.optString("core", VpnService.CORE_XRAY)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseShadowsocksJson(content: String): VpnConfig? {
        return try {
            val parts = content.split("@")
            if (parts.size < 2) return null

            val methodPassword = parts[0].split(":")
            if (methodPassword.size < 2) return null

            val addressPort = parts[1].split(":")
            if (addressPort.size < 2) return null

            VpnConfig(
                name = "Shadowsocks Server",
                serverAddress = addressPort[0],
                serverPort = addressPort[1].toIntOrNull() ?: 0,
                protocol = VpnConfig.PROTOCOL_SHADOWSOCKS,
                uuid = methodPassword[1],
                security = methodPassword[0],
                network = VpnConfig.NETWORK_TCP,
                allowInsecure = false,
                coreType = VpnService.CORE_XRAY
            )
        } catch (_: Exception) {
            null
        }
    }
}
