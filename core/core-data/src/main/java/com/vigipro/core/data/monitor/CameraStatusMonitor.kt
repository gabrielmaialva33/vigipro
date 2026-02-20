package com.vigipro.core.data.monitor

import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.rtsp.RtspConnectionTester
import com.vigipro.core.model.CameraStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraStatusMonitor @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val connectionTester: RtspConnectionTester,
) {
    private var monitorJob: Job? = null

    fun start(scope: CoroutineScope) {
        stop()
        monitorJob = scope.launch {
            while (isActive) {
                checkAllCameras()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private suspend fun checkAllCameras() {
        val cameras = cameraRepository.getAllCameras().first()
        for (camera in cameras) {
            val rtspUrl = camera.rtspUrl ?: continue
            val result = connectionTester.testConnection(rtspUrl)
            val newStatus = if (result.success) CameraStatus.ONLINE else CameraStatus.OFFLINE
            if (camera.status != newStatus) {
                cameraRepository.updateCameraStatus(camera.id, newStatus)
            }
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 60_000L
    }
}
