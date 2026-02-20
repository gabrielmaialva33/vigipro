package com.vigipro.feature.accesscontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.AuthRepository
import com.vigipro.core.data.repository.InvitationRepository
import com.vigipro.core.data.repository.SiteRepository
import com.vigipro.core.model.Invitation
import com.vigipro.core.model.Site
import com.vigipro.core.model.SiteMember
import com.vigipro.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

data class AccessControlState(
    val sites: List<Site> = emptyList(),
    val selectedSiteId: String? = null,
    val members: List<SiteMember> = emptyList(),
    val invitations: List<Invitation> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    // Create invite
    val showCreateInvite: Boolean = false,
    val inviteRole: UserRole = UserRole.VIEWER,
    val inviteExpiresHours: Int = 24,
    val inviteMaxUses: Int = 1,
    // Time window (for TIME_RESTRICTED)
    val timeStart: String = "",
    val timeEnd: String = "",
    val selectedDays: List<Int> = listOf(1, 2, 3, 4, 5),
    // Invite result
    val createdInviteCode: String? = null,
    // Redeem
    val redeemCode: String = "",
    val isRedeeming: Boolean = false,
)

sealed interface AccessControlSideEffect {
    data class ShowSnackbar(val message: String) : AccessControlSideEffect
    data object NavigateBack : AccessControlSideEffect
}

@HiltViewModel
class AccessControlViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val invitationRepository: InvitationRepository,
    private val authRepository: AuthRepository,
) : ViewModel(), ContainerHost<AccessControlState, AccessControlSideEffect> {

    override val container = viewModelScope.container<AccessControlState, AccessControlSideEffect>(
        AccessControlState(),
    ) {
        observeSites()
    }

    private fun observeSites() = intent {
        siteRepository.getUserSites().collect { sites ->
            val selectedId = state.selectedSiteId ?: sites.firstOrNull()?.id
            reduce { state.copy(sites = sites, selectedSiteId = selectedId) }
            if (selectedId != null) {
                loadSiteData(selectedId)
            } else {
                reduce { state.copy(isLoading = false) }
            }
        }
    }

    private fun loadSiteData(siteId: String) = intent {
        reduce { state.copy(isLoading = true) }

        val membersResult = siteRepository.getSiteMembers(siteId)
        val invitationsResult = invitationRepository.getInvitationsForSite(siteId)

        reduce {
            state.copy(
                members = membersResult.getOrDefault(emptyList()),
                invitations = invitationsResult.getOrDefault(emptyList()),
                isLoading = false,
            )
        }
    }

    fun onSiteSelected(siteId: String) = intent {
        reduce { state.copy(selectedSiteId = siteId) }
        loadSiteData(siteId)
    }

    fun onTabSelected(tab: Int) = intent {
        reduce { state.copy(selectedTab = tab) }
    }

    // Create invite
    fun onShowCreateInvite() = intent {
        reduce { state.copy(showCreateInvite = true, createdInviteCode = null) }
    }

    fun onDismissCreateInvite() = intent {
        reduce { state.copy(showCreateInvite = false) }
    }

    fun onInviteRoleChange(role: UserRole) = intent {
        reduce { state.copy(inviteRole = role) }
    }

    fun onInviteExpiresChange(hours: Int) = intent {
        reduce { state.copy(inviteExpiresHours = hours) }
    }

    fun onInviteMaxUsesChange(maxUses: Int) = intent {
        reduce { state.copy(inviteMaxUses = maxUses) }
    }

    fun onTimeStartChange(time: String) = intent {
        reduce { state.copy(timeStart = time) }
    }

    fun onTimeEndChange(time: String) = intent {
        reduce { state.copy(timeEnd = time) }
    }

    fun onSelectedDaysChange(days: List<Int>) = intent {
        reduce { state.copy(selectedDays = days) }
    }

    fun onCreateInvite() = intent {
        val siteId = state.selectedSiteId ?: return@intent

        val result = invitationRepository.createInvitation(
            siteId = siteId,
            role = state.inviteRole,
            maxUses = state.inviteMaxUses,
            expiresInHours = state.inviteExpiresHours,
        )

        result.onSuccess { invitation ->
            reduce {
                state.copy(
                    showCreateInvite = false,
                    createdInviteCode = invitation.inviteCode,
                    invitations = state.invitations + invitation,
                )
            }
            postSideEffect(AccessControlSideEffect.ShowSnackbar("Convite criado"))
        }.onFailure { error ->
            postSideEffect(
                AccessControlSideEffect.ShowSnackbar(error.message ?: "Erro ao criar convite"),
            )
        }
    }

    fun onDismissInviteResult() = intent {
        reduce { state.copy(createdInviteCode = null) }
    }

    fun onDeleteInvitation(invitationId: String) = intent {
        invitationRepository.deleteInvitation(invitationId).onSuccess {
            reduce {
                state.copy(invitations = state.invitations.filter { it.id != invitationId })
            }
            postSideEffect(AccessControlSideEffect.ShowSnackbar("Convite removido"))
        }
    }

    fun onRemoveMember(memberId: String) = intent {
        siteRepository.removeMember(memberId).onSuccess {
            reduce {
                state.copy(members = state.members.filter { it.id != memberId })
            }
            postSideEffect(AccessControlSideEffect.ShowSnackbar("Membro removido"))
        }
    }

    // Redeem invite
    fun onRedeemCodeChange(code: String) = intent {
        reduce { state.copy(redeemCode = code) }
    }

    fun onRedeemInvite() = intent {
        val code = state.redeemCode.trim()
        if (code.isBlank()) return@intent

        reduce { state.copy(isRedeeming = true) }

        invitationRepository.redeemInvitation(code)
            .onSuccess { siteId ->
                reduce { state.copy(isRedeeming = false, redeemCode = "") }
                postSideEffect(AccessControlSideEffect.ShowSnackbar("Convite resgatado com sucesso!"))
                siteRepository.syncSites()
                loadSiteData(siteId)
            }
            .onFailure { error ->
                reduce { state.copy(isRedeeming = false) }
                postSideEffect(
                    AccessControlSideEffect.ShowSnackbar(error.message ?: "Erro ao resgatar convite"),
                )
            }
    }

    fun onBack() = intent {
        postSideEffect(AccessControlSideEffect.NavigateBack)
    }
}
