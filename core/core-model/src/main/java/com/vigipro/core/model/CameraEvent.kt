package com.vigipro.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class CameraEventType {
    WENT_OFFLINE,
    CAME_ONLINE,
    SNAPSHOT_TAKEN,
    CAMERA_ADDED,
    CAMERA_REMOVED,
}

data class CameraEvent(
    val id: Long = 0,
    val cameraId: String,
    val cameraName: String,
    val eventType: CameraEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String? = null,
)
