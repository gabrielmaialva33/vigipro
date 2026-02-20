package com.vigipro.feature.player.patrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vigipro.core.model.PatrolRoute
import com.vigipro.core.model.PatrolWaypoint
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.feature.player.ptz.PtzPreset
import java.util.UUID

private val DwellTimeOptions = listOf(5, 10, 15, 30, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatrolConfigSheet(
    presets: List<PtzPreset>,
    patrolState: PatrolManager.PatrolState,
    cameraId: String,
    onStartPatrol: (PatrolRoute) -> Unit,
    onStopPatrol: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val waypoints = remember { mutableStateListOf<PatrolWaypoint>() }
    var repeatForever by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm),
        ) {
            Text(
                text = "Patrulha Automatica",
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            if (presets.isEmpty()) {
                Text(
                    text = "Nenhum preset configurado na camera. Configure presets PTZ para usar a patrulha automatica.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingXl))
                return@Column
            }

            // Available presets section
            Text(
                text = "Presets disponiveis",
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingSm))

            presets.forEach { preset ->
                val isSelected = waypoints.any { it.presetToken == preset.token }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.SpacingXxs),
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                waypoints.add(
                                    PatrolWaypoint(
                                        presetToken = preset.token,
                                        presetName = preset.name.ifBlank { preset.token },
                                        dwellTimeSeconds = 10,
                                    ),
                                )
                            } else {
                                waypoints.removeAll { it.presetToken == preset.token }
                            }
                        },
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Text(
                        text = preset.name.ifBlank { "Preset ${preset.token}" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Patrol route section
            if (waypoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingLg))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Dimens.SpacingLg))

                Text(
                    text = "Rota de patrulha",
                    style = MaterialTheme.typography.titleSmall,
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingSm))

                LazyColumn(
                    modifier = Modifier.height((waypoints.size * 64).coerceAtMost(256).dp),
                ) {
                    itemsIndexed(waypoints.toList()) { index, waypoint ->
                        WaypointItem(
                            index = index,
                            waypoint = waypoint,
                            onDwellTimeChanged = { newDwell ->
                                waypoints[index] = waypoint.copy(dwellTimeSeconds = newDwell)
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    val item = waypoints.removeAt(index)
                                    waypoints.add(index - 1, item)
                                }
                            },
                            onMoveDown = {
                                if (index < waypoints.lastIndex) {
                                    val item = waypoints.removeAt(index)
                                    waypoints.add(index + 1, item)
                                }
                            },
                            canMoveUp = index > 0,
                            canMoveDown = index < waypoints.lastIndex,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingMd))

                // Repeat toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Repetir indefinidamente",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = repeatForever,
                        onCheckedChange = { repeatForever = it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            // Action button
            if (patrolState.isPatrolling) {
                Button(
                    onClick = onStopPatrol,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Text(text = "Parar Patrulha")
                }
            } else {
                Button(
                    onClick = {
                        val route = PatrolRoute(
                            id = UUID.randomUUID().toString(),
                            cameraId = cameraId,
                            name = "Patrulha",
                            waypoints = waypoints.toList(),
                            repeatForever = repeatForever,
                        )
                        onStartPatrol(route)
                    },
                    enabled = waypoints.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    Text(text = "Iniciar Patrulha")
                }
            }

            // Status indicator when patrolling
            if (patrolState.isPatrolling) {
                Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                Text(
                    text = "Preset atual: ${patrolState.currentPresetName} (${patrolState.remainingDwellSeconds}s restantes)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaypointItem(
    index: Int,
    waypoint: PatrolWaypoint,
    onDwellTimeChanged: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    modifier: Modifier = Modifier,
) {
    var dwellExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingXs),
    ) {
        // Reorder buttons
        Column {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.height(20.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Mover para cima",
                    modifier = Modifier.height(16.dp),
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.height(20.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Mover para baixo",
                    modifier = Modifier.height(16.dp),
                )
            }
        }

        // Index badge
        Text(
            text = "${index + 1}.",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        // Preset name
        Text(
            text = waypoint.presetName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        // Dwell time dropdown
        ExposedDropdownMenuBox(
            expanded = dwellExpanded,
            onExpandedChange = { dwellExpanded = it },
            modifier = Modifier.width(100.dp),
        ) {
            OutlinedTextField(
                value = "${waypoint.dwellTimeSeconds}s",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dwellExpanded) },
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true,
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = dwellExpanded,
                onDismissRequest = { dwellExpanded = false },
            ) {
                DwellTimeOptions.forEach { seconds ->
                    DropdownMenuItem(
                        text = { Text("${seconds}s") },
                        onClick = {
                            onDwellTimeChanged(seconds)
                            dwellExpanded = false
                        },
                    )
                }
            }
        }
    }
}
