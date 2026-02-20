package com.vigipro.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PrivacyZone(
    val id: String,
    val cameraId: String,
    val label: String = "",
    val left: Float,   // 0.0..1.0 normalized
    val top: Float,    // 0.0..1.0 normalized
    val right: Float,  // 0.0..1.0 normalized
    val bottom: Float, // 0.0..1.0 normalized
)
