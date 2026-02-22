package com.vigipro.core.model

import android.graphics.RectF

data class RecognizedText(
    val text: String,
    val confidence: Float?,
    val boundingBox: RectF?,
    val language: String? = null,
)
