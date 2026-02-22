package com.vigipro.feature.accesscontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.vigipro.core.model.Invitation
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.feature.accesscontrol.ui.CreateInviteSheet
import com.vigipro.feature.accesscontrol.ui.InviteResultSheet
import com.vigipro.feature.accesscontrol.ui.MemberListItem
import com.vigipro.feature.accesscontrol.ui.RedeemInviteSection
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessControlScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccessControlViewModel = hiltViewModel(),
) {
    val state = viewModel.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is AccessControlSideEffect.ShowSnackbar -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            AccessControlSideEffect.NavigateBack -> onBack()
        }
    }

    // Bottom sheets
    if (state.showCreateInvite) {
        CreateInviteSheet(
            inviteRole = state.inviteRole,
            inviteExpiresHours = state.inviteExpiresHours,
            inviteMaxUses = state.inviteMaxUses,
            timeStart = state.timeStart,
            timeEnd = state.timeEnd,
            selectedDays = state.selectedDays,
            onRoleChange = viewModel::onInviteRoleChange,
            onExpiresChange = viewModel::onInviteExpiresChange,
            onMaxUsesChange = viewModel::onInviteMaxUsesChange,
            onTimeStartChange = viewModel::onTimeStartChange,
            onTimeEndChange = viewModel::onTimeEndChange,
            onDaysChange = viewModel::onSelectedDaysChange,
            onCreate = viewModel::onCreateInvite,
            onDismiss = viewModel::onDismissCreateInvite,
        )
    }

    if (state.createdInviteCode != null) {
        InviteResultSheet(
            inviteCode = state.createdInviteCode,
            onDismiss = viewModel::onDismissInviteResult,
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Controle de Acesso") },
                navigationIcon = {
                    IconButton(onClick = viewModel::onBack) {
                        Icon(
                            PhosphorIcons.Regular.ArrowLeft,
                            contentDescription = "Voltar",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.selectedTab == 1) {
                FloatingActionButton(onClick = viewModel::onShowCreateInvite) {
                    Icon(PhosphorIcons.Regular.Plus, contentDescription = "Criar convite")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Membros") },
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Convites") },
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    text = { Text("Resgatar") },
                )
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (state.selectedTab) {
                    0 -> MembersTab(
                        state = state,
                        onRemoveMember = viewModel::onRemoveMember,
                    )
                    1 -> InvitationsTab(
                        state = state,
                        onDeleteInvitation = viewModel::onDeleteInvitation,
                    )
                    2 -> RedeemInviteSection(
                        code = state.redeemCode,
                        isRedeeming = state.isRedeeming,
                        onCodeChange = viewModel::onRedeemCodeChange,
                        onRedeem = viewModel::onRedeemInvite,
                    )
                }
            }
        }
    }
}

@Composable
private fun MembersTab(
    state: AccessControlState,
    onRemoveMember: (String) -> Unit,
) {
    if (state.members.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Nenhum membro encontrado",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.members, key = { it.id }) { member ->
                MemberListItem(
                    member = member,
                    canRemove = true,
                    onRemove = { onRemoveMember(member.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun InvitationsTab(
    state: AccessControlState,
    onDeleteInvitation: (String) -> Unit,
) {
    if (state.invitations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Nenhum convite ativo",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.invitations, key = { it.id }) { invitation ->
                InvitationListItem(
                    invitation = invitation,
                    onDelete = { onDeleteInvitation(invitation.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun InvitationListItem(
    invitation: Invitation,
    onDelete: () -> Unit,
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
                text = invitation.inviteCode,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
            Text(
                text = "${invitation.role.name} · ${invitation.usesCount}/${invitation.maxUses} usos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                PhosphorIcons.Regular.Trash,
                contentDescription = "Deletar convite",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
