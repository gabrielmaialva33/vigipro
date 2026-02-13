package com.vigipro.feature.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.media3.common.util.UnstableApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.vigipro.core.ui.components.ErrorState
import com.vigipro.core.ui.components.LoadingIndicator
import com.vigipro.core.ui.components.VigiProTopBar
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

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            PlayerSideEffect.NavigateBack -> onBack()
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Set media source when camera loads
    LaunchedEffect(state.camera?.rtspUrl) {
        val rtspUrl = state.camera?.rtspUrl ?: return@LaunchedEffect
        val mediaItem = MediaItem.fromUri(rtspUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
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
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            VigiProTopBar(
                title = state.camera?.name ?: "Player",
                onBackClick = viewModel::onBack,
            )
        },
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
                    // Video surface
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentAlignment = Alignment.Center,
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )

                        if (state.isBuffering && !state.isPlaying) {
                            LoadingIndicator()
                        }
                    }

                    // Error overlay below video
                    if (state.errorMessage != null) {
                        ErrorState(
                            message = state.errorMessage!!,
                            onRetry = {
                                viewModel.onRetry()
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
