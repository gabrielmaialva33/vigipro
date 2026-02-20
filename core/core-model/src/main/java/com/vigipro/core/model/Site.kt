package com.vigipro.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Site(
    val id: String,
    val name: String,
    val address: String? = null,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("created_at") val createdAt: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("geofence_radius") val geofenceRadius: Float = 200f,
    @SerialName("geofence_enabled") val geofenceEnabled: Boolean = false,
)
