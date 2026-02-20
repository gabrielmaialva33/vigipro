package com.vigipro.feature.accesscontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vigipro.core.model.UserRole
import com.vigipro.core.ui.components.TimeWindowPicker
import com.vigipro.core.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInviteSheet(
    inviteRole: UserRole,
    inviteExpiresHours: Int,
    inviteMaxUses: Int,
    timeStart: String,
    timeEnd: String,
    selectedDays: List<Int>,
    onRoleChange: (UserRole) -> Unit,
    onExpiresChange: (Int) -> Unit,
    onMaxUsesChange: (Int) -> Unit,
    onTimeStartChange: (String) -> Unit,
    onTimeEndChange: (String) -> Unit,
    onDaysChange: (List<Int>) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Criar Convite",
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Role selection
            Text(
                text = "Permissao",
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                modifier = Modifier.padding(vertical = Dimens.SpacingSm),
            ) {
                listOf(
                    UserRole.VIEWER to "Visualizador",
                    UserRole.TIME_RESTRICTED to "Restrito",
                    UserRole.GUEST to "Convidado",
                ).forEach { (role, label) ->
                    FilterChip(
                        selected = inviteRole == role,
                        onClick = { onRoleChange(role) },
                        label = { Text(label) },
                    )
                }
            }

            // Time window (only for TIME_RESTRICTED role)
            if (inviteRole == UserRole.TIME_RESTRICTED) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpacingSm))

                TimeWindowPicker(
                    timeStart = timeStart,
                    timeEnd = timeEnd,
                    selectedDays = selectedDays,
                    onTimeStartChange = onTimeStartChange,
                    onTimeEndChange = onTimeEndChange,
                    onDaysChange = onDaysChange,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpacingSm))

            // Expiration
            Text(
                text = "Validade",
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                modifier = Modifier.padding(vertical = Dimens.SpacingSm),
            ) {
                listOf(
                    1 to "1 hora",
                    24 to "24 horas",
                    168 to "7 dias",
                    720 to "30 dias",
                ).forEach { (hours, label) ->
                    FilterChip(
                        selected = inviteExpiresHours == hours,
                        onClick = { onExpiresChange(hours) },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            // Max uses
            Text(
                text = "Usos maximos",
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                modifier = Modifier.padding(vertical = Dimens.SpacingSm),
            ) {
                listOf(1, 5, 10, 50).forEach { uses ->
                    FilterChip(
                        selected = inviteMaxUses == uses,
                        onClick = { onMaxUsesChange(uses) },
                        label = { Text("$uses") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Criar Convite")
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))
        }
    }
}
