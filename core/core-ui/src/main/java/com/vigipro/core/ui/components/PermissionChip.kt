package com.vigipro.core.ui.components

import androidx.compose.foundation.layout.size
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.vigipro.core.ui.theme.Dimens

enum class PermissionType(val label: String, val icon: ImageVector) {
    VIEW_LIVE("Ao Vivo", PhosphorIcons.Regular.Eye),
    PLAYBACK("Playback", PhosphorIcons.Regular.PlayCircle),
    PTZ("PTZ", PhosphorIcons.Regular.ArrowsOut),
    AUDIO("Audio", PhosphorIcons.Regular.Microphone),
    EXPORT("Export", PhosphorIcons.Regular.Download),
}

@Composable
fun PermissionChip(
    type: PermissionType,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        label = { Text(type.label) },
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}
