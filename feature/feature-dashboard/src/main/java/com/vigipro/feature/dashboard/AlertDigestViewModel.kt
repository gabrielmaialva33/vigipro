package com.vigipro.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.EventRepository
import com.vigipro.core.model.CameraEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

data class AlertDigestState(
    val recentEvents: List<CameraEvent> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class AlertDigestViewModel @Inject constructor(
    private val eventRepository: EventRepository,
) : ViewModel(), ContainerHost<AlertDigestState, Nothing> {

    override val container = viewModelScope.container<AlertDigestState, Nothing>(
        AlertDigestState(),
    ) {
        observeEvents()
    }

    private fun observeEvents() = intent {
        eventRepository.getRecentEvents(limit = 500).collect { events ->
            reduce { state.copy(recentEvents = events, isLoading = false) }
        }
    }
}
