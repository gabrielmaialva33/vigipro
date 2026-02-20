package com.vigipro.feature.player.patrol

import com.vigipro.core.model.PatrolRoute
import com.vigipro.feature.player.ptz.OnvifPtzClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class PatrolManager @Inject constructor() {

    data class PatrolState(
        val isPatrolling: Boolean = false,
        val currentWaypointIndex: Int = 0,
        val currentPresetName: String = "",
        val remainingDwellSeconds: Int = 0,
    )

    private val _state = MutableStateFlow(PatrolState())
    val state: StateFlow<PatrolState> = _state.asStateFlow()

    private var patrolJob: Job? = null
    private var ptzClient: OnvifPtzClient? = null

    fun attach(client: OnvifPtzClient) {
        ptzClient = client
    }

    fun startPatrol(route: PatrolRoute, scope: CoroutineScope) {
        stopPatrol()
        if (route.waypoints.isEmpty()) return

        patrolJob = scope.launch {
            _state.value = PatrolState(isPatrolling = true)

            do {
                for ((index, waypoint) in route.waypoints.withIndex()) {
                    if (!isActive) return@launch

                    _state.value = _state.value.copy(
                        currentWaypointIndex = index,
                        currentPresetName = waypoint.presetName,
                        remainingDwellSeconds = waypoint.dwellTimeSeconds,
                    )

                    // Move to preset
                    val client = ptzClient ?: return@launch
                    try {
                        client.gotoPreset(waypoint.presetToken)
                    } catch (_: Exception) {
                        // Continue patrol even if one preset fails
                    }

                    // Dwell with countdown
                    for (sec in waypoint.dwellTimeSeconds downTo 1) {
                        if (!isActive) return@launch
                        _state.value = _state.value.copy(remainingDwellSeconds = sec)
                        delay(1000L)
                    }
                }
            } while (route.repeatForever && isActive)

            _state.value = PatrolState(isPatrolling = false)
        }
    }

    fun stopPatrol() {
        patrolJob?.cancel()
        patrolJob = null
        _state.value = PatrolState(isPatrolling = false)
    }

    fun detach() {
        stopPatrol()
        ptzClient = null
    }
}
