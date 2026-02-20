package com.vigipro.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.EventRepository
import com.vigipro.core.model.CameraEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

data class EventTimelineState(
    val events: List<CameraEvent> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class EventTimelineViewModel @Inject constructor(
    private val eventRepository: EventRepository,
) : ViewModel(), ContainerHost<EventTimelineState, Nothing> {

    override val container = viewModelScope.container<EventTimelineState, Nothing>(
        EventTimelineState(),
    ) {
        observeEvents()
    }

    private fun observeEvents() = intent {
        eventRepository.getRecentEvents(limit = 200).collect { events ->
            reduce { state.copy(events = events, isLoading = false) }
        }
    }
}
