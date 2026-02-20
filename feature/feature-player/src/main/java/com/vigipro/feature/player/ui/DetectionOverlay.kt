package com.vigipro.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.vigipro.core.model.DetectedObject
import com.vigipro.core.model.DetectionCategory

private val PersonColor = Color(0xFFFF5722)
private val VehicleColor = Color(0xFF2196F3)
private val AnimalColor = Color(0xFF4CAF50)
private val OtherColor = Color(0xFFFFEB3B)

@Composable
fun DetectionOverlay(
    detectedObjects: List<DetectedObject>,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isActive) return

    Box(modifier = modifier.fillMaxSize()) {
        // Bounding boxes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            detectedObjects.forEach { obj ->
                val color = when (obj.category) {
                    DetectionCategory.PERSON -> PersonColor
                    DetectionCategory.VEHICLE -> VehicleColor
                    DetectionCategory.ANIMAL -> AnimalColor
                    DetectionCategory.OTHER -> OtherColor
                }

                val left = obj.boundingBox.left * canvasWidth
                val top = obj.boundingBox.top * canvasHeight
                val right = obj.boundingBox.right * canvasWidth
                val bottom = obj.boundingBox.bottom * canvasHeight

                // Bounding box
                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 3.dp.toPx()),
                )

                // Label background
                val labelText = "${obj.label} ${(obj.confidence * 100).toInt()}%"
                val paint = android.graphics.Paint().apply {
                    textSize = 12.dp.toPx()
                    this.color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                val textWidth = paint.measureText(labelText)
                val textHeight = paint.textSize

                drawRect(
                    color = Color.Black.copy(alpha = 0.7f),
                    topLeft = Offset(left, top - textHeight - 6.dp.toPx()),
                    size = Size(textWidth + 8.dp.toPx(), textHeight + 6.dp.toPx()),
                )

                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    left + 4.dp.toPx(),
                    top - 4.dp.toPx(),
                    paint,
                )
            }
        }

        // AI badge
        AnimatedVisibility(
            visible = detectedObjects.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Surface(
                color = Color(0xFFD32F2F),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = "IA",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
