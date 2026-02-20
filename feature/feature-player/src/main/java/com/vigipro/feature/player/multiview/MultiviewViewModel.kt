package com.vigipro.feature.player.multiview

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.model.Camera
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

enum class MultiviewLayout(val label: String, val maxCells: Int) {
    GRID_2X2("2x2", 4),
    GRID_1_PLUS_3("1+3", 4),
    SINGLE("Simples", 1),
}

@Stable
data class MultiviewState(
    val cameras: List<Camera> = emptyList(),
    val selectedCameras: List<Camera> = emptyList(),
    val layoutMode: MultiviewLayout = MultiviewLayout.GRID_2X2,
    val isLoading: Boolean = true,
    val maxCameras: Int = 4,
)

sealed interface MultiviewSideEffect {
    data class NavigateToPlayer(val cameraId: String) : MultiviewSideEffect
    data object NavigateBack : MultiviewSideEffect
}

@HiltViewModel
class MultiviewViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    @ApplicationContext private val context: Context,
) : ViewModel(), ContainerHost<MultiviewState, MultiviewSideEffect> {

    private val isLowRam: Boolean by lazy {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.isLowRamDevice
    }

    override val container = viewModelScope.container<MultiviewState, MultiviewSideEffect>(MultiviewState()) {
        val maxCams = if (isLowRam) 2 else 4
        reduce { state.copy(maxCameras = maxCams) }
        loadCameras()
    }

    private fun loadCameras() = intent {
        cameraRepository.getAllCameras().collect { cameras ->
            reduce { state.copy(cameras = cameras, isLoading = false) }
        }
    }

    fun onCameraSelect(camera: Camera) = intent {
        if (state.selectedCameras.size >= state.maxCameras) return@intent
        if (state.selectedCameras.any { it.id == camera.id }) return@intent
        reduce { state.copy(selectedCameras = state.selectedCameras + camera) }
    }

    fun onCameraDeselect(camera: Camera) = intent {
        reduce { state.copy(selectedCameras = state.selectedCameras.filter { it.id != camera.id }) }
    }

    fun onCellTap(cameraId: String) = intent {
        postSideEffect(MultiviewSideEffect.NavigateToPlayer(cameraId))
    }

    fun onLayoutChange(layout: MultiviewLayout) = intent {
        reduce {
            val trimmed = if (layout == MultiviewLayout.SINGLE && state.selectedCameras.size > 1) {
                state.selectedCameras.take(1)
            } else {
                state.selectedCameras.take(layout.maxCells.coerceAtMost(state.maxCameras))
            }
            state.copy(layoutMode = layout, selectedCameras = trimmed)
        }
    }

    fun onBack() = intent {
        postSideEffect(MultiviewSideEffect.NavigateBack)
    }
}
