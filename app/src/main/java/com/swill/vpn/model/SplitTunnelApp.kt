package com.swill.vpn.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SplitTunnelApp(
    val packageName: String,
    val appName: String,
    val enabled: Boolean = true,
    val routeThroughVpn: Boolean = true
) : Parcelable
