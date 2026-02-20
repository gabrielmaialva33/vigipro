package com.vigipro.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PatrolRoute(
    val id: String,
    val cameraId: String,
    val name: String,
    val waypoints: List<PatrolWaypoint>,
    val repeatForever: Boolean = true,
)

@Serializable
data class PatrolWaypoint(
    val presetToken: String,
    val presetName: String,
    val dwellTimeSeconds: Int = 10,
)
