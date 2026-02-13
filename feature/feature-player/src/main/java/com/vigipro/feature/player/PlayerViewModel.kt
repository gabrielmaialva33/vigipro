package com.vigipro.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.model.Camera
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

data class PlayerState(
    val camera: Camera? = null,
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PlayerSideEffect {
    data object NavigateBack : PlayerSideEffect
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cameraRepository: CameraRepository,
) : ViewModel(), ContainerHost<PlayerState, PlayerSideEffect> {

    private val cameraId: String = checkNotNull(savedStateHandle["cameraId"])

    override val container = viewModelScope.container<PlayerState, PlayerSideEffect>(PlayerState()) {
        loadCamera()
    }

    private fun loadCamera() = intent {
        val camera = cameraRepository.getCameraById(cameraId)
        if (camera == null) {
            reduce { state.copy(isLoading = false, errorMessage = "Camera nao encontrada") }
        } else {
            reduce { state.copy(camera = camera, isLoading = false) }
        }
    }

    fun onPlaybackStarted() = intent {
        reduce { state.copy(isPlaying = true, isBuffering = false, errorMessage = null) }
    }

    fun onBuffering() = intent {
        reduce { state.copy(isBuffering = true) }
    }

    fun onPlaybackError(message: String) = intent {
        reduce { state.copy(isPlaying = false, isBuffering = false, errorMessage = message) }
    }

    fun onBack() = intent {
        postSideEffect(PlayerSideEffect.NavigateBack)
    }

    fun onRetry() = intent {
        reduce { state.copy(errorMessage = null, isBuffering = true) }
    }
}
