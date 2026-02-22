package com.vigipro.core.network.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Camera DTOs ---

@Serializable
data class CloudCameraDto(
    val id: String,
    val name: String,
    @SerialName("site_id") val siteId: String,
    @SerialName("ptz_capable") val ptzCapable: Boolean = false,
    @SerialName("audio_capable") val audioCapable: Boolean = false,
    val status: String = "OFFLINE",
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("onvif_address") val onvifAddress: String? = null,
    @SerialName("stream_profile") val streamProfile: String? = null,
    // NOTE: rtsp_url and username are NEVER sent to cloud
)

@Serializable
data class CloudCamerasResponse(
    val cameras: List<CloudCameraDto>,
)

@Serializable
data class CloudCameraWrapper(
    val camera: CloudCameraDto,
)

// --- Event DTOs ---

@Serializable
data class CloudEventDto(
    @SerialName("camera_id") val cameraId: String,
    @SerialName("camera_name") val cameraName: String,
    @SerialName("event_type") val eventType: String,
    val message: String? = null,
    val timestamp: Long? = null,
)

@Serializable
data class CloudEventResponse(
    val id: Long,
    @SerialName("camera_id") val cameraId: String,
    @SerialName("camera_name") val cameraName: String,
    @SerialName("event_type") val eventType: String,
    val message: String? = null,
    val timestamp: Long,
)

@Serializable
data class CloudEventsResponse(
    val events: List<CloudEventResponse>,
)

@Serializable
data class CloudEventWrapper(
    val event: CloudEventResponse,
)

@Serializable
data class CloudBatchResponse(
    val logged: Int,
    val status: String,
)

// --- Sync DTOs ---

@Serializable
data class CloudSyncResponse(
    val synced: Int,
    val status: String,
)

// --- Health / Status ---

@Serializable
data class CloudHealthResponse(
    val status: String,
    val version: String,
    val node: String? = null,
    @SerialName("uptime_seconds") val uptimeSeconds: Long? = null,
)

@Serializable
data class CloudStatusResponse(
    val status: String,
)
