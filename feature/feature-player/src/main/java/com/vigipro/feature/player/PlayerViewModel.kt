package com.vigipro.feature.player

import android.net.Uri
import android.view.SurfaceView
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.detection.FrameCaptureHelper
import com.vigipro.core.data.detection.ObjectDetectionEngine
import com.vigipro.core.data.notification.CameraNotificationHelper
import com.vigipro.core.data.preferences.UserPreferencesRepository
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.repository.EventRepository
import com.vigipro.core.model.Camera
import com.vigipro.core.model.CameraEventType
import com.vigipro.core.model.DetectedObject
import com.vigipro.core.model.DetectionCategory
import com.vigipro.feature.player.ptz.OnvifPtzClient
import com.vigipro.feature.player.ptz.PtzPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
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
    val detectedObjects: List<DetectedObject> = emptyList(),
    val isDetectionActive: Boolean = false,
    val detectionEnabled: Boolean = false,
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
    private val eventRepository: EventRepository,
    private val detectionEngine: ObjectDetectionEngine,
    private val notificationHelper: CameraNotificationHelper,
) : ViewModel(), ContainerHost<PlayerState, PlayerSideEffect> {

    private val cameraId: String = checkNotNull(savedStateHandle["cameraId"])
    private var controlsHideJob: Job? = null
    private var detectionJob: Job? = null
    private var surfaceViewRef: SurfaceView? = null
    private var lastEventLogTime = 0L
    private var lastNotificationTime = 0L

    override val container = viewModelScope.container<PlayerState, PlayerSideEffect>(PlayerState()) {
        loadCamera()
        loadAudioPreference()
        loadDetectionPreference()
    }

    override fun onCleared() {
        super.onCleared()
        ptzClient.disconnect()
        stopDetection()
        detectionEngine.release()
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
        state.camera?.let { camera ->
            eventRepository.logEvent(
                cameraId = camera.id,
                cameraName = camera.name,
                type = CameraEventType.SNAPSHOT_TAKEN,
            )
        }
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

    // Detection
    private fun loadDetectionPreference() = intent {
        preferencesRepository.userPreferences.collect { prefs ->
            reduce { state.copy(detectionEnabled = prefs.detectionEnabled) }
        }
    }

    fun setSurfaceView(surfaceView: SurfaceView?) {
        surfaceViewRef = surfaceView
    }

    fun onToggleDetection() = intent {
        val newActive = !state.isDetectionActive
        reduce { state.copy(isDetectionActive = newActive) }
        if (newActive) {
            startDetectionLoop()
        } else {
            stopDetection()
            reduce { state.copy(detectedObjects = emptyList()) }
        }
    }

    private fun startDetectionLoop() {
        detectionJob?.cancel()
        detectionJob = viewModelScope.launch {
            val initPrefs = preferencesRepository.userPreferences.first()
            detectionEngine.initialize(initPrefs.detectionConfidenceThreshold)

            while (isActive) {
                val sv = surfaceViewRef
                if (sv != null && container.stateFlow.value.isDetectionActive) {
                    val bitmap = FrameCaptureHelper.captureFrame(sv)
                    if (bitmap != null) {
                        val currentPrefs = preferencesRepository.userPreferences.first()

                        val results = detectionEngine.detect(
                            bitmap = bitmap,
                            detectPersons = currentPrefs.detectPersons,
                            detectVehicles = currentPrefs.detectVehicles,
                            detectAnimals = currentPrefs.detectAnimals,
                        )
                        bitmap.recycle()

                        intent {
                            reduce { state.copy(detectedObjects = results) }
                        }

                        if (results.any { it.category == DetectionCategory.PERSON }) {
                            handlePersonDetected(currentPrefs)
                        }

                        delay(currentPrefs.detectionIntervalMs)
                    } else {
                        delay(750L)
                    }
                } else {
                    delay(750L)
                }
            }
        }
    }

    private suspend fun handlePersonDetected(
        prefs: com.vigipro.core.data.preferences.UserPreferences,
    ) {
        val now = System.currentTimeMillis()
        val camera = container.stateFlow.value.camera ?: return

        // Log event debounced 10s
        if (now - lastEventLogTime > 10_000L) {
            lastEventLogTime = now
            eventRepository.logEvent(
                cameraId = camera.id,
                cameraName = camera.name,
                type = CameraEventType.OBJECT_DETECTED,
                message = "Pessoa detectada",
            )
        }

        // Notification debounced 30s
        if (prefs.notifyPersonDetected && now - lastNotificationTime > 30_000L) {
            lastNotificationTime = now
            notificationHelper.notifyPersonDetected(
                cameraId = camera.id,
                cameraName = camera.name,
            )
        }
    }

    fun stopDetection() {
        detectionJob?.cancel()
        detectionJob = null
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
