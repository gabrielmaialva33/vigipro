package com.vigipro.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.feature.player.ptz.PtzPreset
import kotlinx.coroutines.withTimeout

@Composable
fun PtzControlPad(
    isVisible: Boolean,
    presets: List<PtzPreset>,
    onMove: (x: Float, y: Float, z: Float) -> Unit,
    onStop: () -> Unit,
    onPresetClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + slideInHorizontally(
            animationSpec = tween(300, easing = EaseOut),
            initialOffsetX = { -it / 2 },
        ),
        exit = fadeOut(animationSpec = tween(150)) + slideOutHorizontally(
            animationSpec = tween(250, easing = EaseIn),
            targetOffsetX = { -it / 2 },
        ),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Dimens.PlayerOverlayPadding),
        ) {
            // D-Pad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Up
                PtzDirectionButton(
                    icon = Icons.Default.ArrowDropUp,
                    description = "Cima",
                    onPress = { onMove(0f, 0.5f, 0f) },
                    onRelease = onStop,
                )

                // Left, Center, Right
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PtzDirectionButton(
                        icon = Icons.Default.ArrowLeft,
                        description = "Esquerda",
                        onPress = { onMove(-0.5f, 0f, 0f) },
                        onRelease = onStop,
                    )
                    Box(
                        modifier = Modifier.size(Dimens.PtzCenterButtonSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(Dimens.IconSm),
                        )
                    }
                    PtzDirectionButton(
                        icon = Icons.Default.ArrowRight,
                        description = "Direita",
                        onPress = { onMove(0.5f, 0f, 0f) },
                        onRelease = onStop,
                    )
                }

                // Down
                PtzDirectionButton(
                    icon = Icons.Default.ArrowDropDown,
                    description = "Baixo",
                    onPress = { onMove(0f, -0.5f, 0f) },
                    onRelease = onStop,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            // Zoom controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
            ) {
                PtzDirectionButton(
                    icon = Icons.Default.Remove,
                    description = "Zoom -",
                    onPress = { onMove(0f, 0f, -0.5f) },
                    onRelease = onStop,
                    size = Dimens.PtzZoomButtonSize,
                )
                PtzDirectionButton(
                    icon = Icons.Default.Add,
                    description = "Zoom +",
                    onPress = { onMove(0f, 0f, 0.5f) },
                    onRelease = onStop,
                    size = Dimens.PtzZoomButtonSize,
                )
            }

            // Presets
            if (presets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs),
                    modifier = Modifier.width(Dimens.PtzPadSize),
                ) {
                    items(presets) { preset ->
                        AssistChip(
                            onClick = { onPresetClick(preset.token) },
                            label = {
                                Text(
                                    text = preset.name.ifBlank { preset.token },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PtzDirectionButton(
    icon: ImageVector,
    description: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.PtzDirectionButtonSize,
) {
    Box(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .pointerInput(onPress, onRelease) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        try {
                            withTimeout(5000L) {
                                awaitRelease()
                            }
                        } catch (_: Exception) {
                            // Timeout or cancellation
                        } finally {
                            onRelease()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(size - 8.dp),
        )
    }
}
