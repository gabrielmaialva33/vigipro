package com.vigipro.core.network.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Alert Request DTOs ---

@Serializable
data class AlertRequest(
    @SerialName("userId") val userId: String,
    @SerialName("siteId") val siteId: String? = null,
    @SerialName("cameraName") val cameraName: String,
    @SerialName("alertType") val alertType: String,
    val message: String? = null,
    @SerialName("fcmToken") val fcmToken: String? = null,
)

@Serializable
data class BroadcastRequest(
    @SerialName("siteId") val siteId: String,
    @SerialName("cameraName") val cameraName: String? = null,
    @SerialName("alertType") val alertType: String,
    val message: String? = null,
)

// --- Alert Response DTOs ---

@Serializable
data class AlertResponse(
    val status: String,
    val published: Boolean = false,
)

@Serializable
data class BroadcastResponse(
    val status: String,
    val broadcast: Boolean = false,
    val topic: String? = null,
)

@Serializable
data class AlertHealthResponse(
    val status: String,
    val uptime: Double? = null,
    val timestamp: String? = null,
)
