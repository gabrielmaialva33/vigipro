package com.vigipro.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.vigipro.core.ui.theme.Dimens

enum class PermissionType(val label: String, val icon: ImageVector) {
    VIEW_LIVE("Ao Vivo", Icons.Default.Visibility),
    PLAYBACK("Playback", Icons.Default.PlayCircle),
    PTZ("PTZ", Icons.Default.OpenWith),
    AUDIO("Audio", Icons.Default.Mic),
    EXPORT("Export", Icons.Default.Download),
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
