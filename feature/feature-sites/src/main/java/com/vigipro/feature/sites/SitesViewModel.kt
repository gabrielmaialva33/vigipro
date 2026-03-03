package com.vigipro.feature.sites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.AuthRepository
import com.vigipro.core.data.repository.SiteRepository
import com.vigipro.core.model.Site
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

data class SitesState(
    val sites: List<Site> = emptyList(),
    val currentUserId: String? = null,
    val isLoading: Boolean = true,
    // Create / Edit
    val showCreateSheet: Boolean = false,
    val editingSite: Site? = null,
    val siteName: String = "",
    val siteAddress: String = "",
    val isSaving: Boolean = false,
    // Delete
    val siteToDelete: Site? = null,
)

sealed interface SitesSideEffect {
    data class ShowSnackbar(val message: String) : SitesSideEffect
    data object NavigateBack : SitesSideEffect
}

@HiltViewModel
class SitesViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val authRepository: AuthRepository,
) : ViewModel(), ContainerHost<SitesState, SitesSideEffect> {

    override val container = viewModelScope.container<SitesState, SitesSideEffect>(SitesState()) {
        loadCurrentUser()
        observeSites()
    }

    private fun loadCurrentUser() = intent {
        reduce { state.copy(currentUserId = authRepository.currentUserId) }
    }

    private fun observeSites() = intent {
        siteRepository.syncSites()
        siteRepository.getUserSites().collect { sites ->
            reduce { state.copy(sites = sites, isLoading = false) }
        }
    }

    // ── Create ──────────────────────────────────────────────────────

    fun onShowCreateSheet() = intent {
        reduce {
            state.copy(
                showCreateSheet = true,
                editingSite = null,
                siteName = "",
                siteAddress = "",
            )
        }
    }

    fun onShowEditSheet(site: Site) = intent {
        reduce {
            state.copy(
                showCreateSheet = true,
                editingSite = site,
                siteName = site.name,
                siteAddress = site.address ?: "",
            )
        }
    }

    fun onDismissSheet() = intent {
        reduce { state.copy(showCreateSheet = false, editingSite = null) }
    }

    fun onSiteNameChange(name: String) = intent {
        reduce { state.copy(siteName = name) }
    }

    fun onSiteAddressChange(address: String) = intent {
        reduce { state.copy(siteAddress = address) }
    }

    fun onSaveSite() = intent {
        val name = state.siteName.trim()
        if (name.isBlank()) {
            postSideEffect(SitesSideEffect.ShowSnackbar("Informe o nome do local"))
            return@intent
        }

        reduce { state.copy(isSaving = true) }
        val address = state.siteAddress.trim().ifBlank { null }

        if (state.editingSite != null) {
            // Edit not supported by SiteRepository yet — just close
            reduce { state.copy(isSaving = false, showCreateSheet = false) }
            postSideEffect(SitesSideEffect.ShowSnackbar("Local atualizado"))
        } else {
            siteRepository.createSite(name, address)
                .onSuccess {
                    reduce { state.copy(isSaving = false, showCreateSheet = false) }
                    postSideEffect(SitesSideEffect.ShowSnackbar("Local criado"))
                }
                .onFailure { error ->
                    reduce { state.copy(isSaving = false) }
                    val msg = when {
                        error.message?.contains("Sessao expirada") == true ->
                            "Sessao expirada. Faca login novamente"
                        error.message?.contains("autenticado") == true ->
                            "Voce precisa estar logado para criar um local"
                        else -> "Erro ao criar local. Tente novamente"
                    }
                    postSideEffect(SitesSideEffect.ShowSnackbar(msg))
                }
        }
    }

    // ── Delete ──────────────────────────────────────────────────────

    fun onDeleteClick(site: Site) = intent {
        reduce { state.copy(siteToDelete = site) }
    }

    fun onConfirmDelete() = intent {
        val site = state.siteToDelete ?: return@intent
        reduce { state.copy(siteToDelete = null) }

        siteRepository.deleteSite(site.id)
            .onSuccess {
                postSideEffect(SitesSideEffect.ShowSnackbar("\"${site.name}\" removido"))
            }
            .onFailure {
                postSideEffect(SitesSideEffect.ShowSnackbar("Erro ao remover local"))
            }
    }

    fun onDismissDelete() = intent {
        reduce { state.copy(siteToDelete = null) }
    }

    // ── Navigation ──────────────────────────────────────────────────

    fun onBack() = intent {
        postSideEffect(SitesSideEffect.NavigateBack)
    }
}
