package com.vigipro.core.data.digest

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vigipro.core.data.db.CameraEventDao
import com.vigipro.core.data.notification.CameraNotificationHelper
import com.vigipro.core.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class AlertDigestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cameraEventDao: CameraEventDao,
    private val notificationHelper: CameraNotificationHelper,
    private val preferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = preferencesRepository.userPreferences.first()
        if (!prefs.alertDigestEnabled) return Result.success()

        // Check quiet hours
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (isInQuietHours(currentHour, prefs.alertDigestQuietHoursStart, prefs.alertDigestQuietHoursEnd)) {
            return Result.success()
        }

        // Get events since last digest
        val intervalMs = prefs.alertDigestIntervalMinutes * 60 * 1000L
        val since = System.currentTimeMillis() - intervalMs
        val events = cameraEventDao.getEventsSince(since)

        if (events.isEmpty()) return Result.success()

        // Group events by camera
        val grouped = events.groupBy { it.cameraName }

        // Build digest summary
        val totalEvents = events.size
        val camerasAffected = grouped.size
        val offlineCount = events.count { it.eventType == "WENT_OFFLINE" || it.eventType == "ERROR" }
        val onlineCount = events.count { it.eventType == "CAME_ONLINE" }
        val detectionCount = events.count { it.eventType.contains("DETECT", ignoreCase = true) }

        val title = "Resumo de Atividade"
        val message = buildString {
            append("$totalEvents eventos em $camerasAffected camera(s)")
            if (offlineCount > 0) append(" \u2022 $offlineCount offline")
            if (onlineCount > 0) append(" \u2022 $onlineCount reconectadas")
            if (detectionCount > 0) append(" \u2022 $detectionCount deteccoes")
        }

        val details = buildString {
            grouped.forEach { (camera, cameraEvents) ->
                append("$camera: ${cameraEvents.size} evento(s)\n")
            }
        }.trim()

        notificationHelper.showDigestNotification(
            title = title,
            summary = message,
            details = details,
        )

        return Result.success()
    }

    private fun isInQuietHours(currentHour: Int, start: Int, end: Int): Boolean {
        return if (start < end) {
            currentHour in start until end
        } else {
            // Wraps midnight (e.g., 22:00 to 07:00)
            currentHour >= start || currentHour < end
        }
    }
}
