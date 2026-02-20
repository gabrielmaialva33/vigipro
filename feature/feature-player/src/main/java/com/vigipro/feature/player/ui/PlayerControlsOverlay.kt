package com.vigipro.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigipro.core.ui.theme.Dimens

private val OverlayScrim = Color.Black.copy(alpha = 0.55f)
private val ControlBg = Color.Black.copy(alpha = 0.5f)

@Composable
fun PlayerControlsOverlay(
    isVisible: Boolean,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    isAudioEnabled: Boolean,
    isPtzCapable: Boolean,
    showPtzControls: Boolean,
    cameraName: String,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onAudioToggle: () -> Unit,
    onSnapshotClick: () -> Unit,
    onPtzToggle: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OverlayScrim),
        ) {
            // Top row: Back, camera name, info, fullscreen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(Dimens.PlayerOverlayPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    OverlayIconButton(
                        onClick = onBackClick,
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White) },
                    )
                    Text(
                        text = cameraName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OverlayIconButton(
                        onClick = onInfoClick,
                        icon = { Icon(Icons.Default.Info, "Informacoes do stream", tint = Color.White) },
                    )
                    OverlayIconButton(
                        onClick = onFullscreenClick,
                        icon = {
                            Icon(
                                if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                if (isFullscreen) "Sair da tela cheia" else "Tela cheia",
                                tint = Color.White,
                            )
                        },
                    )
                }
            }

            // Bottom row: PTZ, audio, snapshot, play/pause
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(Dimens.PlayerOverlayPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isPtzCapable) {
                        OverlayIconButton(
                            onClick = onPtzToggle,
                            icon = {
                                Icon(
                                    Icons.Default.OpenWith,
                                    if (showPtzControls) "Ocultar controle PTZ" else "Mostrar controle PTZ",
                                    tint = if (showPtzControls) MaterialTheme.colorScheme.primary else Color.White,
                                )
                            },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OverlayIconButton(
                        onClick = onAudioToggle,
                        icon = {
                            Icon(
                                if (isAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                if (isAudioEnabled) "Desativar audio" else "Ativar audio",
                                tint = Color.White,
                            )
                        },
                    )
                    OverlayIconButton(
                        onClick = onSnapshotClick,
                        icon = { Icon(Icons.Default.CameraAlt, "Captura", tint = Color.White) },
                    )
                    OverlayIconButton(
                        onClick = onPlayPauseClick,
                        icon = {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isPlaying) "Pausar" else "Reproduzir",
                                tint = Color.White,
                                modifier = Modifier.size(Dimens.PlayerControlIconSize + 4.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(Dimens.PlayerControlSize),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = ControlBg,
            contentColor = Color.White,
        ),
    ) {
        icon()
    }
}
