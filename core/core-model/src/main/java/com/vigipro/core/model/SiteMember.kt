package com.vigipro.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SiteMember(
    val id: String,
    @SerialName("site_id") val siteId: String,
    @SerialName("user_id") val userId: String,
    val role: UserRole,
    @SerialName("invited_by") val invitedBy: String? = null,
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_until") val validUntil: String? = null,
)

@Serializable
enum class UserRole {
    @SerialName("owner") OWNER,
    @SerialName("admin") ADMIN,
    @SerialName("viewer") VIEWER,
    @SerialName("time_restricted") TIME_RESTRICTED,
    @SerialName("guest") GUEST,
}
