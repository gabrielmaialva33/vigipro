package com.vigipro.feature.player.patrol

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PatrolIdleBg = Color.Black.copy(alpha = 0.6f)
private val PatrolActiveBg = Color(0xFF2E7D32)

@Composable
fun PatrolButton(
    patrolState: PatrolManager.PatrolState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier,
    ) {
        val pulseTransition = rememberInfiniteTransition(label = "patrol_pulse")
        val pulseAlpha by pulseTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "patrol_pulse_alpha",
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (patrolState.isPatrolling) PatrolActiveBg else PatrolIdleBg)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .then(
                    if (patrolState.isPatrolling) Modifier.alpha(pulseAlpha) else Modifier,
                ),
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.Path,
                contentDescription = if (patrolState.isPatrolling) "Parar patrulha" else "Patrulha",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
            if (patrolState.isPatrolling) {
                Text(
                    text = patrolState.currentPresetName.ifBlank { "Preset ${patrolState.currentWaypointIndex + 1}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
                Text(
                    text = "${patrolState.remainingDwellSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            } else {
                Text(
                    text = "Patrulha",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}
