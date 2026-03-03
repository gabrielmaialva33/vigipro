package com.vigipro.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowClockwise
import com.adamglin.phosphoricons.regular.Eye
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.VideoCamera
import com.vigipro.core.model.CameraStatus
import com.vigipro.core.ui.components.CameraCard
import com.vigipro.core.ui.components.ConnectionStatus
import com.vigipro.core.ui.theme.Dimens
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToAddCamera: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is DiscoverSideEffect.NavigateToPlayer -> onNavigateToPlayer(sideEffect.cameraId)
            DiscoverSideEffect.NavigateToLogin -> onNavigateToLogin()
            DiscoverSideEffect.NavigateToAddCamera -> onNavigateToAddCamera()
        }
    }

    val gridState = rememberLazyGridState()

    // Detect scroll to bottom for pagination
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoading && !state.isLoadingMore && state.currentPage < state.totalPages) {
            viewModel.onLoadMore()
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onAddCameraClick,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Plus,
                    contentDescription = "Adicionar camera",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = Dimens.GridPadding,
                end = Dimens.GridPadding,
                top = padding.calculateTopPadding() + Dimens.SpacingMd,
                bottom = padding.calculateBottomPadding() + Dimens.SpacingXxl,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GridCellSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.GridCellSpacing),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header
            item(span = { GridItemSpan(2) }) {
                DiscoverHeader(
                    onLoginClick = viewModel::onLoginClick,
                    liveCount = state.cameras.count { it.status == CameraStatus.ONLINE },
                )
            }

            // Category chips
            if (state.categories.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    CategoryChips(
                        categories = state.categories,
                        selectedCategory = state.selectedCategory,
                        onCategorySelected = viewModel::onCategorySelected,
                    )
                }
            }

            // Local cameras section
            if (state.localCameras.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    SectionHeader(title = "Minhas Câmeras")
                }

                items(state.localCameras, key = { "local-${it.id}" }) { camera ->
                    CameraCard(
                        name = camera.name,
                        status = camera.status.toConnectionStatus(),
                        thumbnailUrl = camera.thumbnailUrl,
                        onClick = { viewModel.onCameraClick(camera.id) },
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                }
            }

            // Public cameras section header
            item(span = { GridItemSpan(2) }) {
                SectionHeader(title = "Câmeras Públicas")
            }

            // Content states
            when {
                state.isLoading -> {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                state.error != null -> {
                    item(span = { GridItemSpan(2) }) {
                        DiscoverErrorState(
                            message = state.error!!,
                            onRetry = viewModel::onRetry,
                        )
                    }
                }

                state.cameras.isEmpty() -> {
                    item(span = { GridItemSpan(2) }) {
                        DiscoverEmptyState()
                    }
                }

                else -> {
                    items(state.cameras, key = { it.id }) { camera ->
                        CameraCard(
                            name = camera.name,
                            status = camera.status.toConnectionStatus(),
                            thumbnailUrl = camera.thumbnailUrl,
                            onClick = { viewModel.onCameraClick(camera.id) },
                            isDemo = camera.isDemo,
                            isLive = camera.hlsUrl != null && camera.status == CameraStatus.ONLINE,
                        )
                    }

                    // Loading more indicator
                    if (state.isLoadingMore) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.SpacingLg),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.IconMd),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            // CTA banner
            if (!state.isLoading && state.error == null) {
                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(Dimens.SpacingMd))
                    DiscoverCtaBanner(onLoginClick = viewModel::onLoginClick)
                }
            }
        }
    }
}

@Composable
private fun DiscoverHeader(
    onLoginClick: () -> Unit,
    liveCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs),
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Eye,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.IconMd),
                )
                Text(
                    text = "VigiPro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            TextButton(onClick = onLoginClick) {
                Text(
                    text = "Entrar",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Text(
            text = "Câmeras ao vivo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingXs))

        if (liveCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                )
                Text(
                    text = "$liveCount transmitindo agora",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingMd))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(
    categories: List<com.vigipro.core.network.cloud.CategoryDto>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
        contentPadding = PaddingValues(bottom = Dimens.SpacingMd),
    ) {
        // "Todos" chip
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("Todos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }

        items(categories, key = { it.key }) { category ->
            FilterChip(
                selected = selectedCategory == category.key,
                onClick = { onCategorySelected(category.key) },
                label = { Text("${category.label} (${category.count})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(vertical = Dimens.SpacingXs),
    )
}

@Composable
private fun DiscoverCtaBanner(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ),
            )
            .padding(Dimens.SpacingLg),
    ) {
        Column {
            Icon(
                imageVector = PhosphorIcons.Regular.VideoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.IconLg),
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            Text(
                text = "Monitore suas câmeras",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
            Text(
                text = "Adicione câmeras RTSP, configure alertas e acesse de qualquer lugar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingMd))
            Button(onClick = onLoginClick) {
                Text(
                    text = "Criar conta grátis",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DiscoverErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
    ) {
        Icon(
            imageVector = PhosphorIcons.Regular.ArrowClockwise,
            contentDescription = null,
            modifier = Modifier.size(Dimens.IconXl),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry) {
            Text("Tentar novamente")
        }
    }
}

@Composable
private fun DiscoverEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
    ) {
        Icon(
            imageVector = PhosphorIcons.Regular.VideoCamera,
            contentDescription = null,
            modifier = Modifier.size(Dimens.IconXxl),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        )
        Text(
            text = "Nenhuma câmera disponível",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun CameraStatus.toConnectionStatus(): ConnectionStatus = when (this) {
    CameraStatus.ONLINE -> ConnectionStatus.ONLINE
    CameraStatus.ERROR -> ConnectionStatus.ERROR
    CameraStatus.OFFLINE -> ConnectionStatus.OFFLINE
}
