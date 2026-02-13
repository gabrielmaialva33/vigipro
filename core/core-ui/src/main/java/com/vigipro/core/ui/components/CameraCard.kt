package com.vigipro.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vigipro.core.ui.theme.CameraCardShape
import com.vigipro.core.ui.theme.CameraPreviewShape
import com.vigipro.core.ui.theme.Dimens

@Composable
fun CameraCard(
    name: String,
    status: ConnectionStatus,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ptzCapable: Boolean = false,
    audioCapable: Boolean = false,
    previewContent: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = CameraCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CameraCardElevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            // Camera preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(Dimens.CameraPreviewAspectRatio)
                    .clip(CameraPreviewShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (previewContent != null) {
                    previewContent()
                } else {
                    // Placeholder icon
                    Icon(
                        imageVector = if (status == ConnectionStatus.ONLINE) {
                            Icons.Default.Videocam
                        } else {
                            Icons.Default.VideocamOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconLg),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }

                // Status overlay (top-left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Dimens.SpacingSm),
                ) {
                    StatusBadge(status = status, showLabel = false)
                }
            }

            // Camera info
            Column(
                modifier = Modifier.padding(
                    horizontal = Dimens.SpacingSm,
                    vertical = Dimens.SpacingXs,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (ptzCapable) {
                            Icon(
                                imageVector = Icons.Default.OpenWith,
                                contentDescription = "PTZ",
                                modifier = Modifier.size(Dimens.IconSm),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        if (audioCapable) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Audio",
                                modifier = Modifier.size(Dimens.IconSm),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}
