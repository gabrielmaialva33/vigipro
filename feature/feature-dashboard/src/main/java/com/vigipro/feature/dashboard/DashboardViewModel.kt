package com.vigipro.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.monitor.CameraStatusMonitor
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.model.Camera
import com.vigipro.core.ui.components.GridLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

data class DashboardState(
    val cameras: List<Camera> = emptyList(),
    val gridLayout: GridLayout = GridLayout.GRID_2X2,
    val isLoading: Boolean = true,
)

sealed interface DashboardSideEffect {
    data class NavigateToPlayer(val cameraId: String) : DashboardSideEffect
    data object NavigateToAddCamera : DashboardSideEffect
    data object NavigateToSettings : DashboardSideEffect
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val statusMonitor: CameraStatusMonitor,
) : ViewModel(), ContainerHost<DashboardState, DashboardSideEffect> {

    override val container = viewModelScope.container<DashboardState, DashboardSideEffect>(DashboardState()) {
        observeCameras()
        statusMonitor.start(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        statusMonitor.stop()
    }

    private fun observeCameras() = intent {
        cameraRepository.getAllCameras().collect { cameras ->
            reduce {
                state.copy(cameras = cameras, isLoading = false)
            }
        }
    }

    fun onCameraClick(cameraId: String) = intent {
        postSideEffect(DashboardSideEffect.NavigateToPlayer(cameraId))
    }

    fun onAddCameraClick() = intent {
        postSideEffect(DashboardSideEffect.NavigateToAddCamera)
    }

    fun onSettingsClick() = intent {
        postSideEffect(DashboardSideEffect.NavigateToSettings)
    }

    fun onGridLayoutChange(layout: GridLayout) = intent {
        reduce { state.copy(gridLayout = layout) }
    }
}
