package com.vigipro.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CameraPermission(
    val id: String,
    @SerialName("site_member_id") val siteMemberId: String,
    @SerialName("camera_id") val cameraId: String,
    @SerialName("can_view_live") val canViewLive: Boolean = true,
    @SerialName("can_playback") val canPlayback: Boolean = false,
    @SerialName("can_ptz") val canPtz: Boolean = false,
    @SerialName("can_audio") val canAudio: Boolean = false,
    @SerialName("can_export") val canExport: Boolean = false,
    @SerialName("time_start") val timeStart: String? = null,
    @SerialName("time_end") val timeEnd: String? = null,
    @SerialName("days_of_week") val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
)
