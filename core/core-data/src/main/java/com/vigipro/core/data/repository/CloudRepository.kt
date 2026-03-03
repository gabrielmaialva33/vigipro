package com.vigipro.core.data.repository

import com.vigipro.core.model.Camera
import com.vigipro.core.model.CameraEvent
import com.vigipro.core.network.cloud.CategoriesResponse
import com.vigipro.core.network.cloud.CloudHealthResponse
import com.vigipro.core.network.cloud.PaginationMeta

/**
 * Repository for VigiPro Cloud communication.
 * Syncs camera metadata (never credentials) and events to the Phoenix backend.
 * Auth token is automatically provided from the Supabase session.
 */
interface CloudRepository {

    /** Check if the cloud server is reachable. */
    suspend fun healthCheck(): Result<CloudHealthResponse>

    /** Sync all local cameras to the cloud. Credentials are stripped. */
    suspend fun syncCameras(cameras: List<Camera>): Result<Int>

    /** Update a single camera's status on the cloud. */
    suspend fun updateCameraStatus(cameraId: String, status: String): Result<Unit>

    /** Log a camera event to the cloud. */
    suspend fun logEvent(event: CameraEvent): Result<Unit>

    /** Log multiple events in batch. */
    suspend fun logEventsBatch(events: List<CameraEvent>): Result<Int>

    /** Fetch the user's cameras from the cloud. */
    suspend fun fetchCloudCameras(): Result<List<Camera>>

    /** Fetch public demo cameras with HLS streaming URLs. */
    suspend fun fetchDemoCameras(): Result<List<Camera>>

    /** Fetch paginated public cameras, optionally filtered by category. */
    suspend fun fetchPublicCameras(
        category: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<Pair<List<Camera>, PaginationMeta>>

    /** Fetch available public camera categories. */
    suspend fun fetchPublicCategories(): Result<CategoriesResponse>
}
