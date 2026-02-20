package com.vigipro.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vigipro.core.model.CameraStatus
import com.vigipro.core.ui.components.CameraCard
import com.vigipro.core.ui.components.ConnectionStatus
import com.vigipro.core.ui.components.EmptyState
import com.vigipro.core.ui.components.LoadingIndicator
import com.vigipro.core.ui.theme.Dimens
import kotlinx.coroutines.launch
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
    onNavigateToMultiview: () -> Unit = {},
    onNavigateToRecordings: () -> Unit = {},
    onNavigateToAlertDigest: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is DashboardSideEffect.NavigateToPlayer -> onNavigateToPlayer(sideEffect.cameraId)
            DashboardSideEffect.NavigateToAddCamera -> onNavigateToAddCamera()
            is DashboardSideEffect.NavigateToEditCamera -> onNavigateToEditCamera(sideEffect.cameraId)
            DashboardSideEffect.NavigateToSettings -> onNavigateToSettings()
            DashboardSideEffect.NavigateToAccessControl -> onNavigateToAccessControl()
            DashboardSideEffect.NavigateToEventTimeline -> onNavigateToEventTimeline()
            DashboardSideEffect.NavigateToMultiview -> onNavigateToMultiview()
            DashboardSideEffect.NavigateToRecordings -> onNavigateToRecordings()
            DashboardSideEffect.NavigateToAlertDigest -> onNavigateToAlertDigest()
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                userEmail = state.userEmail,
                onNavigateToMultiview = {
                    scope.launch { drawerState.close() }
                    viewModel.onMultiviewClick()
                },
                onNavigateToRecordings = {
                    scope.launch { drawerState.close() }
                    viewModel.onRecordingsClick()
                },
                onNavigateToEventTimeline = {
                    scope.launch { drawerState.close() }
                    viewModel.onEventTimelineClick()
                },
                onNavigateToAlertDigest = {
                    scope.launch { drawerState.close() }
                    viewModel.onAlertDigestClick()
                },
                onNavigateToAccessControl = {
                    scope.launch { drawerState.close() }
                    viewModel.onAccessControlClick()
                },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    viewModel.onSettingsClick()
                },
            )
        },
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "VigiPro",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                letterSpacing = (-1).sp,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        ) {
                            Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = viewModel::onMultiviewClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        ) {
                            Icon(Icons.Default.GridView, "Multiview", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = viewModel::onEventTimelineClick,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        ) {
                            Icon(Icons.Default.Notifications, "Notificacoes", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = viewModel::onAddCameraClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(64.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar camera", modifier = Modifier.size(32.dp))
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
                        title = "Nenhuma Camera Detectada",
                        subtitle = "Sua central de monitoramento esta vazia. Adicione sua primeira camera IP ou ONVIF para comecar a vigiar seu espaco.",
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
                        // Health summary banner
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

                                Box(modifier = Modifier.animateItem(
                                    fadeInSpec = tween(500),
                                    fadeOutSpec = tween(300),
                                    placementSpec = tween(400, easing = FastOutSlowInEasing),
                                )) {
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
}

@Composable
private fun DrawerContent(
    userEmail: String?,
    onNavigateToMultiview: () -> Unit,
    onNavigateToRecordings: () -> Unit,
    onNavigateToEventTimeline: () -> Unit,
    onNavigateToAlertDigest: () -> Unit,
    onNavigateToAccessControl: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
    ) {
        // Profile header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            // Avatar com iniciais
            val initials = userEmail
                ?.substringBefore("@")
                ?.take(2)
                ?.uppercase()
                ?: "VP"

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Usuario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = userEmail ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation items
        DrawerNavItem(
            icon = Icons.Default.GridView,
            label = "Multiview",
            onClick = onNavigateToMultiview,
        )
        DrawerNavItem(
            icon = Icons.Default.VideoLibrary,
            label = "Gravacoes",
            onClick = onNavigateToRecordings,
        )
        DrawerNavItem(
            icon = Icons.Default.Notifications,
            label = "Timeline de Eventos",
            onClick = onNavigateToEventTimeline,
        )
        DrawerNavItem(
            icon = Icons.Default.Summarize,
            label = "Resumo de Alertas",
            onClick = onNavigateToAlertDigest,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        DrawerNavItem(
            icon = Icons.Default.People,
            label = "Controle de Acesso",
            onClick = onNavigateToAccessControl,
        )
        DrawerNavItem(
            icon = Icons.Default.Settings,
            label = "Configuracoes",
            onClick = onNavigateToSettings,
        )
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

private fun CameraStatus.toConnectionStatus(): ConnectionStatus = when (this) {
    CameraStatus.ONLINE -> ConnectionStatus.ONLINE
    CameraStatus.OFFLINE -> ConnectionStatus.OFFLINE
    CameraStatus.ERROR -> ConnectionStatus.ERROR
}
