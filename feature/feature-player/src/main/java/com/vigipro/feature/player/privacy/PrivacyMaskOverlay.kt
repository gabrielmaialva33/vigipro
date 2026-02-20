package com.vigipro.feature.player.privacy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.vigipro.core.model.PrivacyZone

@Composable
fun PrivacyMaskOverlay(
    zones: List<PrivacyZone>,
    modifier: Modifier = Modifier,
) {
    if (zones.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        zones.forEach { zone ->
            val rect = Rect(
                left = zone.left * size.width,
                top = zone.top * size.height,
                right = zone.right * size.width,
                bottom = zone.bottom * size.height,
            )

            // Dark mask
            drawRect(
                color = Color.Black.copy(alpha = 0.85f),
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
            )

            // Grid pattern lines for visual indication
            val step = 12f
            var x = rect.left
            while (x < rect.right) {
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(x, rect.top),
                    end = Offset(x, rect.bottom),
                    strokeWidth = 1f,
                )
                x += step
            }
            var y = rect.top
            while (y < rect.bottom) {
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(rect.left, y),
                    end = Offset(rect.right, y),
                    strokeWidth = 1f,
                )
                y += step
            }
        }
    }
}
