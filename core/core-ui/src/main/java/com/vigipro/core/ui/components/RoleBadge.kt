package com.vigipro.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vigipro.core.model.UserRole
import com.vigipro.core.ui.theme.StatusBadgeShape

@Composable
fun RoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier,
) {
    val (label, bgColor, textColor) = when (role) {
        UserRole.OWNER -> Triple(
            "Proprietario",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        UserRole.ADMIN -> Triple(
            "Admin",
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
        )
        UserRole.VIEWER -> Triple(
            "Viewer",
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary,
        )
        UserRole.TIME_RESTRICTED -> Triple(
            "Horario",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        UserRole.GUEST -> Triple(
            "Convidado",
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurface,
        )
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = modifier
            .background(
                color = bgColor,
                shape = StatusBadgeShape,
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
