package com.vigipro.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val VigiProShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// Camera card specific
val CameraCardShape = RoundedCornerShape(12.dp)
val CameraPreviewShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val StatusBadgeShape = RoundedCornerShape(4.dp)
