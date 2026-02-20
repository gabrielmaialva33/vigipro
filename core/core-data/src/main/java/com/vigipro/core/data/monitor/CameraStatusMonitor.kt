package com.vigipro.core.data.monitor

import com.vigipro.core.data.notification.CameraNotificationHelper
import com.vigipro.core.data.preferences.UserPreferencesRepository
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.repository.EventRepository
import com.vigipro.core.data.rtsp.RtspConnectionTester
import com.vigipro.core.model.CameraEventType
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
    private val eventRepository: EventRepository,
    private val notificationHelper: CameraNotificationHelper,
    private val preferencesRepository: UserPreferencesRepository,
) {
    private var monitorJob: Job? = null

    fun start(scope: CoroutineScope) {
        stop()
        monitorJob = scope.launch {
            eventRepository.cleanOldEvents(keepDays = 30)

            while (isActive) {
                checkAllCameras()
                val interval = preferencesRepository.userPreferences.first().statusMonitorIntervalMs
                delay(interval)
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private suspend fun checkAllCameras() {
        val cameras = cameraRepository.getAllCameras().first()
        val prefs = preferencesRepository.userPreferences.first()

        for (camera in cameras) {
            val rtspUrl = camera.rtspUrl ?: continue
            val result = connectionTester.testConnection(rtspUrl)
            val newStatus = if (result.success) CameraStatus.ONLINE else CameraStatus.OFFLINE

            if (camera.status != newStatus) {
                cameraRepository.updateCameraStatus(camera.id, newStatus)

                val eventType = if (newStatus == CameraStatus.ONLINE) {
                    CameraEventType.CAME_ONLINE
                } else {
                    CameraEventType.WENT_OFFLINE
                }

                eventRepository.logEvent(
                    cameraId = camera.id,
                    cameraName = camera.name,
                    type = eventType,
                )

                if (newStatus == CameraStatus.OFFLINE && prefs.notifyOffline) {
                    notificationHelper.notifyCameraOffline(camera.id, camera.name)
                } else if (newStatus == CameraStatus.ONLINE && prefs.notifyOnline) {
                    notificationHelper.notifyCameraOnline(camera.id, camera.name)
                }
            }
        }
    }
}
