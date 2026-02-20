package com.vigipro.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.core.ui.theme.StreamInfoShape

@Composable
fun StreamInfoOverlay(
    isVisible: Boolean,
    codec: String,
    resolution: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(150)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = StreamInfoShape,
                )
                .padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (codec.isNotBlank()) {
                Text(
                    text = codec,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
            }
            if (resolution.isNotBlank()) {
                Text(
                    text = resolution,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}
