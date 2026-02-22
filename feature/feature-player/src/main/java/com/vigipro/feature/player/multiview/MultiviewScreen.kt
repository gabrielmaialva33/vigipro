package com.vigipro.feature.player.multiview

import android.app.ActivityManager
import android.content.Context
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import com.vigipro.core.model.Camera
import com.vigipro.core.model.CameraStatus
import com.vigipro.core.ui.components.EmptyState
import com.vigipro.core.ui.components.LoadingIndicator
import com.vigipro.core.ui.components.VigiProTopBar
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.core.ui.theme.StatusError
import com.vigipro.core.ui.theme.StatusOffline
import com.vigipro.core.ui.theme.StatusOnline
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiviewScreen(
    onBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MultiviewViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    var showCameraPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is MultiviewSideEffect.NavigateToPlayer -> onNavigateToPlayer(sideEffect.cameraId)
            MultiviewSideEffect.NavigateBack -> onBack()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            VigiProTopBar(
                title = "Multiview",
                onBackClick = viewModel::onBack,
                actions = {
                    if (state.selectedCameras.isNotEmpty()) {
                        IconButton(onClick = { showCameraPicker = true }) {
                            Icon(PhosphorIcons.Regular.PencilSimple, contentDescription = "Editar cameras")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                LoadingIndicator(
                    message = "Carregando cameras...",
                    modifier = Modifier.padding(padding),
                )
            }
            state.selectedCameras.isEmpty() -> {
                CameraPicker(
                    cameras = state.cameras,
                    selectedCameras = state.selectedCameras,
                    maxCameras = state.maxCameras,
                    onSelect = viewModel::onCameraSelect,
                    onDeselect = viewModel::onCameraDeselect,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Layout mode chips
                    LayoutModeSelector(
                        currentLayout = state.layoutMode,
                        onLayoutChange = viewModel::onLayoutChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm),
                    )

                    // Grid of players
                    MultiviewGrid(
                        cameras = state.selectedCameras,
                        layoutMode = state.layoutMode,
                        onCellTap = viewModel::onCellTap,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimens.SpacingSm),
                    )
                }
            }
        }
    }

    // Bottom sheet camera picker
    if (showCameraPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCameraPicker = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = Dimens.SpacingLg,
                    end = Dimens.SpacingLg,
                    bottom = Dimens.SpacingXl,
                ),
            ) {
                Text(
                    text = "Selecionar Cameras (${state.selectedCameras.size}/${state.maxCameras})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimens.SpacingMd),
                )
                CameraPickerList(
                    cameras = state.cameras,
                    selectedCameras = state.selectedCameras,
                    maxCameras = state.maxCameras,
                    onSelect = viewModel::onCameraSelect,
                    onDeselect = viewModel::onCameraDeselect,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LayoutModeSelector(
    currentLayout: MultiviewLayout,
    onLayoutChange: (MultiviewLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
    ) {
        MultiviewLayout.entries.forEach { layout ->
            FilterChip(
                selected = currentLayout == layout,
                onClick = { onLayoutChange(layout) },
                label = { Text(layout.label) },
                leadingIcon = if (currentLayout == layout) {
                    { Icon(PhosphorIcons.Regular.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun CameraPicker(
    cameras: List<Camera>,
    selectedCameras: List<Camera>,
    maxCameras: Int,
    onSelect: (Camera) -> Unit,
    onDeselect: (Camera) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Selecione as cameras para o mosaico",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(
                start = Dimens.SpacingLg,
                end = Dimens.SpacingLg,
                top = Dimens.SpacingLg,
                bottom = Dimens.SpacingSm,
            ),
        )
        Text(
            text = "Maximo de $maxCameras cameras simultaneas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = Dimens.SpacingLg,
                end = Dimens.SpacingLg,
                bottom = Dimens.SpacingMd,
            ),
        )
        if (cameras.isEmpty()) {
            EmptyState(
                icon = PhosphorIcons.Regular.GridFour,
                title = "Nenhuma Camera Disponivel",
                subtitle = "Adicione cameras ao sistema para utilizar o modo multiview.",
            )
        } else {
            CameraPickerList(
                cameras = cameras,
                selectedCameras = selectedCameras,
                maxCameras = maxCameras,
                onSelect = onSelect,
                onDeselect = onDeselect,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CameraPickerList(
    cameras: List<Camera>,
    selectedCameras: List<Camera>,
    maxCameras: Int,
    onSelect: (Camera) -> Unit,
    onDeselect: (Camera) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(cameras, key = { it.id }) { camera ->
            val isSelected = selectedCameras.any { it.id == camera.id }
            val canSelect = selectedCameras.size < maxCameras || isSelected

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = canSelect) {
                        if (isSelected) onDeselect(camera) else onSelect(camera)
                    }
                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked ->
                        if (checked) onSelect(camera) else onDeselect(camera)
                    },
                    enabled = canSelect,
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingMd))

                // Status dot
                Box(
                    modifier = Modifier
                        .size(Dimens.CameraStatusDotSize)
                        .clip(CircleShape)
                        .background(
                            when (camera.status) {
                                CameraStatus.ONLINE -> StatusOnline
                                CameraStatus.OFFLINE -> StatusOffline
                                CameraStatus.ERROR -> StatusError
                            },
                        ),
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingMd))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = camera.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when (camera.status) {
                            CameraStatus.ONLINE -> "Online"
                            CameraStatus.OFFLINE -> "Offline"
                            CameraStatus.ERROR -> "Erro"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiviewGrid(
    cameras: List<Camera>,
    layoutMode: MultiviewLayout,
    onCellTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (layoutMode) {
        MultiviewLayout.GRID_2X2 -> Grid2x2(cameras, onCellTap, modifier)
        MultiviewLayout.GRID_1_PLUS_3 -> Grid1Plus3(cameras, onCellTap, modifier)
        MultiviewLayout.SINGLE -> {
            cameras.firstOrNull()?.let { camera ->
                MultiviewCell(
                    camera = camera,
                    onTap = { onCellTap(camera.id) },
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun Grid2x2(
    cameras: List<Camera>,
    onCellTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.GridCellSpacing),
    ) {
        // Top row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GridCellSpacing),
        ) {
            cameras.getOrNull(0)?.let { camera ->
                MultiviewCell(
                    camera = camera,
                    onTap = { onCellTap(camera.id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            cameras.getOrNull(1)?.let { camera ->
                MultiviewCell(
                    camera = camera,
                    onTap = { onCellTap(camera.id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
        }

        // Bottom row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GridCellSpacing),
        ) {
            cameras.getOrNull(2)?.let { camera ->
                MultiviewCell(
                    camera = camera,
                    onTap = { onCellTap(camera.id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            cameras.getOrNull(3)?.let { camera ->
                MultiviewCell(
                    camera = camera,
                    onTap = { onCellTap(camera.id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Grid1Plus3(
    cameras: List<Camera>,
    onCellTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GridCellSpacing),
    ) {
        // Main large cell (2/3 width)
        cameras.getOrNull(0)?.let { camera ->
            MultiviewCell(
                camera = camera,
                onTap = { onCellTap(camera.id) },
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
            )
        } ?: Spacer(modifier = Modifier.weight(2f))

        // Three small stacked cells (1/3 width)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(Dimens.GridCellSpacing),
        ) {
            cameras.getOrNull(1)?.let { camera ->
                MultiviewCell(
                    camera = camera,
                    onTap = { onCellTap(camera.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            cameras.getOrNull(2)?.let { camera ->
                MultiviewCell(
                    camera = camera,
                    onTap = { onCellTap(camera.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            cameras.getOrNull(3)?.let { camera ->
                MultiviewCell(
                    camera = camera,
                    onTap = { onCellTap(camera.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } ?: Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MultiviewCell(
    camera: Camera,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(camera.id) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 500,
                /* maxBufferMs = */ 2_000,
                /* bufferForPlaybackMs = */ 250,
                /* bufferForPlaybackAfterRebufferMs = */ 500,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
    }

    // Set RTSP source
    LaunchedEffect(camera.rtspUrl) {
        val rtspUrl = camera.rtspUrl ?: return@LaunchedEffect

        val rtspMediaSource = RtspMediaSource.Factory()
            .setForceUseRtpTcp(true)
            .setTimeoutMs(10_000L)
            .createMediaSource(MediaItem.fromUri(rtspUrl))

        exoPlayer.setMediaSource(rtspMediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.volume = 0f // Muted in multiview
    }

    // Lifecycle-aware pause/resume
    DisposableEffect(lifecycleOwner, camera.id) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer.playbackState != androidx.media3.common.Player.STATE_IDLE) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .clickable(onClick = onTap),
    ) {
        // SurfaceView for efficient rendering (hardware overlay, less memory than TextureView)
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).also { surface ->
                    exoPlayer.setVideoSurfaceView(surface)
                }
            },
            onRelease = { surface ->
                exoPlayer.clearVideoSurfaceView(surface)
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Camera name overlay at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Status indicator dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when (camera.status) {
                                CameraStatus.ONLINE -> StatusOnline
                                CameraStatus.OFFLINE -> StatusOffline
                                CameraStatus.ERROR -> StatusError
                            },
                        ),
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                Text(
                    text = camera.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Loading indicator when buffering
        if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
            LoadingIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
