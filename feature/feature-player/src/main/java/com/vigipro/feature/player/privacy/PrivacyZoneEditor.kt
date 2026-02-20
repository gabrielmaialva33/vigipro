package com.vigipro.feature.player.privacy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vigipro.core.model.PrivacyZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val EditorOverlayColor = Color(0x4D1565C0) // semi-transparent blue
private val ExistingZoneBorderColor = Color(0xFFD32F2F) // red
private val ExistingZoneFillColor = Color(0x33D32F2F) // semi-transparent red
private val DrawingZoneBorderColor = Color(0xFF4CAF50) // green
private val DrawingZoneFillColor = Color(0x334CAF50) // semi-transparent green
private val DeleteButtonBg = Color(0xFFD32F2F)

@Composable
fun PrivacyZoneEditor(
    zones: List<PrivacyZone>,
    onZoneAdded: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onZoneDeleted: (PrivacyZone) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Interactive canvas overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStart = offset
                            dragCurrent = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragCurrent = change.position
                        },
                        onDragEnd = {
                            val start = dragStart
                            val current = dragCurrent
                            if (start != null && current != null && canvasSize.width > 0 && canvasSize.height > 0) {
                                val left = min(start.x, current.x) / canvasSize.width
                                val top = min(start.y, current.y) / canvasSize.height
                                val right = max(start.x, current.x) / canvasSize.width
                                val bottom = max(start.y, current.y) / canvasSize.height

                                // Only add if the zone has meaningful size (at least 2% in both dimensions)
                                if (right - left > 0.02f && bottom - top > 0.02f) {
                                    onZoneAdded(
                                        left.coerceIn(0f, 1f),
                                        top.coerceIn(0f, 1f),
                                        right.coerceIn(0f, 1f),
                                        bottom.coerceIn(0f, 1f),
                                    )
                                }
                            }
                            dragStart = null
                            dragCurrent = null
                        },
                        onDragCancel = {
                            dragStart = null
                            dragCurrent = null
                        },
                    )
                },
        ) {
            canvasSize = size

            // Semi-transparent blue overlay
            drawRect(
                color = EditorOverlayColor,
                topLeft = Offset.Zero,
                size = size,
            )

            // Draw existing zones
            zones.forEach { zone ->
                val rect = Rect(
                    left = zone.left * size.width,
                    top = zone.top * size.height,
                    right = zone.right * size.width,
                    bottom = zone.bottom * size.height,
                )
                drawRect(
                    color = ExistingZoneFillColor,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                )
                drawRect(
                    color = ExistingZoneBorderColor,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            // Draw zone being drawn
            val start = dragStart
            val current = dragCurrent
            if (start != null && current != null) {
                val drawingRect = Rect(
                    left = min(start.x, current.x),
                    top = min(start.y, current.y),
                    right = max(start.x, current.x),
                    bottom = max(start.y, current.y),
                )
                drawRect(
                    color = DrawingZoneFillColor,
                    topLeft = Offset(drawingRect.left, drawingRect.top),
                    size = Size(drawingRect.width, drawingRect.height),
                )
                drawRect(
                    color = DrawingZoneBorderColor,
                    topLeft = Offset(drawingRect.left, drawingRect.top),
                    size = Size(drawingRect.width, drawingRect.height),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        // Delete buttons for existing zones
        val density = LocalDensity.current
        zones.forEach { zone ->
            val deleteX = with(density) {
                (zone.right * canvasSize.width).roundToInt() - 12.dp.roundToPx()
            }
            val deleteY = with(density) {
                (zone.top * canvasSize.height).roundToInt() - 12.dp.roundToPx()
            }
            IconButton(
                onClick = { onZoneDeleted(zone) },
                modifier = Modifier
                    .offset { IntOffset(deleteX, deleteY) }
                    .size(24.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = DeleteButtonBg,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remover zona",
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        // Instructions text (top-center)
        Text(
            text = "Arraste para criar uma zona de privacidade",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )

        // Zone count indicator (top-start)
        if (zones.isNotEmpty()) {
            Text(
                text = "${zones.size} zona${if (zones.size > 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp, start = 12.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        // "Concluir" button (bottom-center)
        FilledTonalButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "  Concluir",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
