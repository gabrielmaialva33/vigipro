package com.vigipro.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.core.ui.theme.StatusBadgeShape
import com.vigipro.core.ui.theme.StatusError
import com.vigipro.core.ui.theme.StatusOffline
import com.vigipro.core.ui.theme.StatusOnline

enum class ConnectionStatus { ONLINE, OFFLINE, ERROR, CONNECTING }

@Composable
fun StatusBadge(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    // Direct color lookup — animateColorAsState causes recomposition on status change.
    // For a status indicator, instant color swap is perfectly fine and lighter.
    val dotColor = remember(status) {
        when (status) {
            ConnectionStatus.ONLINE -> StatusOnline
            ConnectionStatus.OFFLINE -> StatusOffline
            ConnectionStatus.ERROR -> StatusError
            ConnectionStatus.CONNECTING -> StatusOnline.copy(alpha = 0.5f)
        }
    }

    val label = when (status) {
        ConnectionStatus.ONLINE -> "Online"
        ConnectionStatus.OFFLINE -> "Offline"
        ConnectionStatus.ERROR -> "Erro"
        ConnectionStatus.CONNECTING -> "Conectando..."
    }

    Row(
        modifier = modifier
            .background(
                color = dotColor.copy(alpha = 0.12f),
                shape = StatusBadgeShape,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Animated status dot
        if (status == ConnectionStatus.ONLINE) {
            PulsingStatusDot(color = dotColor)
        } else {
            Box(
                modifier = Modifier
                    .size(Dimens.CameraStatusDotSize)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = dotColor,
            )
        }
    }
}

@Composable
private fun PulsingStatusDot(
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    // Single animation instead of two (scale + alpha) — halves CPU cost on low-end devices.
    // On a Galaxy A01 with 4 cameras = 4 fewer simultaneous animators.
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )

    Box(
        modifier = modifier
            .size(Dimens.CameraStatusDotSize)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
fun StatusDot(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
) {
    val dotColor = when (status) {
        ConnectionStatus.ONLINE -> StatusOnline
        ConnectionStatus.OFFLINE -> StatusOffline
        ConnectionStatus.ERROR -> StatusError
        ConnectionStatus.CONNECTING -> StatusOnline.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .size(Dimens.CameraStatusDotSize)
            .clip(CircleShape)
            .background(dotColor),
    )
}
