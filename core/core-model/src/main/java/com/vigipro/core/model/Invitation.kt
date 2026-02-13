package com.vigipro.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Invitation(
    val id: String,
    @SerialName("site_id") val siteId: String,
    @SerialName("invite_code") val inviteCode: String,
    val role: UserRole,
    @SerialName("camera_ids") val cameraIds: List<String>? = null,
    @SerialName("time_start") val timeStart: String? = null,
    @SerialName("time_end") val timeEnd: String? = null,
    @SerialName("days_of_week") val daysOfWeek: List<Int>? = null,
    @SerialName("max_uses") val maxUses: Int = 1,
    @SerialName("uses_count") val usesCount: Int = 0,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("created_by") val createdBy: String? = null,
)
