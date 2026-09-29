package com.swill.vpn.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class VpnConfig(
    val id: String = System.currentTimeMillis().toString(),
    val name: String = "Untitled",
    val serverAddress: String = "",
    val serverPort: Int = 0,
    val protocol: String = "vless",
    val uuid: String = "",
    val alterId: Int? = null,
    val security: String? = null,
    val network: String? = null,
    val headerType: String? = null,
    val requestHost: String? = null,
    val path: String? = null,
    val sni: String? = null,
    val allowInsecure: Boolean = false,
    val coreType: String = "xray"
) : Parcelable {
    
    // Protocol constants
    companion object {
        const val PROTOCOL_VLESS = "vless"
        const val PROTOCOL_VMESS = "vmess"
        const val PROTOCOL_TROJAN = "trojan"
        const val PROTOCOL_SHADOWSOCKS = "shadowsocks"
        const val PROTOCOL_WIREGUARD = "wireguard"
        
        const val NETWORK_TCP = "tcp"
        const val NETWORK_WS = "ws"
        const val NETWORK_GRPC = "grpc"
        const val NETWORK_KCP = "kcp"
        const val NETWORK_QUIC = "quic"
        
        const val SECURITY_NONE = "none"
        const val SECURITY_TLS = "tls"
        const val SECURITY_REALITY = "reality"
    }
    
    fun toJson(): String {
        val json = JSONObject()
        
        when (protocol) {
            PROTOCOL_VLESS -> {
                json.put("inbounds", JSONObject().apply {
                    put("port", 1080)
                    put("protocol", "socks")
                    put("settings", JSONObject().apply {
                        put("auth", "noauth")
                        put("udp", true)
                    })
                    put("sniffing", JSONObject().apply {
                        put("enabled", true)
                        put("destOverride", listOf("http", "tls"))
                    })
                })
                
                json.put("outbounds", listOf(JSONObject().apply {
                    put("protocol", "vless")
                    put("settings", JSONObject().apply {
                        put("vnext", listOf(JSONObject().apply {
                            put("address", serverAddress)
                            put("port", serverPort)
                            put("users", listOf(JSONObject().apply {
                                put("id", uuid)
                                put("alterId", alterId ?: 0)
                                put("security", security ?: SECURITY_NONE)
                            }))
                        }))
                    })
                    
                    // Add stream settings based on network
                    if (network != null) {
                        put("streamSettings", JSONObject().apply {
                            put("network", network)
                            
                            when (network) {
                                NETWORK_WS -> {
                                    put("wsSettings", JSONObject().apply {
                                        headerType?.let { put("headers", JSONObject().apply {
                                            put("Host", requestHost ?: "")
                                        }) }
                                        path?.let { put("path", it) }
                                    })
                                }
                                NETWORK_GRPC -> {
                                    put("grpcSettings", JSONObject().apply {
                                        put("serviceName", path ?: "")
                                    })
                                }
                                NETWORK_TCP -> {
                                    requestHost?.let { 
                                        put("tcpSettings", JSONObject().apply {
                                            put("header", JSONObject().apply {
                                                put("type", headerType ?: "none")
                                                put("request", JSONObject().apply {
                                                    put("host", listOf(it))
                                                })
                                            })
                                        })
                                    }
                                }
                                NETWORK_KCP -> {
                                    put("kcpSettings", JSONObject().apply {
                                        put("mtu", 1350)
                                        put("tti", 50)
                                        put("uplinkCapacity", 5)
                                        put("downlinkCapacity", 20)
                                        put("congestion", false)
                                        put("readBufferSize", 2)
                                        put("writeBufferSize", 2)
                                    })
                                }
                                NETWORK_QUIC -> {
                                    put("quicSettings", JSONObject().apply {
                                        put("security", security ?: SECURITY_TLS)
                                        requestHost?.let { put("key", it) }
                                    })
                                }
                            }
                            
                            // Add TLS/SNI settings
                            if (security == SECURITY_TLS || security == SECURITY_REALITY) {
                                put("security", security)
                                put("tlsSettings", JSONObject().apply {
                                    sni?.let { put("serverName", it) }
                                    put("allowInsecure", allowInsecure)
                                })
                            }
                        })
                    }
                }))
            }
            
            PROTOCOL_VMESS -> {
                json.put("inbounds", JSONObject().apply {
                    put("port", 1080)
                    put("protocol", "socks")
                    put("settings", JSONObject().apply {
                        put("auth", "noauth")
                        put("udp", true)
                    })
                })
                
                json.put("outbounds", listOf(JSONObject().apply {
                    put("protocol", "vmess")
                    put("settings", JSONObject().apply {
                        put("vnext", listOf(JSONObject().apply {
                            put("address", serverAddress)
                            put("port", serverPort)
                            put("users", listOf(JSONObject().apply {
                                put("id", uuid)
                                put("alterId", alterId ?: 0)
                                put("security", security ?: "auto")
                            }))
                        }))
                    })
                    
                    if (network != null) {
                        put("streamSettings", JSONObject().apply {
                            put("network", network)
                            // Similar stream settings as VLESS
                        })
                    }
                }))
            }
            
            PROTOCOL_TROJAN -> {
                json.put("inbounds", JSONObject().apply {
                    put("port", 1080)
                    put("protocol", "socks")
                    put("settings", JSONObject().apply {
                        put("auth", "noauth")
                        put("udp", true)
                    })
                })
                
                json.put("outbounds", listOf(JSONObject().apply {
                    put("protocol", "trojan")
                    put("settings", JSONObject().apply {
                        put("servers", listOf(JSONObject().apply {
                            put("address", serverAddress)
                            put("port", serverPort)
                            put("password", uuid)
                        }))
                    })
                    
                    if (network != null) {
                        put("streamSettings", JSONObject().apply {
                            put("network", network)
                            put("security", security ?: SECURITY_TLS)
                            sni?.let { 
                                put("tlsSettings", JSONObject().apply {
                                    put("serverName", it)
                                })
                            }
                        })
                    }
                }))
            }
            
            PROTOCOL_SHADOWSOCKS -> {
                json.put("inbounds", JSONObject().apply {
                    put("port", 1080)
                    put("protocol", "socks")
                    put("settings", JSONObject().apply {
                        put("auth", "noauth")
                        put("udp", true)
                    })
                })
                
                json.put("outbounds", listOf(JSONObject().apply {
                    put("protocol", "shadowsocks")
                    put("settings", JSONObject().apply {
                        put("servers", listOf(JSONObject().apply {
                            put("address", serverAddress)
                            put("port", serverPort)
                            put("password", uuid)
                            put("method", security ?: "aes-256-gcm")
                        }))
                    })
                }))
            }
            
            else -> {
                // Default configuration
                json.put("inbounds", JSONObject().apply {
                    put("port", 1080)
                    put("protocol", "socks")
                    put("settings", JSONObject().apply {
                        put("auth", "noauth")
                        put("udp", true)
                    })
                })
                
                json.put("outbounds", listOf(JSONObject().apply {
                    put("protocol", "freedom")
                }))
            }
        }
        
        // Add routing for Sing-box compatibility
        if (coreType == "singbox") {
            json.put("route", JSONObject().apply {
                put("rules", listOf(
                    JSONObject().apply {
                        put("protocol", "dns")
                        put("outbound", "dns-out")
                    },
                    JSONObject().apply {
                        put("geoip", listOf("private"))
                        put("outbound", "direct")
                    },
                    JSONObject().apply {
                        put("geoip", listOf("cn"))
                        put("outbound", "direct")
                    }
                ))
                put("auto_detect_interface", true)
            })
            
            json.put("dns", JSONObject().apply {
                put("servers", listOf(
                    JSONObject().apply {
                        put("address", "8.8.8.8")
                        put("detour", "direct")
                    },
                    JSONObject().apply {
                        put("address", "1.1.1.1")
                        put("detour", "direct")
                    }
                ))
            })
        }
        
        return json.toString()
    }
    
    fun toSingBoxJson(): String {
        // Sing-box specific configuration
        val json = JSONObject()
        
        json.put("log", JSONObject().apply {
            put("level", "info")
            put("output", "/dev/null")
        })
        
        json.put("dns", JSONObject().apply {
            put("servers", listOf(
                JSONObject().apply {
                    put("address", "8.8.8.8")
                    put("detour", "direct")
                }
            ))
        })
        
        json.put("inbounds", listOf(JSONObject().apply {
            put("type", "tun")
            put("tag", "tun-in")
            put("interface_name", "swill_tun")
            put("mtu", 1500)
            put("stack", "system")
            put("endpoint_independent_nat", true)
        }))
        
        json.put("outbounds", listOf(
            JSONObject().apply {
                put("type", protocol)
                put("tag", "proxy")
                
                when (protocol) {
                    PROTOCOL_VLESS -> {
                        put("server", serverAddress)
                        put("server_port", serverPort)
                        put("uuid", uuid)
                        put("flow", "xtls-rprx-vision")
                    }
                    PROTOCOL_VMESS -> {
                        put("server", serverAddress)
                        put("server_port", serverPort)
                        put("uuid", uuid)
                        alterId?.let { put("alter_id", it) }
                    }
                    PROTOCOL_TROJAN -> {
                        put("server", serverAddress)
                        put("server_port", serverPort)
                        put("password", uuid)
                    }
                    PROTOCOL_SHADOWSOCKS -> {
                        put("server", serverAddress)
                        put("server_port", serverPort)
                        put("password", uuid)
                        put("method", security ?: "aes-256-gcm")
                    }
                }
                
                // Add transport layer settings
                if (network != null) {
                    put("transport", JSONObject().apply {
                        put("type", network)
                        when (network) {
                            NETWORK_WS -> {
                                put("path", path ?: "/")
                                requestHost?.let { put("headers", JSONObject().apply {
                                    put("Host", it)
                                }) }
                            }
                            NETWORK_GRPC -> {
                                put("service_name", path ?: "")
                            }
                        }
                    })
                }
                
                // Add TLS settings
                if (security == SECURITY_TLS || security == SECURITY_REALITY) {
                    put("tls", JSONObject().apply {
                        put("enabled", true)
                        sni?.let { put("server_name", it) }
                        put("insecure", allowInsecure)
                    })
                }
            },
            JSONObject().apply {
                put("type", "direct")
                put("tag", "direct")
            },
            JSONObject().apply {
                put("type", "block")
                put("tag", "block")
            },
            JSONObject().apply {
                put("type", "dns")
                put("tag", "dns-out")
            }
        ))
        
        json.put("route", JSONObject().apply {
            put("rules", listOf(
                JSONObject().apply {
                    put("protocol", "dns")
                    put("outbound", "dns-out")
                },
                JSONObject().apply {
                    put("geoip", listOf("private"))
                    put("outbound", "direct")
                }
            ))
            put("auto_detect_interface", true)
        })
        
        return json.toString()
    }
}
