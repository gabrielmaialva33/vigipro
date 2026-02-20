package com.vigipro.core.model

data class Recording(
    val id: Long = 0,
    val cameraId: String,
    val cameraName: String,
    val filePath: String,
    val startTime: Long,
    val endTime: Long = 0,
    val fileSize: Long = 0,
    val durationMs: Long = 0,
    val thumbnailPath: String? = null,
)
