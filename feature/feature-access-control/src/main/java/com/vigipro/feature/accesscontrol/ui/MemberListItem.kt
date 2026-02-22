package com.vigipro.feature.accesscontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vigipro.core.model.SiteMember
import com.vigipro.core.model.UserRole
import com.vigipro.core.ui.components.RoleBadge
import com.vigipro.core.ui.theme.Dimens

@Composable
fun MemberListItem(
    member: SiteMember,
    canRemove: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.userId.take(8) + "...",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (member.validUntil != null) {
                Text(
                    text = "Valido ate: ${member.validUntil}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RoleBadge(role = member.role)
        if (canRemove && member.role != UserRole.OWNER) {
            IconButton(onClick = onRemove) {
                Icon(
                    PhosphorIcons.Regular.UserMinus,
                    contentDescription = "Remover membro",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
