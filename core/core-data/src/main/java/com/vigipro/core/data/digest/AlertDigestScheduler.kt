package com.vigipro.core.data.digest

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertDigestScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scheduleDigest(intervalMinutes: Int) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<AlertDigestWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES,
            (intervalMinutes / 3).toLong().coerceAtLeast(5), TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .addTag(DIGEST_WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                DIGEST_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
    }

    fun cancelDigest() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(DIGEST_WORK_NAME)
    }

    companion object {
        private const val DIGEST_WORK_NAME = "alert_digest"
        private const val DIGEST_WORK_TAG = "alert_digest_tag"
    }
}
