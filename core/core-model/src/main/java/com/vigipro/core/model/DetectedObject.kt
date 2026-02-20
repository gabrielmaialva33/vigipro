package com.vigipro.core.model

import android.graphics.RectF

data class DetectedObject(
    val label: String,
    val category: DetectionCategory,
    val confidence: Float,
    val boundingBox: RectF,
    val trackingId: Int? = null,
)

enum class DetectionCategory {
    PERSON,
    VEHICLE,
    ANIMAL,
    OTHER,
}
