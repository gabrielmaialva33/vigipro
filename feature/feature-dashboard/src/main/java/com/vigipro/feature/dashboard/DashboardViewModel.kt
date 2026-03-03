package com.vigipro.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.monitor.CameraStatusMonitor
import com.vigipro.core.data.repository.AuthRepository
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.repository.CloudRepository
import com.vigipro.core.data.repository.SiteRepository
import com.vigipro.core.data.seed.DevSeedHelper
import com.vigipro.core.data.sync.CloudSyncManager
import com.vigipro.core.data.sync.LocalCameraMigrator
import com.vigipro.core.model.Camera
import kotlinx.coroutines.flow.first
import com.vigipro.core.model.Site
import com.vigipro.core.ui.components.GridLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

data class DashboardState(
    val cameras: List<Camera> = emptyList(),
    val demoCameras: List<Camera> = emptyList(),
    val sites: List<Site> = emptyList(),
    val selectedSiteId: String? = null,
    val gridLayout: GridLayout = GridLayout.GRID_2X2,
    val isLoading: Boolean = true,
    val cameraToDelete: Camera? = null,
    val userEmail: String? = null,
) {
    val allCameras: List<Camera> get() = cameras + demoCameras
}

sealed interface DashboardSideEffect {
    data class NavigateToPlayer(val cameraId: String) : DashboardSideEffect
    data object NavigateToAddCamera : DashboardSideEffect
    data class NavigateToEditCamera(val cameraId: String) : DashboardSideEffect
    data object NavigateToSettings : DashboardSideEffect
    data object NavigateToAccessControl : DashboardSideEffect
    data object NavigateToEventTimeline : DashboardSideEffect
    data object NavigateToMultiview : DashboardSideEffect
    data object NavigateToRecordings : DashboardSideEffect
    data object NavigateToAlertDigest : DashboardSideEffect
    data object NavigateToSites : DashboardSideEffect
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val siteRepository: SiteRepository,
    private val statusMonitor: CameraStatusMonitor,
    private val authRepository: AuthRepository,
    private val cloudRepository: CloudRepository,
    private val devSeedHelper: DevSeedHelper,
    private val cloudSyncManager: CloudSyncManager,
    private val localCameraMigrator: LocalCameraMigrator,
) : ViewModel(), ContainerHost<DashboardState, DashboardSideEffect> {

    override val container = viewModelScope.container<DashboardState, DashboardSideEffect>(DashboardState()) {
        observeSites()
        devSeedHelper.seedIfEmpty()
        migrateLocalCamerasIfNeeded()
        observeCameras()
        loadDemoCameras()
        loadUserInfo()
        statusMonitor.start(viewModelScope)
        syncToCloud()
    }

    private fun loadUserInfo() = intent {
        reduce { state.copy(userEmail = authRepository.currentUserEmail) }
    }

    private fun loadDemoCameras() = intent {
        cloudRepository.fetchDemoCameras()
            .onSuccess { demos ->
                reduce { state.copy(demoCameras = demos) }
            }
    }

    private fun migrateLocalCamerasIfNeeded() = intent {
        try {
            if (!localCameraMigrator.hasLocalCameras()) return@intent
            val firstSite = siteRepository.getUserSites()
                .first()
                .firstOrNull() ?: return@intent
            localCameraMigrator.migrateLocalCameras(firstSite.id)
        } catch (_: Exception) {
            // Migration is best-effort
        }
    }

    private fun syncToCloud() = intent {
        // Background sync — fire and forget, don't block UI
        cloudSyncManager.syncAll()
    }

    override fun onCleared() {
        super.onCleared()
        statusMonitor.stop()
    }

    private fun observeSites() = intent {
        siteRepository.getUserSites().collect { sites ->
            val selectedId = state.selectedSiteId ?: sites.firstOrNull()?.id
            reduce { state.copy(sites = sites, selectedSiteId = selectedId) }
        }
    }

    private fun observeCameras() = intent {
        cameraRepository.getAllCameras().collect { cameras ->
            reduce {
                val filtered = if (state.selectedSiteId != null) {
                    cameras.filter { it.siteId == state.selectedSiteId }
                } else {
                    cameras
                }
                state.copy(cameras = filtered, isLoading = false)
            }
        }
    }

    fun onSiteSelected(siteId: String) = intent {
        // Simply update selected site — observeCameras() already filters reactively
        // based on state.selectedSiteId. Launching a new collect here was a bug:
        // each call stacked a new active collector without cancelling the previous one.
        reduce { state.copy(selectedSiteId = siteId, isLoading = true) }
    }

    fun onCameraClick(cameraId: String) = intent {
        postSideEffect(DashboardSideEffect.NavigateToPlayer(cameraId))
    }

    fun onAddCameraClick() = intent {
        postSideEffect(DashboardSideEffect.NavigateToAddCamera)
    }

    fun onEditCameraClick(cameraId: String) = intent {
        postSideEffect(DashboardSideEffect.NavigateToEditCamera(cameraId))
    }

    fun onDeleteCameraClick(cameraId: String) = intent {
        val camera = state.cameras.find { it.id == cameraId }
        reduce { state.copy(cameraToDelete = camera) }
    }

    fun onConfirmDelete() = intent {
        val camera = state.cameraToDelete ?: return@intent
        cameraRepository.deleteCamera(camera.id)
        reduce { state.copy(cameraToDelete = null) }
    }

    fun onDismissDelete() = intent {
        reduce { state.copy(cameraToDelete = null) }
    }

    fun onSettingsClick() = intent {
        postSideEffect(DashboardSideEffect.NavigateToSettings)
    }

    fun onAccessControlClick() = intent {
        postSideEffect(DashboardSideEffect.NavigateToAccessControl)
    }

    fun onGridLayoutChange(layout: GridLayout) = intent {
        reduce { state.copy(gridLayout = layout) }
    }

    fun onEventTimelineClick() = intent {
        postSideEffect(DashboardSideEffect.NavigateToEventTimeline)
    }

    fun onMultiviewClick() = intent {
        postSideEffect(DashboardSideEffect.NavigateToMultiview)
    }

    fun onRecordingsClick() = intent {
        postSideEffect(DashboardSideEffect.NavigateToRecordings)
    }

    fun onAlertDigestClick() = intent {
        postSideEffect(DashboardSideEffect.NavigateToAlertDigest)
    }

    fun onSitesClick() = intent {
        postSideEffect(DashboardSideEffect.NavigateToSites)
    }
}
