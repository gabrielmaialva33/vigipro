package com.vigipro.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Site(
    val id: String,
    val name: String,
    val address: String? = null,
    val ownerId: String,
    val createdAt: String? = null,
)
