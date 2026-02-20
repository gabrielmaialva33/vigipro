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
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import com.vigipro.core.ui.components.ErrorState
import com.vigipro.core.ui.components.LoadingIndicator
import com.vigipro.core.ui.components.VigiProTopBar
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.feature.player.snapshot.SnapshotManager
import com.vigipro.feature.player.ui.DetectionOverlay
import com.vigipro.feature.player.ui.PlayerControlsOverlay
import com.vigipro.feature.player.ui.TalkbackButton
import com.vigipro.feature.player.ui.PtzControlPad
import com.vigipro.feature.player.ui.StreamInfoOverlay
import com.vigipro.feature.player.util.FullscreenHelper
import com.vigipro.feature.player.util.findActivity
import com.vigipro.feature.player.webhook.AddWebhookDialog
import com.vigipro.feature.player.webhook.WebhookButton
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

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

    // Detect PiP mode
    val activity = context.findActivity()
    var isInPipMode by remember { mutableStateOf(false) }

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
                    val uri = playerViewRef?.let {
                        snapshotManager.captureFrame(
                            playerView = it,
                            cameraName = state.camera?.name,
                        )
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
        }
    }

    // Lifecycle-aware pause/resume (PiP-aware)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val inPip = activity?.isInPictureInPictureMode == true
                    if (!inPip) {
                        // Normal pause: stop everything
                        exoPlayer.pause()
                    }
                    // Always stop detection, talkback, and recording (even in PiP)
                    viewModel.stopDetection()
                    viewModel.onTalkbackRelease()
                    if (state.isRecording) {
                        viewModel.onToggleRecording()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer.playbackState != Player.STATE_IDLE) {
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
        }
    }

    // Set RTSP media source with TCP and timeout
    LaunchedEffect(state.camera?.rtspUrl) {
        val rtspUrl = state.camera?.rtspUrl ?: return@LaunchedEffect

        val rtspMediaSource = RtspMediaSource.Factory()
            .setForceUseRtpTcp(true)
            .setTimeoutMs(10_000L)
            .createMediaSource(MediaItem.fromUri(rtspUrl))

        exoPlayer.setMediaSource(rtspMediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Audio volume sync
    LaunchedEffect(state.isAudioEnabled) {
        exoPlayer.volume = if (state.isAudioEnabled) 1f else 0f
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
                if (!state.retriedWithSubStream) {
                    viewModel.onSwitchToSubStream()
                } else {
                    viewModel.onPlaybackError(
                        error.localizedMessage ?: "Erro de reproducao",
                    )
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
                snackbarHostState.showSnackbar("Segure o botao de microfone para falar")
            }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (isInPipMode) {
        // PiP mode: video only, no controls or UI chrome
        PipPlayerContent(
            exoPlayer = exoPlayer,
            viewModel = viewModel,
            onPlayerViewCreated = { playerViewRef = it },
        )
    } else if (state.isFullscreen) {
        FullscreenPlayerContent(
            state = state,
            exoPlayer = exoPlayer,
            snackbarHostState = snackbarHostState,
            viewModel = viewModel,
            onPlayerViewCreated = { playerViewRef = it },
            onTalkbackToggle = onTalkbackToggle,
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = {
                VigiProTopBar(
                    title = state.camera?.name ?: "Player",
                    onBackClick = viewModel::onBack,
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
                            viewModel = viewModel,
                            onPlayerViewCreated = { playerViewRef = it },
                            onTalkbackToggle = onTalkbackToggle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                        )

                        if (state.errorMessage != null) {
                            ErrorState(
                                message = state.errorMessage!!,
                                onRetry = {
                                    viewModel.onRetry()
                                    exoPlayer.stop()
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
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
    snackbarHostState: SnackbarHostState,
    viewModel: PlayerViewModel,
    onPlayerViewCreated: (PlayerView) -> Unit,
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
            viewModel = viewModel,
            onPlayerViewCreated = onPlayerViewCreated,
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
    exoPlayer: ExoPlayer,
    viewModel: PlayerViewModel,
    onPlayerViewCreated: (PlayerView) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
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
                viewModel.setSurfaceView(view.videoSurfaceView as? SurfaceView)
            },
            onRelease = { view ->
                view.player = null
                viewModel.setSurfaceView(null)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(
    state: PlayerState,
    exoPlayer: ExoPlayer,
    viewModel: PlayerViewModel,
    onPlayerViewCreated: (PlayerView) -> Unit,
    onTalkbackToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { viewModel.onControlsTap() },
                    onDoubleTap = { viewModel.onToggleFullscreen() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Video surface (no native controls)
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
                viewModel.setSurfaceView(view.videoSurfaceView as? SurfaceView)
            },
            onRelease = { view ->
                view.player = null
                viewModel.setSurfaceView(null)
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Detection overlay (bounding boxes + AI badge)
        DetectionOverlay(
            detectedObjects = state.detectedObjects,
            isActive = state.isDetectionActive,
        )

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

        // Talkback push-to-talk button (bottom-start)
        TalkbackButton(
            isActive = state.isTalkbackActive,
            isVisible = state.talkbackAvailable && state.hasMicPermission && state.talkbackEnabled,
            onPress = viewModel::onTalkbackPress,
            onRelease = viewModel::onTalkbackRelease,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )

        // Webhook button (bottom-end, above controls)
        WebhookButton(
            webhooks = state.webhooks,
            onExecute = viewModel::onExecuteWebhook,
            isExecuting = state.isWebhookExecuting,
            onAddWebhook = viewModel::onShowAddWebhook,
            onDeleteWebhook = viewModel::onDeleteWebhook,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )

        // Add Webhook dialog
        if (state.showAddWebhookDialog) {
            AddWebhookDialog(
                cameraId = state.camera?.id ?: "",
                onSave = viewModel::onSaveWebhook,
                onDismiss = viewModel::onDismissAddWebhook,
            )
        }

        // PTZ control pad (center-left)
        PtzControlPad(
            isVisible = state.showPtzControls && state.isPtzConnected,
            presets = state.ptzPresets,
            onMove = viewModel::onPtzMove,
            onStop = viewModel::onPtzStop,
            onPresetClick = viewModel::onPtzPreset,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        // Custom controls overlay
        PlayerControlsOverlay(
            isVisible = state.showControls,
            isPlaying = state.isPlaying,
            isFullscreen = state.isFullscreen,
            isAudioEnabled = state.isAudioEnabled,
            isPtzCapable = state.camera?.ptzCapable == true && state.isPtzConnected,
            showPtzControls = state.showPtzControls,
            isDetectionActive = state.isDetectionActive,
            detectionEnabled = state.detectionEnabled,
            isTalkbackActive = state.isTalkbackActive,
            talkbackAvailable = state.talkbackAvailable && state.talkbackEnabled,
            cameraName = state.camera?.name ?: "",
            onBackClick = viewModel::onBack,
            onPlayPauseClick = {
                viewModel.onPlayPause()
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            onFullscreenClick = viewModel::onToggleFullscreen,
            onAudioToggle = viewModel::onToggleAudio,
            onSnapshotClick = viewModel::onSnapshot,
            onPtzToggle = viewModel::onTogglePtzControls,
            onInfoClick = viewModel::onToggleStreamInfo,
            onDetectionToggle = viewModel::onToggleDetection,
            onTalkbackToggle = onTalkbackToggle,
            onPipClick = viewModel::onEnterPip,
            isRecording = state.isRecording,
            recordingDurationMs = state.recordingDurationMs,
            onRecordClick = viewModel::onToggleRecording,
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
