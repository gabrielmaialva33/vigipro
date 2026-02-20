package com.vigipro.core.ui.extensions

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Applies a shimmer loading effect to any composable.
 * Use on placeholder elements while content is loading.
 *
 * Example: `Box(modifier = Modifier.fillMaxWidth().height(120.dp).shimmerEffect())`
 */
fun Modifier.shimmerEffect(
    baseColor: Color = Color(0xFFE0E0E0),
    highlightColor: Color = Color(0xFFF5F5F5),
    durationMillis: Int = 1200,
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    this.background(
        brush = Brush.linearGradient(
            colors = listOf(baseColor, highlightColor, baseColor),
            start = Offset(translateAnim - 200f, 0f),
            end = Offset(translateAnim + 200f, 0f),
        ),
    )
}
