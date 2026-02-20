package com.vigipro.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private val TalkbackIdleColor = Color.Black.copy(alpha = 0.6f)
private val TalkbackActiveColor = Color(0xFFD32F2F)

/**
 * Push-to-talk button for 2-way audio.
 * Press and hold to talk, release to stop.
 * Shows pulsating red background while active.
 */
@Composable
fun TalkbackButton(
    isActive: Boolean,
    isVisible: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier,
    ) {
        val pulseTransition = rememberInfiniteTransition(label = "talkback_pulse")
        val pulseScale by pulseTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse_scale",
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(if (isActive) pulseScale else 1f)
                .clip(CircleShape)
                .background(if (isActive) TalkbackActiveColor else TalkbackIdleColor)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onPress()
                        waitForUpOrCancellation()
                        onRelease()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = if (isActive) "Falando" else "Segure para falar",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
