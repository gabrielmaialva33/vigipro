package com.vigipro.feature.player

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.preferences.UserPreferencesRepository
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.model.Camera
import com.vigipro.feature.player.ptz.OnvifPtzClient
import com.vigipro.feature.player.ptz.PtzPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

@Stable
data class PlayerState(
    val camera: Camera? = null,
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isFullscreen: Boolean = false,
    val isAudioEnabled: Boolean = false,
    val showControls: Boolean = true,
    val showPtzControls: Boolean = false,
    val showStreamInfo: Boolean = false,
    val ptzPresets: List<PtzPreset> = emptyList(),
    val isPtzConnected: Boolean = false,
    val errorMessage: String? = null,
    val codec: String = "",
    val resolution: String = "",
)

sealed interface PlayerSideEffect {
    data object NavigateBack : PlayerSideEffect
    data class ToggleFullscreen(val isFullscreen: Boolean) : PlayerSideEffect
    data object RequestSnapshot : PlayerSideEffect
    data class ShareSnapshot(val uri: Uri) : PlayerSideEffect
    data class ShowSnackbar(val message: String) : PlayerSideEffect
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cameraRepository: CameraRepository,
    private val ptzClient: OnvifPtzClient,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel(), ContainerHost<PlayerState, PlayerSideEffect> {

    private val cameraId: String = checkNotNull(savedStateHandle["cameraId"])
    private var controlsHideJob: Job? = null

    override val container = viewModelScope.container<PlayerState, PlayerSideEffect>(PlayerState()) {
        loadCamera()
        loadAudioPreference()
    }

    override fun onCleared() {
        super.onCleared()
        ptzClient.disconnect()
    }

    private fun loadCamera() = intent {
        val camera = cameraRepository.getCameraById(cameraId)
        if (camera == null) {
            reduce { state.copy(isLoading = false, errorMessage = "Camera nao encontrada") }
        } else {
            reduce { state.copy(camera = camera, isLoading = false) }
            if (camera.ptzCapable && camera.onvifAddress != null) {
                connectPtz(camera)
            }
        }
    }

    private fun loadAudioPreference() = intent {
        preferencesRepository.userPreferences.collect { prefs ->
            reduce {
                if (!state.isPlaying) {
                    state.copy(isAudioEnabled = prefs.audioEnabledByDefault)
                } else {
                    state
                }
            }
        }
    }

    private fun connectPtz(camera: Camera) {
        viewModelScope.launch {
            try {
                val connected = ptzClient.connect(
                    onvifAddress = camera.onvifAddress ?: return@launch,
                    username = camera.username ?: "",
                    password = "",
                    streamProfileToken = camera.streamProfile,
                )
                intent {
                    reduce { state.copy(isPtzConnected = connected) }
                    if (connected) {
                        val presetsResult = ptzClient.getPresets()
                        presetsResult.onSuccess { presets ->
                            reduce { state.copy(ptzPresets = presets) }
                        }
                    }
                }
            } catch (_: Exception) {
                intent { reduce { state.copy(isPtzConnected = false) } }
            }
        }
    }

    // Player callbacks
    fun onPlaybackStarted() = intent {
        reduce { state.copy(isPlaying = true, isBuffering = false, errorMessage = null) }
        scheduleControlsHide()
    }

    fun onBuffering() = intent {
        reduce { state.copy(isBuffering = true) }
    }

    fun onPlaybackError(message: String) = intent {
        reduce { state.copy(isPlaying = false, isBuffering = false, errorMessage = message) }
    }

    fun onVideoInfoAvailable(codec: String, resolution: String) = intent {
        reduce { state.copy(codec = codec, resolution = resolution) }
    }

    // Controls
    fun onControlsTap() = intent {
        val newVisibility = !state.showControls
        reduce { state.copy(showControls = newVisibility) }
        if (newVisibility) scheduleControlsHide()
    }

    private fun scheduleControlsHide() {
        controlsHideJob?.cancel()
        controlsHideJob = viewModelScope.launch {
            delay(3000L)
            intent { reduce { state.copy(showControls = false) } }
        }
    }

    fun onPlayPause() = intent {
        controlsHideJob?.cancel()
        scheduleControlsHide()
    }

    fun onToggleFullscreen() = intent {
        val newFullscreen = !state.isFullscreen
        reduce { state.copy(isFullscreen = newFullscreen) }
        postSideEffect(PlayerSideEffect.ToggleFullscreen(newFullscreen))
    }

    fun onToggleAudio() = intent {
        reduce { state.copy(isAudioEnabled = !state.isAudioEnabled) }
    }

    fun onSnapshot() = intent {
        postSideEffect(PlayerSideEffect.RequestSnapshot)
    }

    fun onSnapshotSaved(uri: Uri) = intent {
        postSideEffect(PlayerSideEffect.ShowSnackbar("Captura salva"))
        postSideEffect(PlayerSideEffect.ShareSnapshot(uri))
    }

    fun onSnapshotFailed() = intent {
        postSideEffect(PlayerSideEffect.ShowSnackbar("Falha ao capturar imagem"))
    }

    fun onTogglePtzControls() = intent {
        reduce { state.copy(showPtzControls = !state.showPtzControls) }
        controlsHideJob?.cancel()
    }

    fun onToggleStreamInfo() = intent {
        reduce { state.copy(showStreamInfo = !state.showStreamInfo) }
    }

    // PTZ commands
    fun onPtzMove(x: Float, y: Float, z: Float) {
        viewModelScope.launch { ptzClient.continuousMove(x, y, z) }
    }

    fun onPtzStop() {
        viewModelScope.launch { ptzClient.stop() }
    }

    fun onPtzPreset(presetToken: String) {
        viewModelScope.launch { ptzClient.gotoPreset(presetToken) }
    }

    fun onBack() = intent {
        if (state.isFullscreen) {
            reduce { state.copy(isFullscreen = false) }
            postSideEffect(PlayerSideEffect.ToggleFullscreen(false))
        } else {
            postSideEffect(PlayerSideEffect.NavigateBack)
        }
    }

    fun onRetry() = intent {
        reduce { state.copy(errorMessage = null, isBuffering = true) }
    }
}
