package com.vigipro.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Camera(
    val id: String,
    @SerialName("site_id") val siteId: String,
    val name: String,
    @SerialName("onvif_address") val onvifAddress: String? = null,
    @SerialName("rtsp_url") val rtspUrl: String? = null,
    val username: String? = null,
    @SerialName("stream_profile") val streamProfile: String? = null,
    @SerialName("ptz_capable") val ptzCapable: Boolean = false,
    @SerialName("audio_capable") val audioCapable: Boolean = false,
    val status: CameraStatus = CameraStatus.OFFLINE,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
enum class CameraStatus {
    @SerialName("online") ONLINE,
    @SerialName("offline") OFFLINE,
    @SerialName("error") ERROR,
}
