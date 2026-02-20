package com.vigipro.feature.player.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.RecordingRepository
import com.vigipro.core.model.Recording
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

data class RecordingsState(
    val recordings: List<Recording> = emptyList(),
    val isLoading: Boolean = true,
    val recordingToDelete: Recording? = null,
)

sealed interface RecordingsSideEffect {
    data class NavigateToPlayback(val filePath: String) : RecordingsSideEffect
    data class ShareRecording(val filePath: String) : RecordingsSideEffect
    data object NavigateBack : RecordingsSideEffect
}

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
) : ViewModel(), ContainerHost<RecordingsState, RecordingsSideEffect> {

    override val container = viewModelScope.container<RecordingsState, RecordingsSideEffect>(RecordingsState()) {
        observeRecordings()
    }

    private fun observeRecordings() = intent {
        recordingRepository.getAllRecordings().collect { recordings ->
            reduce {
                state.copy(
                    recordings = recordings.sortedByDescending { it.startTime },
                    isLoading = false,
                )
            }
        }
    }

    fun onRecordingClick(recording: Recording) = intent {
        postSideEffect(RecordingsSideEffect.NavigateToPlayback(recording.filePath))
    }

    fun onShareClick(recording: Recording) = intent {
        postSideEffect(RecordingsSideEffect.ShareRecording(recording.filePath))
    }

    fun onDeleteClick(recording: Recording) = intent {
        reduce { state.copy(recordingToDelete = recording) }
    }

    fun onConfirmDelete() = intent {
        val recording = state.recordingToDelete ?: return@intent
        recordingRepository.deleteRecording(recording.id)
        reduce { state.copy(recordingToDelete = null) }
    }

    fun onDismissDelete() = intent {
        reduce { state.copy(recordingToDelete = null) }
    }

    fun onBack() = intent {
        postSideEffect(RecordingsSideEffect.NavigateBack)
    }
}
