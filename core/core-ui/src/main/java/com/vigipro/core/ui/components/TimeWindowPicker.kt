package com.vigipro.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vigipro.core.ui.theme.Dimens

private val DAYS = listOf(
    1 to "Seg",
    2 to "Ter",
    3 to "Qua",
    4 to "Qui",
    5 to "Sex",
    6 to "Sab",
    7 to "Dom",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeWindowPicker(
    timeStart: String,
    timeEnd: String,
    selectedDays: List<Int>,
    onTimeStartChange: (String) -> Unit,
    onTimeEndChange: (String) -> Unit,
    onDaysChange: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Janela de Horario",
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingSm))

        // Time range
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
        ) {
            OutlinedTextField(
                value = timeStart,
                onValueChange = onTimeStartChange,
                label = { Text("Inicio") },
                leadingIcon = {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("08:00") },
            )

            Text(
                text = "ate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = timeEnd,
                onValueChange = onTimeEndChange,
                label = { Text("Fim") },
                leadingIcon = {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("18:00") },
            )
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))

        // Days of week
        Text(
            text = "Dias da Semana",
            style = MaterialTheme.typography.labelLarge,
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingXs))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs),
        ) {
            DAYS.forEach { (dayNum, dayLabel) ->
                FilterChip(
                    selected = dayNum in selectedDays,
                    onClick = {
                        onDaysChange(
                            if (dayNum in selectedDays) {
                                selectedDays - dayNum
                            } else {
                                selectedDays + dayNum
                            }
                        )
                    },
                    label = { Text(dayLabel) },
                )
            }
        }
    }
}
