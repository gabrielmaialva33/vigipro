package com.vigipro.feature.player

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
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
import com.vigipro.feature.player.ui.PlayerControlsOverlay
import com.vigipro.feature.player.ui.PtzControlPad
import com.vigipro.feature.player.ui.StreamInfoOverlay
import com.vigipro.feature.player.util.FullscreenHelper
import com.vigipro.feature.player.util.findActivity
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

    // Handle back press in fullscreen
    BackHandler(enabled = state.isFullscreen) {
        viewModel.onBack()
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
                    val uri = playerViewRef?.let { snapshotManager.captureFrame(it) }
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
        }
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

    // Lifecycle-aware pause/resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
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
                viewModel.onPlaybackError(
                    error.localizedMessage ?: "Erro de reproducao",
                )
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

    if (state.isFullscreen) {
        FullscreenPlayerContent(
            state = state,
            exoPlayer = exoPlayer,
            snackbarHostState = snackbarHostState,
            viewModel = viewModel,
            onPlayerViewCreated = { playerViewRef = it },
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
private fun VideoSurface(
    state: PlayerState,
    exoPlayer: ExoPlayer,
    viewModel: PlayerViewModel,
    onPlayerViewCreated: (PlayerView) -> Unit,
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
            },
            onRelease = { view ->
                view.player = null
            },
            modifier = Modifier.fillMaxSize(),
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
