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
)
