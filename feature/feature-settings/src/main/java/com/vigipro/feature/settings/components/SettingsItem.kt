package com.vigipro.feature.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.vigipro.core.ui.theme.Dimens

@Composable
fun SettingsToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun <T> SettingsDropdownItem(
    title: String,
    currentValue: T,
    options: List<T>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    displayMapper: (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = { expanded = true },
                onClickLabel = "Alterar $title",
            )
            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = displayMapper(currentValue),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayMapper(option)) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SettingsInfoItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    destructive: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                onClickLabel = title,
                role = Role.Button,
            )
            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
