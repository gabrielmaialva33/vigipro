package com.vigipro.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
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
    isDetectionActive: Boolean,
    detectionEnabled: Boolean,
    isTalkbackActive: Boolean,
    talkbackAvailable: Boolean,
    cameraName: String,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onAudioToggle: () -> Unit,
    onSnapshotClick: () -> Unit,
    onPtzToggle: () -> Unit,
    onInfoClick: () -> Unit,
    onDetectionToggle: () -> Unit,
    onTalkbackToggle: () -> Unit,
    onPipClick: () -> Unit,
    isRecording: Boolean,
    recordingDurationMs: () -> Long,
    onRecordClick: () -> Unit,
    isPatrolling: Boolean,
    onPatrolClick: () -> Unit,
    onPrivacyZoneToggle: () -> Unit,
    hasPrivacyZones: Boolean,
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
                        icon = { Icon(PhosphorIcons.Regular.ArrowLeft, "Voltar", tint = Color.White) },
                    )
                    Text(
                        text = cameraName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isRecording) {
                        Spacer(modifier = Modifier.width(8.dp))
                        RecordingIndicator(recordingDurationMs = recordingDurationMs)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OverlayIconButton(
                        onClick = onInfoClick,
                        icon = { Icon(PhosphorIcons.Regular.Info, "Informacoes do stream", tint = Color.White) },
                    )
                    OverlayIconButton(
                        onClick = onPipClick,
                        icon = { Icon(PhosphorIcons.Regular.PictureInPicture, "Picture-in-Picture", tint = Color.White) },
                    )
                    OverlayIconButton(
                        onClick = onFullscreenClick,
                        icon = {
                            Icon(
                                if (isFullscreen) PhosphorIcons.Regular.CornersIn else PhosphorIcons.Regular.CornersOut,
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
                                    PhosphorIcons.Regular.ArrowsOut,
                                    if (showPtzControls) "Ocultar controle PTZ" else "Mostrar controle PTZ",
                                    tint = if (showPtzControls) MaterialTheme.colorScheme.primary else Color.White,
                                )
                            },
                        )
                        OverlayIconButton(
                            onClick = onPatrolClick,
                            icon = {
                                Icon(
                                    PhosphorIcons.Regular.Path,
                                    if (isPatrolling) "Patrulha ativa" else "Patrulha",
                                    tint = if (isPatrolling) Color(0xFF4CAF50) else Color.White,
                                )
                            },
                        )
                    }
                    if (detectionEnabled) {
                        OverlayIconButton(
                            onClick = onDetectionToggle,
                            icon = {
                                Icon(
                                    if (isDetectionActive) PhosphorIcons.Regular.Eye else PhosphorIcons.Regular.EyeSlash,
                                    if (isDetectionActive) "Desativar detecção" else "Ativar detecção",
                                    tint = if (isDetectionActive) Color(0xFFFF9800) else Color.White,
                                )
                            },
                        )
                    }
                    if (talkbackAvailable) {
                        OverlayIconButton(
                            onClick = onTalkbackToggle,
                            icon = {
                                Icon(
                                    if (isTalkbackActive) PhosphorIcons.Regular.Microphone else PhosphorIcons.Regular.MicrophoneSlash,
                                    if (isTalkbackActive) "Áudio bidirecional ativo" else "Ativar áudio bidirecional",
                                    tint = if (isTalkbackActive) Color(0xFFD32F2F) else Color.White,
                                )
                            },
                        )
                    }
                    OverlayIconButton(
                        onClick = onPrivacyZoneToggle,
                        icon = {
                            Icon(
                                PhosphorIcons.Regular.Shield,
                                "Máscaras de privacidade",
                                tint = if (hasPrivacyZones) Color(0xFF7C4DFF) else Color.White,
                            )
                        },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OverlayIconButton(
                        onClick = onAudioToggle,
                        icon = {
                            Icon(
                                if (isAudioEnabled) PhosphorIcons.Regular.SpeakerHigh else PhosphorIcons.Regular.SpeakerSlash,
                                if (isAudioEnabled) "Desativar áudio" else "Ativar áudio",
                                tint = Color.White,
                            )
                        },
                    )
                    OverlayIconButton(
                        onClick = onSnapshotClick,
                        icon = { Icon(PhosphorIcons.Regular.Camera, "Captura", tint = Color.White) },
                    )
                    OverlayIconButton(
                        onClick = onRecordClick,
                        icon = {
                            Icon(
                                if (isRecording) PhosphorIcons.Regular.Stop else PhosphorIcons.Regular.Circle,
                                if (isRecording) "Parar gravacao" else "Gravar",
                                tint = if (isRecording) Color(0xFFD32F2F) else Color.White,
                            )
                        },
                    )
                    OverlayIconButton(
                        onClick = onPlayPauseClick,
                        icon = {
                            Icon(
                                if (isPlaying) PhosphorIcons.Regular.Pause else PhosphorIcons.Regular.Play,
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
private fun RecordingIndicator(
    recordingDurationMs: () -> Long,
    modifier: Modifier = Modifier,
) {
    val totalSeconds = (recordingDurationMs() / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(
                color = Color(0xCCD32F2F),
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
        )
        Text(
            text = timerText,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
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
