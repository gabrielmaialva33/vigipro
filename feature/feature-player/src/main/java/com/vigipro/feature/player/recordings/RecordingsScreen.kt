package com.vigipro.feature.player.recordings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.vigipro.core.model.Recording
import com.vigipro.core.ui.components.EmptyState
import com.vigipro.core.ui.components.LoadingIndicator
import com.vigipro.core.ui.components.VigiProTopBar
import com.vigipro.core.ui.theme.Dimens
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    onBack: () -> Unit,
    onNavigateToPlayback: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordingsViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is RecordingsSideEffect.NavigateToPlayback -> onNavigateToPlayback(sideEffect.filePath)
            is RecordingsSideEffect.ShareRecording -> {
                val file = File(sideEffect.filePath)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartilhar gravacao"))
                }
            }
            RecordingsSideEffect.NavigateBack -> onBack()
        }
    }

    // Delete confirmation dialog
    if (state.recordingToDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text("Excluir Gravacao") },
            text = {
                Text(
                    "Tem certeza que deseja excluir a gravacao de \"${state.recordingToDelete!!.cameraName}\"? " +
                        "O arquivo sera removido permanentemente.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDelete) {
                    Text("Cancelar")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            VigiProTopBar(
                title = "Gravacoes",
                onBackClick = viewModel::onBack,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                LoadingIndicator(
                    message = "Carregando gravacoes...",
                    modifier = Modifier.padding(padding),
                )
            }
            state.recordings.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.VideoLibrary,
                    title = "Nenhuma gravacao",
                    subtitle = "Grave videos das suas cameras para revisa-los aqui",
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                RecordingsList(
                    recordings = state.recordings,
                    onRecordingClick = viewModel::onRecordingClick,
                    onShareClick = viewModel::onShareClick,
                    onDeleteClick = viewModel::onDeleteClick,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun RecordingsList(
    recordings: List<Recording>,
    onRecordingClick: (Recording) -> Unit,
    onShareClick: (Recording) -> Unit,
    onDeleteClick: (Recording) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = recordings.groupBy { recording ->
        getDateGroup(recording.startTime)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = Dimens.SpacingLg,
            vertical = Dimens.SpacingSm,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm),
    ) {
        grouped.forEach { (group, groupRecordings) ->
            item(key = "header_$group") {
                Text(
                    text = group,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        top = Dimens.SpacingMd,
                        bottom = Dimens.SpacingXs,
                    ),
                )
            }

            items(
                items = groupRecordings,
                key = { it.id },
            ) { recording ->
                RecordingCard(
                    recording = recording,
                    onClick = { onRecordingClick(recording) },
                    onShareClick = { onShareClick(recording) },
                    onDeleteClick = { onDeleteClick(recording) },
                )
            }
        }
    }
}

@Composable
private fun RecordingCard(
    recording: Recording,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = recording.cameraName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
                ) {
                    Text(
                        text = formatDateTime(recording.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDuration(recording.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatFileSize(recording.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimens.SpacingSm))

            Row {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproduzir",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartilhar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun getDateGroup(timestampMs: Long): String {
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    calendar.timeInMillis = timestampMs

    val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

    today.add(Calendar.DAY_OF_YEAR, -1)
    val isYesterday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

    return when {
        isToday -> "Hoje"
        isYesterday -> "Ontem"
        else -> {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            formatter.format(Date(timestampMs))
        }
    }
}

private fun formatDateTime(timestampMs: Long): String {
    val formatter = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
    return formatter.format(Date(timestampMs))
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        "%.1f MB".format(mb)
    } else {
        val kb = bytes / 1024.0
        "%.0f KB".format(kb)
    }
}
