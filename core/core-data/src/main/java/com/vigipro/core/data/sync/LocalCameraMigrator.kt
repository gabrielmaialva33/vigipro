package com.vigipro.core.data.sync

import com.vigipro.core.data.db.CameraDao
import com.vigipro.core.model.LOCAL_SITE_ID
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Migrates cameras saved with LOCAL_SITE_ID to a real site
 * after the user creates an account and their first site.
 */
@Singleton
class LocalCameraMigrator @Inject constructor(
    private val cameraDao: CameraDao,
) {

    suspend fun hasLocalCameras(): Boolean {
        return cameraDao.countBySite(LOCAL_SITE_ID) > 0
    }

    suspend fun migrateLocalCameras(targetSiteId: String): Int {
        val localCameras = cameraDao.getCamerasBySiteSnapshot(LOCAL_SITE_ID)
        if (localCameras.isEmpty()) return 0

        localCameras.forEach { camera ->
            cameraDao.update(camera.copy(siteId = targetSiteId))
        }

        Timber.i("Migrated ${localCameras.size} local cameras to site $targetSiteId")
        return localCameras.size
    }
}
