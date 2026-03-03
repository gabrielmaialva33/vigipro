package com.vigipro.feature.player

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Rational
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView
import com.vigipro.core.model.PatrolRoute
import com.vigipro.core.model.PrivacyZone
import com.vigipro.core.model.WebhookAction
import com.vigipro.core.ui.components.ErrorState
import com.vigipro.core.ui.components.LoadingIndicator
import com.vigipro.core.ui.components.VigiProTopBar
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.feature.player.patrol.PatrolButton
import com.vigipro.feature.player.patrol.PatrolConfigSheet
import com.vigipro.feature.player.privacy.PrivacyMaskOverlay
import com.vigipro.feature.player.privacy.PrivacyZoneEditor
import com.vigipro.feature.player.snapshot.SnapshotManager
import com.vigipro.feature.player.ui.DetectionOverlay
import com.vigipro.feature.player.ui.PlayerControlsOverlay
import com.vigipro.feature.player.ui.TalkbackButton
import com.vigipro.feature.player.ui.PtzControlPad
import com.vigipro.feature.player.ui.StreamInfoOverlay
import com.vigipro.feature.player.util.FullscreenHelper
import com.vigipro.feature.player.util.findActivity
import com.vigipro.feature.player.vlc.VlcPlayerWrapper
import com.vigipro.feature.player.vlc.VlcVideoSurface
import com.vigipro.feature.player.webhook.AddWebhookDialog
import com.vigipro.feature.player.webhook.WebhookButton
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Stable
class PlayerActions(
    val onControlsTap: () -> Unit,
    val onToggleFullscreen: () -> Unit,
    val onBack: () -> Unit,
    val setSurfaceView: (SurfaceView?) -> Unit,
    val onPlayPause: () -> Unit,
    val onToggleAudio: () -> Unit,
    val onSnapshot: () -> Unit,
    val onTogglePtzControls: () -> Unit,
    val onToggleStreamInfo: () -> Unit,
    val onToggleDetection: () -> Unit,
    val onPtzMove: (Float, Float, Float) -> Unit,
    val onPtzStop: () -> Unit,
    val onPtzPreset: (String) -> Unit,
    val onToggleRecording: () -> Unit,
    val onEnterPip: () -> Unit,
    val onTalkbackPress: () -> Unit,
    val onTalkbackRelease: () -> Unit,
    val onAddPrivacyZone: (Float, Float, Float, Float) -> Unit,
    val onDeletePrivacyZone: (PrivacyZone) -> Unit,
    val onTogglePrivacyZoneEditor: () -> Unit,
    val onExecuteWebhook: (WebhookAction) -> Unit,
    val onShowAddWebhook: () -> Unit,
    val onDismissAddWebhook: () -> Unit,
    val onDeleteWebhook: (String) -> Unit,
    val onSaveWebhook: (WebhookAction) -> Unit,
    val onShowPatrolSheet: () -> Unit,
    val onDismissPatrolSheet: () -> Unit,
    val onStartPatrol: (PatrolRoute) -> Unit,
    val onStopPatrol: () -> Unit,
    val onRetry: () -> Unit,
)

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snapshotManager = remember { SnapshotManager(context) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var vlcPlayer by remember { mutableStateOf<VlcPlayerWrapper?>(null) }
    var vlcSurfaceViewRef by remember { mutableStateOf<SurfaceView?>(null) }

    // Detect PiP mode
    val activity = context.findActivity()
    var isInPipMode by remember { mutableStateOf(false) }

    // Build stable actions wrapper — created once per viewModel instance
    val actions = remember(viewModel) {
        PlayerActions(
            onControlsTap = viewModel::onControlsTap,
            onToggleFullscreen = viewModel::onToggleFullscreen,
            onBack = viewModel::onBack,
            setSurfaceView = viewModel::setSurfaceView,
            onPlayPause = viewModel::onPlayPause,
            onToggleAudio = viewModel::onToggleAudio,
            onSnapshot = viewModel::onSnapshot,
            onTogglePtzControls = viewModel::onTogglePtzControls,
            onToggleStreamInfo = viewModel::onToggleStreamInfo,
            onToggleDetection = viewModel::onToggleDetection,
            onPtzMove = viewModel::onPtzMove,
            onPtzStop = viewModel::onPtzStop,
            onPtzPreset = viewModel::onPtzPreset,
            onToggleRecording = viewModel::onToggleRecording,
            onEnterPip = viewModel::onEnterPip,
            onTalkbackPress = viewModel::onTalkbackPress,
            onTalkbackRelease = viewModel::onTalkbackRelease,
            onAddPrivacyZone = viewModel::onAddPrivacyZone,
            onDeletePrivacyZone = viewModel::onDeletePrivacyZone,
            onTogglePrivacyZoneEditor = viewModel::onTogglePrivacyZoneEditor,
            onExecuteWebhook = viewModel::onExecuteWebhook,
            onShowAddWebhook = viewModel::onShowAddWebhook,
            onDismissAddWebhook = viewModel::onDismissAddWebhook,
            onDeleteWebhook = viewModel::onDeleteWebhook,
            onSaveWebhook = viewModel::onSaveWebhook,
            onShowPatrolSheet = viewModel::onShowPatrolSheet,
            onDismissPatrolSheet = viewModel::onDismissPatrolSheet,
            onStartPatrol = viewModel::onStartPatrol,
            onStopPatrol = viewModel::onStopPatrol,
            onRetry = viewModel::onRetry,
        )
    }

    // Track PiP mode changes via lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val pipObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_RESUME) {
                isInPipMode = activity?.isInPictureInPictureMode == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(pipObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(pipObserver) }
    }

    // Mic permission for talkback
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onMicPermissionResult(granted) }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onMicPermissionResult(granted)
    }

    // Handle back press in fullscreen
    BackHandler(enabled = state.isFullscreen) {
        viewModel.onBack()
    }

    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1_000,
                /* maxBufferMs = */ 5_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }

    // Side effects
    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            PlayerSideEffect.NavigateBack -> onBack()
            is PlayerSideEffect.ToggleFullscreen -> {
                context.findActivity()?.let { activity ->
                    FullscreenHelper.setFullscreen(activity, sideEffect.isFullscreen)
                }
            }
            PlayerSideEffect.RequestSnapshot -> {
                scope.launch {
                    val uri = if (state.useVlcPlayer) {
                        vlcSurfaceViewRef?.let {
                            snapshotManager.captureFrame(
                                surfaceView = it,
                                cameraName = state.camera?.name,
                            )
                        }
                    } else {
                        playerViewRef?.let {
                            snapshotManager.captureFrame(
                                playerView = it,
                                cameraName = state.camera?.name,
                            )
                        }
                    }
                    if (uri != null) {
                        viewModel.onSnapshotSaved(uri)
                    } else {
                        viewModel.onSnapshotFailed()
                    }
                }
            }
            is PlayerSideEffect.ShareSnapshot -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, sideEffect.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar captura"))
            }
            is PlayerSideEffect.ShowSnackbar -> {
                scope.launch { snackbarHostState.showSnackbar(sideEffect.message) }
            }
            PlayerSideEffect.RequestMicPermission -> {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            PlayerSideEffect.EnterPipMode -> {
                context.findActivity()?.let { act ->
                    val pipParams = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                    act.enterPictureInPictureMode(pipParams)
                }
            }
            is PlayerSideEffect.ShareRecording -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        sideEffect.file,
                    ))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar gravacao"))
            }
            is PlayerSideEffect.SwitchToSubStream -> {
                val subStreamSource = RtspMediaSource.Factory()
                    .setForceUseRtpTcp(true)
                    .setTimeoutMs(10_000L)
                    .createMediaSource(MediaItem.fromUri(sideEffect.subStreamUrl))
                exoPlayer.stop()
                exoPlayer.setMediaSource(subStreamSource)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
            is PlayerSideEffect.SwitchToVlcPlayer -> {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                val vlc = vlcPlayer ?: VlcPlayerWrapper(context).also { vlcPlayer = it }
                vlc.onPlaybackStarted = { viewModel.onPlaybackStarted() }
                vlc.onBuffering = { viewModel.onBuffering() }
                vlc.onError = { msg -> viewModel.onVlcPlaybackError(msg) }
                vlc.onVideoInfo = { codec, w, h ->
                    viewModel.onVideoInfoAvailable(codec = codec, resolution = "${w}x${h}")
                }
                // Playback starts when VlcVideoSurface composes and attaches
            }
        }
    }

    // Lifecycle-aware pause/resume (PiP-aware)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val inPip = activity?.isInPictureInPictureMode == true
                    if (!inPip) {
                        exoPlayer.pause()
                        vlcPlayer?.pause()
                    }
                    viewModel.stopDetection()
                    viewModel.onTalkbackRelease()
                    if (state.isRecording) {
                        viewModel.onToggleRecording()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (state.useVlcPlayer) {
                        vlcPlayer?.resume()
                    } else if (exoPlayer.playbackState != Player.STATE_IDLE) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
            vlcPlayer?.release()
        }
    }

    // Set media source — HLS for cloud cameras, RTSP for local cameras
    LaunchedEffect(state.camera?.hlsUrl, state.camera?.rtspUrl) {
        val camera = state.camera ?: return@LaunchedEffect

        val mediaSource = when {
            camera.hlsUrl != null -> {
                HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
                    .createMediaSource(MediaItem.fromUri(camera.hlsUrl!!))
            }
            camera.rtspUrl != null -> {
                RtspMediaSource.Factory()
                    .setTimeoutMs(10_000L)
                    .createMediaSource(MediaItem.fromUri(camera.rtspUrl!!))
            }
            else -> return@LaunchedEffect
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Audio volume sync
    LaunchedEffect(state.isAudioEnabled, state.useVlcPlayer) {
        if (state.useVlcPlayer) {
            vlcPlayer?.setVolume(if (state.isAudioEnabled) 100 else 0)
        } else {
            exoPlayer.volume = if (state.isAudioEnabled) 1f else 0f
        }
    }

    // Listen to player events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> viewModel.onPlaybackStarted()
                    Player.STATE_BUFFERING -> viewModel.onBuffering()
                    else -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                when {
                    !state.retriedWithSubStream -> viewModel.onSwitchToSubStream()
                    !state.vlcRetried && state.camera?.rtspUrl != null -> viewModel.onSwitchToVlc()
                    else -> {
                        val message = when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                            -> "Falha na conexão com a câmera"
                            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
                            -> "Transmissão ao vivo interrompida"
                            else -> "Erro ao reproduzir vídeo"
                        }
                        viewModel.onPlaybackError(message)
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    viewModel.onVideoInfoAvailable(
                        codec = exoPlayer.videoFormat?.sampleMimeType?.replace("video/", "")?.uppercase() ?: "",
                        resolution = "${videoSize.width}x${videoSize.height}",
                    )
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    val onTalkbackToggle: () -> Unit = {
        if (state.hasMicPermission) {
            scope.launch {
                snackbarHostState.showSnackbar("Segure o botão de microfone para falar")
            }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (isInPipMode) {
        // PiP mode: video only, no controls or UI chrome
        PipPlayerContent(
            state = state,
            exoPlayer = exoPlayer,
            vlcPlayer = vlcPlayer,
            setSurfaceView = actions.setSurfaceView,
            onPlayerViewCreated = { playerViewRef = it },
            onVlcSurfaceReady = { vlcSurfaceViewRef = it },
        )
    } else if (state.isFullscreen) {
        FullscreenPlayerContent(
            state = state,
            exoPlayer = exoPlayer,
            vlcPlayer = vlcPlayer,
            snackbarHostState = snackbarHostState,
            actions = actions,
            onPlayerViewCreated = { playerViewRef = it },
            onVlcSurfaceReady = { vlcSurfaceViewRef = it },
            onTalkbackToggle = onTalkbackToggle,
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = {
                VigiProTopBar(
                    title = state.camera?.name ?: "Player",
                    onBackClick = actions.onBack,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            when {
                state.isLoading -> {
                    LoadingIndicator(
                        message = "Carregando camera...",
                        modifier = Modifier.padding(padding),
                    )
                }
                state.errorMessage != null && state.camera == null -> {
                    ErrorState(
                        message = state.errorMessage!!,
                        onRetry = null,
                        modifier = Modifier.padding(padding),
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        VideoSurface(
                            state = state,
                            exoPlayer = exoPlayer,
                            vlcPlayer = vlcPlayer,
                            actions = actions,
                            onPlayerViewCreated = { playerViewRef = it },
                            onVlcSurfaceReady = { vlcSurfaceViewRef = it },
                            onTalkbackToggle = onTalkbackToggle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                        )

                        if (state.errorMessage != null) {
                            ErrorState(
                                message = state.errorMessage!!,
                                onRetry = {
                                    actions.onRetry()
                                    vlcPlayer?.stop()
                                    val camera = state.camera
                                    val source = when {
                                        camera?.hlsUrl != null ->
                                            HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
                                                .createMediaSource(MediaItem.fromUri(camera.hlsUrl!!))
                                        camera?.rtspUrl != null ->
                                            RtspMediaSource.Factory()
                                                .setTimeoutMs(10_000L)
                                                .createMediaSource(MediaItem.fromUri(camera.rtspUrl!!))
                                        else -> null
                                    }
                                    if (source != null) {
                                        exoPlayer.setMediaSource(source)
                                        exoPlayer.prepare()
                                        exoPlayer.playWhenReady = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun FullscreenPlayerContent(
    state: PlayerState,
    exoPlayer: ExoPlayer,
    vlcPlayer: VlcPlayerWrapper?,
    snackbarHostState: SnackbarHostState,
    actions: PlayerActions,
    onPlayerViewCreated: (PlayerView) -> Unit,
    onVlcSurfaceReady: (SurfaceView) -> Unit,
    onTalkbackToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        VideoSurface(
            state = state,
            exoPlayer = exoPlayer,
            vlcPlayer = vlcPlayer,
            actions = actions,
            onPlayerViewCreated = onPlayerViewCreated,
            onVlcSurfaceReady = onVlcSurfaceReady,
            onTalkbackToggle = onTalkbackToggle,
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PipPlayerContent(
    state: PlayerState,
    exoPlayer: ExoPlayer,
    vlcPlayer: VlcPlayerWrapper?,
    setSurfaceView: (SurfaceView?) -> Unit,
    onPlayerViewCreated: (PlayerView) -> Unit,
    onVlcSurfaceReady: (SurfaceView) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (state.useVlcPlayer && vlcPlayer != null) {
            VlcVideoSurface(
                vlcPlayer = vlcPlayer,
                rtspUrl = state.camera?.rtspUrl ?: "",
                onSurfaceViewReady = { surface ->
                    onVlcSurfaceReady(surface)
                    setSurfaceView(surface)
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                    onPlayerViewCreated(view)
                    setSurfaceView(view.videoSurfaceView as? SurfaceView)
                },
                onRelease = { view ->
                    view.player = null
                    setSurfaceView(null)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(
    state: PlayerState,
    exoPlayer: ExoPlayer,
    vlcPlayer: VlcPlayerWrapper?,
    actions: PlayerActions,
    onPlayerViewCreated: (PlayerView) -> Unit,
    onVlcSurfaceReady: (SurfaceView) -> Unit,
    onTalkbackToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .then(
                // ExoPlayer: use Compose pointerInput for tap gestures
                // VLC: skip — GestureDetector on SurfaceView handles taps natively
                if (state.useVlcPlayer) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { actions.onControlsTap() },
                            onDoubleTap = { actions.onToggleFullscreen() },
                        )
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Video surface — VLC fallback or ExoPlayer
        if (state.useVlcPlayer && vlcPlayer != null) {
            VlcVideoSurface(
                vlcPlayer = vlcPlayer,
                rtspUrl = state.camera?.rtspUrl ?: "",
                onSurfaceViewReady = { surface ->
                    onVlcSurfaceReady(surface)
                    actions.setSurfaceView(surface)
                },
                onTap = { actions.onControlsTap() },
                onDoubleTap = { actions.onToggleFullscreen() },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                    onPlayerViewCreated(view)
                    actions.setSurfaceView(view.videoSurfaceView as? SurfaceView)
                },
                onRelease = { view ->
                    view.player = null
                    actions.setSurfaceView(null)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        val isDemo = state.camera?.isDemo == true

        // Detection overlay (bounding boxes + AI badge) — not for demo
        if (!isDemo) {
            DetectionOverlay(
                detectedObjects = state.detectedObjects,
                isActive = state.isDetectionActive,
            )
        }

        // Privacy mask overlay (always visible when zones exist and masking enabled) — not for demo
        if (!isDemo && state.privacyMaskingEnabled && state.privacyZones.isNotEmpty() && !state.isEditingPrivacyZones) {
            PrivacyMaskOverlay(
                zones = state.privacyZones,
            )
        }

        // Privacy zone editor (interactive, when editing) — not for demo
        if (!isDemo && state.isEditingPrivacyZones) {
            PrivacyZoneEditor(
                zones = state.privacyZones,
                onZoneAdded = { left, top, right, bottom ->
                    actions.onAddPrivacyZone(left, top, right, bottom)
                },
                onZoneDeleted = actions.onDeletePrivacyZone,
                onDismiss = actions.onTogglePrivacyZoneEditor,
            )
        }

        // Buffering indicator
        if (state.isBuffering && !state.isPlaying) {
            LoadingIndicator()
        }

        // Stream info overlay (top-right when visible)
        StreamInfoOverlay(
            isVisible = state.showStreamInfo,
            codec = state.codec,
            resolution = state.resolution,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = if (state.isFullscreen) Dimens.SpacingXxxl else Dimens.SpacingSm),
        )

        // Talkback push-to-talk button (bottom-start) — not for demo
        if (!isDemo) {
            TalkbackButton(
                isActive = state.isTalkbackActive,
                isVisible = state.talkbackAvailable && state.hasMicPermission && state.talkbackEnabled,
                onPress = actions.onTalkbackPress,
                onRelease = actions.onTalkbackRelease,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            )
        }

        // Patrol status button (top-start, below controls) — not for demo
        if (!isDemo && state.camera?.ptzCapable == true && state.isPtzConnected && state.patrolState.isPatrolling) {
            PatrolButton(
                patrolState = state.patrolState,
                onClick = actions.onShowPatrolSheet,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = if (state.isFullscreen) 72.dp else 56.dp),
            )
        }

        // Webhook button (bottom-end, above controls) — not for demo
        if (!isDemo) {
            WebhookButton(
                webhooks = state.webhooks,
                onExecute = actions.onExecuteWebhook,
                isExecuting = state.isWebhookExecuting,
                onAddWebhook = actions.onShowAddWebhook,
                onDeleteWebhook = actions.onDeleteWebhook,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }

        // Add Webhook dialog — not for demo
        if (!isDemo && state.showAddWebhookDialog) {
            AddWebhookDialog(
                cameraId = state.camera?.id ?: "",
                onSave = actions.onSaveWebhook,
                onDismiss = actions.onDismissAddWebhook,
            )
        }

        // Patrol config sheet — not for demo
        if (!isDemo && state.showPatrolSheet) {
            PatrolConfigSheet(
                presets = state.ptzPresets,
                patrolState = state.patrolState,
                cameraId = state.camera?.id ?: "",
                onStartPatrol = actions.onStartPatrol,
                onStopPatrol = actions.onStopPatrol,
                onDismiss = actions.onDismissPatrolSheet,
            )
        }

        // PTZ control pad (center-left) — not for demo
        if (!isDemo) {
            PtzControlPad(
                isVisible = state.showPtzControls && state.isPtzConnected,
                presets = state.ptzPresets,
                onMove = actions.onPtzMove,
                onStop = actions.onPtzStop,
                onPresetClick = actions.onPtzPreset,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        // Custom controls overlay
        PlayerControlsOverlay(
            isVisible = state.showControls,
            isPlaying = state.isPlaying,
            isFullscreen = state.isFullscreen,
            isAudioEnabled = state.isAudioEnabled,
            isPtzCapable = !isDemo && state.camera?.ptzCapable == true && state.isPtzConnected,
            showPtzControls = !isDemo && state.showPtzControls,
            isDetectionActive = !isDemo && state.isDetectionActive,
            detectionEnabled = !isDemo && state.detectionEnabled,
            isTalkbackActive = !isDemo && state.isTalkbackActive,
            talkbackAvailable = !isDemo && state.talkbackAvailable && state.talkbackEnabled,
            cameraName = state.camera?.name ?: "",
            onBackClick = actions.onBack,
            onPlayPauseClick = {
                actions.onPlayPause()
                if (state.useVlcPlayer) {
                    val vlc = vlcPlayer
                    if (vlc != null && vlc.isPlaying) vlc.pause() else vlc?.resume()
                } else {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
            },
            onFullscreenClick = actions.onToggleFullscreen,
            onAudioToggle = actions.onToggleAudio,
            onSnapshotClick = actions.onSnapshot,
            onPtzToggle = actions.onTogglePtzControls,
            onInfoClick = actions.onToggleStreamInfo,
            onDetectionToggle = actions.onToggleDetection,
            onTalkbackToggle = onTalkbackToggle,
            onPipClick = actions.onEnterPip,
            isRecording = !isDemo && state.isRecording,
            recordingDurationMs = { state.recordingDurationMs },
            onRecordClick = actions.onToggleRecording,
            isPatrolling = !isDemo && state.patrolState.isPatrolling,
            onPatrolClick = actions.onShowPatrolSheet,
            onPrivacyZoneToggle = actions.onTogglePrivacyZoneEditor,
            hasPrivacyZones = !isDemo && state.privacyZones.isNotEmpty(),
            modifier = if (state.isFullscreen) {
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout)
            } else {
                Modifier.fillMaxSize()
            },
        )
    }
}
