package com.vigipro.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vigipro.core.model.CameraEvent
import com.vigipro.core.model.CameraEventType
import com.vigipro.core.ui.components.EmptyState
import com.vigipro.core.ui.components.LoadingIndicator
import com.vigipro.core.ui.theme.Dimens
import com.vigipro.core.ui.theme.EventCameraAdded
import com.vigipro.core.ui.theme.EventCameraRemoved
import com.vigipro.core.ui.theme.EventDetection
import com.vigipro.core.ui.theme.EventOffline
import com.vigipro.core.ui.theme.EventOnline
import com.vigipro.core.ui.theme.EventSnapshot
import org.orbitmvi.orbit.compose.collectAsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDigestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlertDigestViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Resumo de Alertas")
                        if (state.recentEvents.isNotEmpty()) {
                            Text(
                                text = "${state.recentEvents.size} eventos recentes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PhosphorIcons.Regular.ArrowLeft, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                LoadingIndicator(
                    message = "Carregando resumo...",
                    modifier = Modifier.padding(padding),
                )
            }
            state.recentEvents.isEmpty() -> {
                EmptyState(
                    icon = PhosphorIcons.Regular.FileText,
                    title = "Nenhum evento recente",
                    subtitle = "Os resumos de atividade das cameras aparecerao aqui quando houver eventos",
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                val now = System.currentTimeMillis()
                val oneHourAgo = now - 3_600_000L
                val sixHoursAgo = now - 21_600_000L
                val oneDayAgo = now - 86_400_000L

                val lastHour = state.recentEvents.filter { it.timestamp >= oneHourAgo }
                val last6Hours = state.recentEvents.filter {
                    it.timestamp in sixHoursAgo until oneHourAgo
                }
                val last24Hours = state.recentEvents.filter {
                    it.timestamp in oneDayAgo until sixHoursAgo
                }
                val older = state.recentEvents.filter { it.timestamp < oneDayAgo }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
                ) {
                    if (lastHour.isNotEmpty()) {
                        item {
                            DigestPeriodSection(
                                title = "Ultima hora",
                                events = lastHour,
                            )
                        }
                    }
                    if (last6Hours.isNotEmpty()) {
                        item {
                            DigestPeriodSection(
                                title = "Ultimas 6 horas",
                                events = last6Hours,
                            )
                        }
                    }
                    if (last24Hours.isNotEmpty()) {
                        item {
                            DigestPeriodSection(
                                title = "Ultimas 24 horas",
                                events = last24Hours,
                            )
                        }
                    }
                    if (older.isNotEmpty()) {
                        item {
                            DigestPeriodSection(
                                title = "Mais antigos",
                                events = older,
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(Dimens.SpacingXl)) }
                }
            }
        }
    }
}

@Composable
private fun DigestPeriodSection(
    title: String,
    events: List<CameraEvent>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }
    val grouped = events.groupBy { it.cameraName }

    Column(modifier = modifier.padding(horizontal = Dimens.SpacingLg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = Dimens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Badge(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = "${events.size}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(modifier = Modifier.width(Dimens.SpacingSm))
            Icon(
                imageVector = if (expanded) PhosphorIcons.Regular.CaretUp else PhosphorIcons.Regular.CaretDown,
                contentDescription = if (expanded) "Recolher" else "Expandir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.IconMd),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)) {
                // Camera summary cards
                grouped.forEach { (cameraName, cameraEvents) ->
                    DigestCameraSummaryCard(
                        cameraName = cameraName,
                        events = cameraEvents,
                    )
                }
            }
        }
    }
}

@Composable
private fun DigestCameraSummaryCard(
    cameraName: String,
    events: List<CameraEvent>,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDetails = !showDetails },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(Dimens.SpacingSm),
    ) {
        Column(modifier = Modifier.padding(Dimens.SpacingMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = cameraName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                EventTypeBadges(events = events)
                Spacer(modifier = Modifier.width(Dimens.SpacingXs))
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Text(
                        text = "${events.size}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            AnimatedVisibility(
                visible = showDetails,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(top = Dimens.SpacingSm),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXxs),
                ) {
                    events.forEach { event ->
                        DigestEventItem(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventTypeBadges(
    events: List<CameraEvent>,
    modifier: Modifier = Modifier,
) {
    val offlineCount = events.count { it.eventType == CameraEventType.WENT_OFFLINE }
    val onlineCount = events.count { it.eventType == CameraEventType.CAME_ONLINE }
    val detectionCount = events.count { it.eventType == CameraEventType.OBJECT_DETECTED }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXxs)) {
        if (offlineCount > 0) {
            Surface(
                color = EventOffline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = "$offlineCount off",
                    style = MaterialTheme.typography.labelSmall,
                    color = EventOffline,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        if (onlineCount > 0) {
            Surface(
                color = EventOnline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = "$onlineCount on",
                    style = MaterialTheme.typography.labelSmall,
                    color = EventOnline,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        if (detectionCount > 0) {
            Surface(
                color = EventDetection.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = "$detectionCount det",
                    style = MaterialTheme.typography.labelSmall,
                    color = EventDetection,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun DigestEventItem(
    event: CameraEvent,
    modifier: Modifier = Modifier,
) {
    val (icon, color, label) = digestEventVisuals(event.eventType)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingXxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(Dimens.IconSm),
        )
        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = digestFormatTime(event.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class DigestEventVisuals(
    val icon: ImageVector,
    val color: Color,
    val label: String,
)

private fun digestEventVisuals(type: CameraEventType): DigestEventVisuals = when (type) {
    CameraEventType.WENT_OFFLINE -> DigestEventVisuals(
        icon = PhosphorIcons.Regular.CloudSlash,
        color = EventOffline,
        label = "Ficou offline",
    )
    CameraEventType.CAME_ONLINE -> DigestEventVisuals(
        icon = PhosphorIcons.Regular.Cloud,
        color = EventOnline,
        label = "Voltou online",
    )
    CameraEventType.SNAPSHOT_TAKEN -> DigestEventVisuals(
        icon = PhosphorIcons.Regular.Camera,
        color = EventSnapshot,
        label = "Snapshot capturado",
    )
    CameraEventType.CAMERA_ADDED -> DigestEventVisuals(
        icon = PhosphorIcons.Regular.Sparkle,
        color = EventCameraAdded,
        label = "Camera adicionada",
    )
    CameraEventType.CAMERA_REMOVED -> DigestEventVisuals(
        icon = PhosphorIcons.Regular.Trash,
        color = EventCameraRemoved,
        label = "Camera removida",
    )
    CameraEventType.OBJECT_DETECTED -> DigestEventVisuals(
        icon = PhosphorIcons.Regular.Eye,
        color = EventDetection,
        label = "Objeto detectado",
    )
}

private fun digestFormatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(timestamp))
}
