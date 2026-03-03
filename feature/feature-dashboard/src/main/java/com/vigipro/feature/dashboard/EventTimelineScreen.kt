package com.vigipro.feature.dashboard

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
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTimelineScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventTimelineViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Eventos")
                        if (state.events.isNotEmpty()) {
                            Text(
                                text = "${state.events.size} eventos",
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
                    message = "Carregando eventos...",
                    modifier = Modifier.padding(padding),
                )
            }
            state.events.isEmpty() -> {
                EmptyState(
                    icon = PhosphorIcons.Regular.Cloud,
                    title = "Nenhum evento",
                    subtitle = "Os eventos de status das câmeras aparecerão aqui automaticamente",
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                val grouped = state.events.groupBy { event ->
                    getDateLabel(event.timestamp)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs),
                ) {
                    grouped.forEach { (dateLabel, events) ->
                        item {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    horizontal = Dimens.SpacingLg,
                                    vertical = Dimens.SpacingSm,
                                ),
                            )
                        }
                        items(events, key = { it.id }) { event ->
                            EventCard(
                                event = event,
                                modifier = Modifier.padding(horizontal = Dimens.SpacingLg),
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
private fun EventCard(
    event: CameraEvent,
    modifier: Modifier = Modifier,
) {
    val (icon, color, label) = eventVisuals(event.eventType)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(Dimens.IconLg),
            )
            Spacer(modifier = Modifier.width(Dimens.SpacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.cameraName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
                event.message?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = formatTime(event.timestamp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class EventVisuals(
    val icon: ImageVector,
    val color: Color,
    val label: String,
)

private fun eventVisuals(type: CameraEventType): EventVisuals = when (type) {
    CameraEventType.WENT_OFFLINE -> EventVisuals(
        icon = PhosphorIcons.Regular.CloudSlash,
        color = EventOffline,
        label = "Ficou offline",
    )
    CameraEventType.CAME_ONLINE -> EventVisuals(
        icon = PhosphorIcons.Regular.Cloud,
        color = EventOnline,
        label = "Voltou online",
    )
    CameraEventType.SNAPSHOT_TAKEN -> EventVisuals(
        icon = PhosphorIcons.Regular.Camera,
        color = EventSnapshot,
        label = "Snapshot capturado",
    )
    CameraEventType.CAMERA_ADDED -> EventVisuals(
        icon = PhosphorIcons.Regular.Sparkle,
        color = EventCameraAdded,
        label = "Camera adicionada",
    )
    CameraEventType.CAMERA_REMOVED -> EventVisuals(
        icon = PhosphorIcons.Regular.Trash,
        color = EventCameraRemoved,
        label = "Camera removida",
    )
    CameraEventType.OBJECT_DETECTED -> EventVisuals(
        icon = PhosphorIcons.Regular.Eye,
        color = EventDetection,
        label = "Objeto detectado",
    )
}

private fun getDateLabel(timestamp: Long): String {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_YEAR)
    val year = cal.get(Calendar.YEAR)

    cal.timeInMillis = timestamp
    val eventDay = cal.get(Calendar.DAY_OF_YEAR)
    val eventYear = cal.get(Calendar.YEAR)

    return when {
        eventYear == year && eventDay == today -> "Hoje"
        eventYear == year && eventDay == today - 1 -> "Ontem"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(timestamp))
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(timestamp))
}
