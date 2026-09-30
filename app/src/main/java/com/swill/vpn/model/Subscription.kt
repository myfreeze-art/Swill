package com.swill.vpn.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Subscription(
    val id: String = System.currentTimeMillis().toString(),
    val name: String = "Untitled",
    val url: String = "",
    val lastUpdated: Long = 0,
    val autoUpdate: Boolean = false,
    val updateInterval: Long = 24 * 60 * 60 * 1000
) : Parcelable
