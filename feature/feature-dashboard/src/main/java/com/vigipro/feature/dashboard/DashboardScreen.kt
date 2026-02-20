package com.vigipro.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vigipro.core.model.CameraStatus
import com.vigipro.core.ui.components.CameraCard
import com.vigipro.core.ui.components.ConnectionStatus
import com.vigipro.core.ui.components.EmptyState
import com.vigipro.core.ui.components.GridLayoutToggle
import com.vigipro.core.ui.components.LoadingIndicator
import com.vigipro.core.ui.components.SiteDropdown
import com.vigipro.core.ui.components.SiteItem
import com.vigipro.core.ui.components.VigiProTopBar
import com.vigipro.core.ui.theme.Dimens
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToAddCamera: () -> Unit,
    onNavigateToEditCamera: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccessControl: () -> Unit = {},
    onNavigateToEventTimeline: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    var showSiteDropdown by remember { mutableStateOf(false) }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is DashboardSideEffect.NavigateToPlayer -> onNavigateToPlayer(sideEffect.cameraId)
            DashboardSideEffect.NavigateToAddCamera -> onNavigateToAddCamera()
            is DashboardSideEffect.NavigateToEditCamera -> onNavigateToEditCamera(sideEffect.cameraId)
            DashboardSideEffect.NavigateToSettings -> onNavigateToSettings()
            DashboardSideEffect.NavigateToAccessControl -> onNavigateToAccessControl()
            DashboardSideEffect.NavigateToEventTimeline -> onNavigateToEventTimeline()
        }
    }

    // Delete confirmation dialog
    if (state.cameraToDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text("Excluir Camera") },
            text = { Text("Tem certeza que deseja excluir \"${state.cameraToDelete!!.name}\"?") },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDelete) {
                    Text("Cancelar")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            VigiProTopBar(
                title = state.sites.find { it.id == state.selectedSiteId }?.name ?: "VigiPro",
                actions = {
                    // Site selector (only show if 2+ sites)
                    if (state.sites.size > 1) {
                        Box {
                            IconButton(onClick = { showSiteDropdown = true }) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Trocar local")
                            }
                            SiteDropdown(
                                expanded = showSiteDropdown,
                                sites = state.sites.map { site ->
                                    SiteItem(
                                        id = site.id,
                                        name = site.name,
                                        cameraCount = state.cameras.count { it.siteId == site.id },
                                    )
                                },
                                selectedSiteId = state.selectedSiteId,
                                onSiteSelected = { siteId ->
                                    showSiteDropdown = false
                                    viewModel.onSiteSelected(siteId)
                                },
                                onDismiss = { showSiteDropdown = false },
                            )
                        }
                    }

                    // Event timeline
                    IconButton(onClick = viewModel::onEventTimelineClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Eventos")
                    }

                    // Access control
                    IconButton(onClick = viewModel::onAccessControlClick) {
                        Icon(Icons.Default.People, contentDescription = "Controle de acesso")
                    }

                    GridLayoutToggle(
                        currentLayout = state.gridLayout,
                        onLayoutChange = viewModel::onGridLayoutChange,
                    )
                    IconButton(onClick = viewModel::onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuracoes")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onAddCameraClick,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar camera")
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                LoadingIndicator(
                    message = "Carregando cameras...",
                    modifier = Modifier.padding(padding),
                )
            }
            state.cameras.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.VideocamOff,
                    title = "Nenhuma camera",
                    subtitle = "Adicione sua primeira camera para comecar o monitoramento",
                    actionLabel = "Adicionar Camera",
                    onAction = viewModel::onAddCameraClick,
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Health summary banner with animated visibility
                    val onlineCount = state.cameras.count { it.status == CameraStatus.ONLINE }
                    val totalCount = state.cameras.size
                    val allHealthy = onlineCount == totalCount && totalCount > 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = tween(300))
                            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Animated health dot
                        val dotColor = if (allHealthy) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                        Text(
                            text = "$onlineCount/$totalCount online",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        AnimatedVisibility(
                            visible = allHealthy,
                            enter = fadeIn(tween(500)) + expandVertically(),
                            exit = fadeOut(tween(300)) + shrinkVertically(),
                        ) {
                            Row {
                                Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                                Text(
                                    text = "Tudo funcionando",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(state.gridLayout.columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Dimens.GridCellSpacing),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GridCellSpacing),
                        verticalArrangement = Arrangement.spacedBy(Dimens.GridCellSpacing),
                    ) {
                        items(state.cameras, key = { it.id }) { camera ->
                            var showMenu by remember { mutableStateOf(false) }

                            Box(modifier = Modifier.animateItem()) {
                                CameraCard(
                                    name = camera.name,
                                    status = camera.status.toConnectionStatus(),
                                    thumbnailUrl = camera.thumbnailUrl,
                                    onClick = { viewModel.onCameraClick(camera.id) },
                                    onLongClick = { showMenu = true },
                                    ptzCapable = camera.ptzCapable,
                                    audioCapable = camera.audioCapable,
                                )

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Editar") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.onEditCameraClick(camera.id)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Excluir") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.onDeleteCameraClick(camera.id)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                                        colors = MenuDefaults.itemColors(
                                            textColor = MaterialTheme.colorScheme.error,
                                            leadingIconColor = MaterialTheme.colorScheme.error,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun CameraStatus.toConnectionStatus(): ConnectionStatus = when (this) {
    CameraStatus.ONLINE -> ConnectionStatus.ONLINE
    CameraStatus.OFFLINE -> ConnectionStatus.OFFLINE
    CameraStatus.ERROR -> ConnectionStatus.ERROR
}
