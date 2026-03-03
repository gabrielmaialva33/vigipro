package com.vigipro.feature.sites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vigipro.core.model.Site
import com.vigipro.core.ui.theme.Dimens
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SitesViewModel = hiltViewModel(),
) {
    val state = viewModel.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is SitesSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            SitesSideEffect.NavigateBack -> onBack()
        }
    }

    // Create/Edit bottom sheet
    if (state.showCreateSheet) {
        CreateSiteSheet(
            siteName = state.siteName,
            siteAddress = state.siteAddress,
            isEditing = state.editingSite != null,
            isSaving = state.isSaving,
            onNameChange = viewModel::onSiteNameChange,
            onAddressChange = viewModel::onSiteAddressChange,
            onSave = viewModel::onSaveSite,
            onDismiss = viewModel::onDismissSheet,
        )
    }

    // Delete confirmation
    if (state.siteToDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            icon = { Icon(PhosphorIcons.Regular.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remover Local") },
            text = {
                Text(
                    "Tem certeza que deseja remover \"${state.siteToDelete.name}\"? " +
                        "Todos os membros e convites vinculados serao removidos.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) {
                    Text("Remover", color = MaterialTheme.colorScheme.error)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Meus Locais") },
                navigationIcon = {
                    IconButton(onClick = viewModel::onBack) {
                        Icon(PhosphorIcons.Regular.ArrowLeft, contentDescription = "Voltar")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onShowCreateSheet) {
                Icon(PhosphorIcons.Regular.Plus, contentDescription = "Criar local")
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.sites.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            PhosphorIcons.Regular.MapPin,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingLg))
                        Text(
                            text = "Nenhum local cadastrado",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                        Text(
                            text = "Crie um local para organizar suas cameras e gerenciar acessos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingXl))
                        Button(onClick = viewModel::onShowCreateSheet) {
                            Icon(PhosphorIcons.Regular.Plus, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                            Text("Criar Local")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimens.SpacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
                ) {
                    items(state.sites, key = { it.id }) { site ->
                        SiteCard(
                            site = site,
                            isOwner = site.ownerId == state.currentUserId,
                            onEdit = { viewModel.onShowEditSheet(site) },
                            onDelete = { viewModel.onDeleteClick(site) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteCard(
    site: Site,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingLg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    PhosphorIcons.Regular.MapPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingMd))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = site.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val address = site.address
                    if (!address.isNullOrBlank()) {
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (isOwner) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            PhosphorIcons.Regular.PencilSimple,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            PhosphorIcons.Regular.Trash,
                            contentDescription = "Remover",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMd))

            // Info chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
            ) {
                if (isOwner) {
                    InfoChip(
                        icon = PhosphorIcons.Regular.Crown,
                        text = "Proprietario",
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    InfoChip(
                        icon = PhosphorIcons.Regular.User,
                        text = "Membro",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                if (site.geofenceEnabled) {
                    InfoChip(
                        icon = PhosphorIcons.Regular.Crosshair,
                        text = "${site.geofenceRadius.toInt()}m",
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color,
        )
        Spacer(modifier = Modifier.width(Dimens.SpacingXs))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSiteSheet(
    siteName: String,
    siteAddress: String,
    isEditing: Boolean,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingXl, vertical = Dimens.SpacingLg),
        ) {
            Text(
                text = if (isEditing) "Editar Local" else "Criar Local",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            OutlinedTextField(
                value = siteName,
                onValueChange = onNameChange,
                label = { Text("Nome do local") },
                placeholder = { Text("Ex: Casa, Escritorio, Loja...") },
                leadingIcon = { Icon(PhosphorIcons.Regular.MapPin, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            OutlinedTextField(
                value = siteAddress,
                onValueChange = onAddressChange,
                label = { Text("Endereco (opcional)") },
                placeholder = { Text("Rua, numero, cidade...") },
                leadingIcon = { Icon(PhosphorIcons.Regular.NavigationArrow, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = siteName.isNotBlank() && !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                }
                Text(if (isEditing) "Salvar" else "Criar Local")
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))
        }
    }
}
