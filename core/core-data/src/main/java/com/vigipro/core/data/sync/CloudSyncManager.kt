package com.vigipro.core.data.sync

import android.util.Log
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.repository.CloudRepository
import com.vigipro.core.data.repository.EventRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages synchronization between local data and VigiPro Cloud.
 * Call [syncAll] after authentication or when connectivity changes.
 *
 * Security: RTSP URLs and credentials are NEVER synced to the cloud.
 * Only camera metadata (name, site, capabilities, status) is sent.
 */
@Singleton
class CloudSyncManager @Inject constructor(
    private val cloudRepository: CloudRepository,
    private val cameraRepository: CameraRepository,
    private val eventRepository: EventRepository,
) {

    /**
     * Sync all local cameras to the cloud.
     * Returns true if sync succeeded.
     */
    suspend fun syncAll(): Boolean {
        return try {
            val cameras = cameraRepository.getAllCameras().first()
            val result = cloudRepository.syncCameras(cameras)
            result.fold(
                onSuccess = { count ->
                    Log.d(TAG, "Cloud sync: $count cameras synced")
                    true
                },
                onFailure = { e ->
                    Log.w(TAG, "Cloud sync failed", e)
                    false
                },
            )
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sync error", e)
            false
        }
    }

    /**
     * Sync recent events to the cloud.
     */
    suspend fun syncEvents(): Boolean {
        return try {
            val events = eventRepository.getRecentEvents(limit = 50).first()
            if (events.isEmpty()) return true

            val result = cloudRepository.logEventsBatch(events)
            result.fold(
                onSuccess = { count ->
                    Log.d(TAG, "Cloud sync: $count events synced")
                    true
                },
                onFailure = { e ->
                    Log.w(TAG, "Cloud event sync failed", e)
                    false
                },
            )
        } catch (e: Exception) {
            Log.w(TAG, "Cloud event sync error", e)
            false
        }
    }

    /**
     * Check if the cloud server is reachable.
     */
    suspend fun isCloudReachable(): Boolean {
        return cloudRepository.healthCheck().isSuccess
    }

    companion object {
        private const val TAG = "CloudSync"
    }
}
