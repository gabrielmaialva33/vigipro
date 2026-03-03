package com.vigipro.feature.player

import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.audio.AudioCaptureManager
import com.vigipro.core.data.detection.FrameCaptureHelper
import com.vigipro.core.data.detection.ObjectDetectionEngine
import com.vigipro.core.data.notification.CameraNotificationHelper
import com.vigipro.core.data.preferences.UserPreferencesRepository
import com.vigipro.core.data.recording.StreamRecorder
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.repository.CloudRepository
import com.vigipro.core.data.repository.EventRepository
import com.vigipro.core.data.repository.PrivacyZoneRepository
import com.vigipro.core.data.repository.WebhookRepository
import com.vigipro.core.data.webhook.WebhookExecutor
import com.vigipro.core.model.Camera
import com.vigipro.core.model.CameraEventType
import com.vigipro.core.model.PatrolRoute
import com.vigipro.core.model.PrivacyZone
import com.vigipro.core.model.WebhookAction
import com.vigipro.core.model.DetectedObject
import com.vigipro.core.model.DetectionCategory
import com.vigipro.feature.player.patrol.PatrolManager
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
    val isTalkbackActive: Boolean = false,
    val talkbackAvailable: Boolean = false,
    val hasMicPermission: Boolean = false,
    val talkbackEnabled: Boolean = true,
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0,
    val webhooks: List<WebhookAction> = emptyList(),
    val isWebhookExecuting: Boolean = false,
    val showAddWebhookDialog: Boolean = false,
    val isSubStream: Boolean = false,
    val retriedWithSubStream: Boolean = false,
    val useVlcPlayer: Boolean = false,
    val vlcRetried: Boolean = false,
    val patrolState: PatrolManager.PatrolState = PatrolManager.PatrolState(),
    val showPatrolSheet: Boolean = false,
    val privacyZones: List<PrivacyZone> = emptyList(),
    val isEditingPrivacyZones: Boolean = false,
    val privacyMaskingEnabled: Boolean = true,
)

sealed interface PlayerSideEffect {
    data object NavigateBack : PlayerSideEffect
    data class ToggleFullscreen(val isFullscreen: Boolean) : PlayerSideEffect
    data object RequestSnapshot : PlayerSideEffect
    data class ShareSnapshot(val uri: Uri) : PlayerSideEffect
    data class ShowSnackbar(val message: String) : PlayerSideEffect
    data object RequestMicPermission : PlayerSideEffect
    data object EnterPipMode : PlayerSideEffect
    data class ShareRecording(val file: java.io.File) : PlayerSideEffect
    data class SwitchToSubStream(val subStreamUrl: String) : PlayerSideEffect
    data class SwitchToVlcPlayer(val rtspUrl: String) : PlayerSideEffect
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cameraRepository: CameraRepository,
    private val cloudRepository: CloudRepository,
    private val ptzClient: OnvifPtzClient,
    private val preferencesRepository: UserPreferencesRepository,
    private val eventRepository: EventRepository,
    private val detectionEngine: ObjectDetectionEngine,
    private val notificationHelper: CameraNotificationHelper,
    private val audioCaptureManager: AudioCaptureManager,
    private val streamRecorder: StreamRecorder,
    private val webhookRepository: WebhookRepository,
    private val webhookExecutor: WebhookExecutor,
    private val patrolManager: PatrolManager,
    private val privacyZoneRepository: PrivacyZoneRepository,
) : ViewModel(), ContainerHost<PlayerState, PlayerSideEffect> {

    private val cameraId: String = checkNotNull(savedStateHandle["cameraId"])
    private var controlsHideJob: Job? = null
    private var detectionJob: Job? = null
    private var talkbackJob: Job? = null
    private var surfaceViewRef: SurfaceView? = null
    private var lastEventLogTime = 0L
    private var lastNotificationTime = 0L

    override val container = viewModelScope.container<PlayerState, PlayerSideEffect>(PlayerState()) {
        loadCamera()
        loadAudioPreference()
        loadDetectionPreference()
        loadTalkbackPreference()
        loadWebhooks()
        observePatrolState()
        loadPrivacyZones()
        loadPrivacyMaskingPreference()
    }

    override fun onCleared() {
        viewModelScope.launch { streamRecorder.stopRecording() }
        super.onCleared()
        patrolManager.detach()
        ptzClient.disconnect()
        stopDetection()
        detectionEngine.release()
        stopTalkback()
    }

    private fun loadCamera() = intent {
        // Try local Room DB first
        var camera = cameraRepository.getCameraById(cameraId)

        // Fallback: try cloud demo cameras
        if (camera == null) {
            camera = cloudRepository.fetchDemoCameras()
                .getOrNull()
                ?.find { it.id == cameraId }
        }

        // Fallback: try public cameras catalog
        if (camera == null) {
            camera = cloudRepository.fetchPublicCameras()
                .getOrNull()
                ?.first?.find { it.id == cameraId }
        }

        if (camera == null) {
            reduce { state.copy(isLoading = false, errorMessage = "Câmera não encontrada") }
        } else {
            reduce {
                state.copy(
                    camera = camera,
                    isLoading = false,
                    talkbackAvailable = camera.audioCapable && !camera.isDemo,
                )
            }
            if (camera.ptzCapable && camera.onvifAddress != null && !camera.isDemo) {
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
                        patrolManager.attach(ptzClient)
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
        Log.d("PlayerVM", "onControlsTap: showControls ${ state.showControls } -> $newVisibility")
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

    // Talkback
    private fun loadTalkbackPreference() = intent {
        preferencesRepository.userPreferences.collect { prefs ->
            reduce { state.copy(talkbackEnabled = prefs.talkbackEnabled) }
        }
    }

    fun onMicPermissionResult(granted: Boolean) = intent {
        reduce { state.copy(hasMicPermission = granted) }
    }

    fun onTalkbackPress() {
        val camera = container.stateFlow.value.camera ?: return
        val rtspUrl = camera.rtspUrl ?: return

        talkbackJob?.cancel()
        talkbackJob = viewModelScope.launch {
            val connected = audioCaptureManager.connectToCamera(
                rtspUrl = rtspUrl,
                username = camera.username ?: "",
                password = "",
            )
            if (!connected) {
                intent { postSideEffect(PlayerSideEffect.ShowSnackbar("Falha ao conectar áudio bidirecional")) }
                return@launch
            }
            intent { reduce { state.copy(isTalkbackActive = true) } }
            audioCaptureManager.startCapture() // blocks until stopped
        }
    }

    fun onTalkbackRelease() {
        audioCaptureManager.stopCapture()
        talkbackJob?.cancel()
        talkbackJob = null
        viewModelScope.launch {
            audioCaptureManager.disconnect()
            intent { reduce { state.copy(isTalkbackActive = false) } }
        }
    }

    private fun stopTalkback() {
        audioCaptureManager.stopCapture()
        talkbackJob?.cancel()
        talkbackJob = null
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
        reduce {
            state.copy(
                errorMessage = null,
                isBuffering = true,
                useVlcPlayer = false,
                vlcRetried = false,
                retriedWithSubStream = false,
                isSubStream = false,
            )
        }
    }

    // Recording
    fun onToggleRecording() = intent {
        if (state.isRecording) {
            val file = streamRecorder.stopRecording()
            reduce { state.copy(isRecording = false, recordingDurationMs = 0) }
            if (file != null) {
                postSideEffect(PlayerSideEffect.ShowSnackbar("Gravacao salva"))
                postSideEffect(PlayerSideEffect.ShareRecording(file))
            }
        } else {
            val camera = state.camera ?: return@intent
            val rtspUrl = camera.rtspUrl ?: return@intent
            val res = state.resolution.split("x")
            val w = res.getOrNull(0)?.toIntOrNull() ?: 640
            val h = res.getOrNull(1)?.toIntOrNull() ?: 480
            val started = streamRecorder.startRecording(rtspUrl, camera.id, camera.name, w, h)
            if (started) {
                reduce { state.copy(isRecording = true) }
                startRecordingTimer()
            } else {
                postSideEffect(PlayerSideEffect.ShowSnackbar("Falha ao iniciar gravacao"))
            }
        }
    }

    private fun startRecordingTimer() {
        viewModelScope.launch {
            while (streamRecorder.isRecording) {
                val elapsed = System.currentTimeMillis() - streamRecorder.recordingStartTime
                intent { reduce { state.copy(recordingDurationMs = elapsed) } }
                delay(1000L)
            }
        }
    }

    // Webhooks
    private fun loadWebhooks() = intent {
        val camera = state.camera ?: return@intent
        webhookRepository.getWebhooksByCamera(camera.id).collect { hooks ->
            reduce { state.copy(webhooks = hooks) }
        }
    }

    fun onExecuteWebhook(webhook: WebhookAction) = intent {
        reduce { state.copy(isWebhookExecuting = true) }
        val result = webhookExecutor.execute(
            url = webhook.url,
            method = webhook.method.name,
            headers = webhook.headers,
            body = webhook.body,
        )
        reduce { state.copy(isWebhookExecuting = false) }
        val msg = if (result.success) {
            "${webhook.name}: Sucesso"
        } else {
            "${webhook.name}: Falha (${result.message})"
        }
        postSideEffect(PlayerSideEffect.ShowSnackbar(msg))
    }

    fun onShowAddWebhook() = intent {
        reduce { state.copy(showAddWebhookDialog = true) }
    }

    fun onDismissAddWebhook() = intent {
        reduce { state.copy(showAddWebhookDialog = false) }
    }

    fun onSaveWebhook(webhook: WebhookAction) = intent {
        webhookRepository.saveWebhook(webhook)
        reduce { state.copy(showAddWebhookDialog = false) }
    }

    fun onDeleteWebhook(webhookId: String) = intent {
        webhookRepository.deleteWebhook(webhookId)
    }

    // Patrol
    private fun observePatrolState() = intent {
        patrolManager.state.collect { patrol ->
            reduce { state.copy(patrolState = patrol) }
        }
    }

    fun onShowPatrolSheet() = intent {
        reduce { state.copy(showPatrolSheet = true) }
        controlsHideJob?.cancel()
    }

    fun onDismissPatrolSheet() = intent {
        reduce { state.copy(showPatrolSheet = false) }
    }

    fun onStartPatrol(route: PatrolRoute) {
        patrolManager.startPatrol(route, viewModelScope)
    }

    fun onStopPatrol() {
        patrolManager.stopPatrol()
    }

    // Privacy Zones
    private fun loadPrivacyZones() = intent {
        privacyZoneRepository.getZonesForCamera(cameraId).collect { zones ->
            reduce { state.copy(privacyZones = zones) }
        }
    }

    private fun loadPrivacyMaskingPreference() = intent {
        preferencesRepository.userPreferences.collect { prefs ->
            reduce { state.copy(privacyMaskingEnabled = prefs.privacyMaskingEnabled) }
        }
    }

    fun onTogglePrivacyZoneEditor() = intent {
        val newEditing = !state.isEditingPrivacyZones
        reduce { state.copy(isEditingPrivacyZones = newEditing, showControls = false) }
        controlsHideJob?.cancel()
    }

    fun onAddPrivacyZone(left: Float, top: Float, right: Float, bottom: Float) = intent {
        privacyZoneRepository.addZone(
            cameraId = cameraId,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )
    }

    fun onDeletePrivacyZone(zone: PrivacyZone) = intent {
        privacyZoneRepository.deleteZone(zone)
    }

    // Stream quality fallback
    fun onSwitchToSubStream() = intent {
        val camera = state.camera ?: return@intent
        val originalUrl = camera.rtspUrl ?: return@intent
        val subUrl = deriveSubStreamUrl(originalUrl)
        reduce {
            state.copy(
                isSubStream = true,
                retriedWithSubStream = true,
                errorMessage = null,
                isBuffering = true,
            )
        }
        postSideEffect(PlayerSideEffect.SwitchToSubStream(subUrl))
    }

    fun onSwitchToVlc() = intent {
        val camera = state.camera ?: return@intent
        val rtspUrl = camera.rtspUrl ?: return@intent
        reduce {
            state.copy(
                useVlcPlayer = true,
                vlcRetried = true,
                errorMessage = null,
                isBuffering = true,
            )
        }
        postSideEffect(PlayerSideEffect.SwitchToVlcPlayer(rtspUrl))
    }

    fun onVlcPlaybackError(message: String) = intent {
        reduce { state.copy(isPlaying = false, isBuffering = false, errorMessage = message) }
    }

    fun onEnterPip() = intent {
        postSideEffect(PlayerSideEffect.EnterPipMode)
    }

    companion object {
        fun deriveSubStreamUrl(mainUrl: String): String {
            return when {
                mainUrl.contains("subtype=0") ->
                    mainUrl.replace("subtype=0", "subtype=1")
                mainUrl.contains("subtype=") ->
                    mainUrl // Already a sub stream or custom, leave as-is
                mainUrl.contains("?") ->
                    "$mainUrl&subtype=1"
                else ->
                    "$mainUrl?subtype=1"
            }
        }
    }
}
