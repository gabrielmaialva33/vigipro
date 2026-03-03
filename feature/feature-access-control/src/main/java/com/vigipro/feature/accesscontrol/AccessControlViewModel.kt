package com.vigipro.feature.accesscontrol

import androidx.lifecycle.SavedStateHandle
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
    data object NavigateToSites : AccessControlSideEffect
}

@HiltViewModel
class AccessControlViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val siteRepository: SiteRepository,
    private val invitationRepository: InvitationRepository,
    private val authRepository: AuthRepository,
) : ViewModel(), ContainerHost<AccessControlState, AccessControlSideEffect> {

    private val initialRedeemCode: String = savedStateHandle.get<String>("code") ?: ""

    override val container = viewModelScope.container<AccessControlState, AccessControlSideEffect>(
        AccessControlState(),
    ) {
        if (initialRedeemCode.isNotBlank()) {
            reduce { state.copy(redeemCode = initialRedeemCode, selectedTab = 2) }
        }
        observeSites()
    }

    private fun observeSites() = intent {
        siteRepository.syncSites()
        siteRepository.getUserSites().collect { sites ->
            val selectedId = state.selectedSiteId ?: sites.firstOrNull()?.id
            reduce { state.copy(sites = sites, selectedSiteId = selectedId, isLoading = false) }
            if (selectedId != null) {
                loadSiteData(selectedId)
            }
        }
    }

    fun onNavigateToSites() = intent {
        postSideEffect(AccessControlSideEffect.NavigateToSites)
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
        if (state.selectedSiteId == null) {
            postSideEffect(
                AccessControlSideEffect.ShowSnackbar("Selecione um local primeiro"),
            )
            return@intent
        }
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
        val siteId = state.selectedSiteId
        if (siteId == null) {
            postSideEffect(
                AccessControlSideEffect.ShowSnackbar("Selecione um local primeiro"),
            )
            return@intent
        }

        val isTimeRestricted = state.inviteRole == UserRole.TIME_RESTRICTED
        val result = invitationRepository.createInvitation(
            siteId = siteId,
            role = state.inviteRole,
            timeStart = state.timeStart.takeIf { isTimeRestricted && it.isNotBlank() },
            timeEnd = state.timeEnd.takeIf { isTimeRestricted && it.isNotBlank() },
            daysOfWeek = state.selectedDays.takeIf { isTimeRestricted },
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
            val msg = when {
                error.message?.contains("Sessao expirada") == true ->
                    "Sessao expirada. Faca login novamente"
                else -> "Erro ao criar convite. Tente novamente"
            }
            postSideEffect(AccessControlSideEffect.ShowSnackbar(msg))
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
                val message = when {
                    error.message?.contains("Sessao expirada") == true ->
                        "Sessao expirada. Faca login novamente"
                    error.message?.contains("ja e membro") == true -> "Voce ja e membro deste site"
                    error.message?.contains("nao encontrado") == true -> "Convite nao encontrado"
                    error.message?.contains("expirado") == true -> "Convite expirado"
                    error.message?.contains("esgotado") == true -> "Convite esgotado"
                    else -> "Erro ao resgatar convite. Verifique o codigo"
                }
                postSideEffect(AccessControlSideEffect.ShowSnackbar(message))
            }
    }

    fun onBack() = intent {
        postSideEffect(AccessControlSideEffect.NavigateBack)
    }
}
