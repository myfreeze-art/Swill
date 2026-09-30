package com.swill.vpn.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ServerPinger(private val context: Context) {

    sealed class PingResult {
        data class Success(val latency: Long, val method: String) : PingResult()
        data class Failure(val error: String, val method: String) : PingResult()
    }

    suspend fun pingServer(
        address: String,
        port: Int = 80,
        methods: List<String> = listOf(METHOD_HTTP, METHOD_TCP, METHOD_DNS, METHOD_ICMP),
        timeout: Int = 3000
    ): List<PingResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PingResult>()

        for (method in methods) {
            try {
                val result = when (method) {
                    METHOD_ICMP -> pingIcmp(address, timeout)
                    METHOD_TCP -> pingTcp(address, port, timeout)
                    METHOD_HTTP -> pingHttp(address, port, timeout)
                    METHOD_DNS -> pingDns(address, timeout)
                    else -> PingResult.Failure("Unknown method: $method", method)
                }
                results.add(result)
            } catch (e: Exception) {
                results.add(PingResult.Failure(e.message ?: "Unknown error", method))
            }
        }

        results
    }

    private fun pingIcmp(host: String, timeout: Int): PingResult {
        return try {
            val inetAddress = InetAddress.getByName(host)
            val startTime = System.currentTimeMillis()

            val isReachable = inetAddress.isReachable(timeout)
            val latency = System.currentTimeMillis() - startTime

            if (isReachable) {
                PingResult.Success(latency, METHOD_ICMP)
            } else {
                PingResult.Failure("Host unreachable", METHOD_ICMP)
            }
        } catch (e: IOException) {
            PingResult.Failure(e.message ?: "ICMP ping failed", METHOD_ICMP)
        }
    }

    private fun pingTcp(host: String, port: Int, timeout: Int): PingResult {
        return try {
            val startTime = System.currentTimeMillis()

            val socket = Socket()
            socket.connect(InetAddress.getByName(host).let { InetAddress.getByName(it.hostAddress) }, port, timeout)
            val latency = System.currentTimeMillis() - startTime

            socket.close()
            PingResult.Success(latency, METHOD_TCP)
        } catch (e: Exception) {
            PingResult.Failure(e.message ?: "TCP ping failed", METHOD_TCP)
        }
    }

    private fun pingHttp(host: String, port: Int, timeout: Int): PingResult {
        return try {
            val startTime = System.currentTimeMillis()
            val url = if (port == 443) "https://$host" else "http://$host:$port"

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.connect()

            val responseCode = connection.responseCode
            val latency = System.currentTimeMillis() - startTime
            connection.disconnect()

            if (responseCode in 200..399) {
                PingResult.Success(latency, METHOD_HTTP)
            } else {
                PingResult.Failure("HTTP $responseCode", METHOD_HTTP)
            }
        } catch (e: Exception) {
            PingResult.Failure(e.message ?: "HTTP ping failed", METHOD_HTTP)
        }
    }

    private fun pingDns(host: String, timeout: Int): PingResult {
        return try {
            val startTime = System.currentTimeMillis()

            val socket = DatagramSocket()
            socket.soTimeout = timeout

            val dnsQuery = buildDnsQuery(host)
            val dnsServer = InetAddress.getByName("8.8.8.8")

            val packet = DatagramPacket(dnsQuery, dnsQuery.size, dnsServer, 53)
            socket.send(packet)

            val responseBuffer = ByteArray(1024)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)

            val latency = System.currentTimeMillis() - startTime
            socket.close()

            PingResult.Success(latency, METHOD_DNS)
        } catch (e: Exception) {
            PingResult.Failure(e.message ?: "DNS ping failed", METHOD_DNS)
        }
    }

    private fun buildDnsQuery(hostname: String): ByteArray {
        val randomId = (Math.random() * 65535).toInt()
        val query = ByteArrayOutputStream()

        query.writeShort(randomId)
        query.writeShort(0x0100)
        query.writeShort(1)
        query.writeShort(1)
        query.writeShort(0)
        query.writeShort(0)
        query.writeShort(0)

        val parts = hostname.split(".")
        for (part in parts) {
            query.writeByte(part.length)
            query.write(part.toByteArray())
        }

        query.writeByte(0)
        query.writeShort(1)
        query.writeShort(1)

        return query.toByteArray()
    }

    companion object {
        const val METHOD_ICMP = "icmp"
        const val METHOD_TCP = "tcp"
        const val METHOD_HTTP = "http"
        const val METHOD_DNS = "dns"

        const val TAG = "ServerPinger"
    }
}

class ByteArrayOutputStream : java.io.ByteArrayOutputStream() {
    fun writeShort(value: Int) {
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }

    fun writeByte(value: Int) {
        write(value and 0xFF)
    }
}
