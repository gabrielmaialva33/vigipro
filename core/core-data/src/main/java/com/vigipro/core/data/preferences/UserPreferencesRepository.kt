package com.vigipro.core.data.preferences

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun updateVideoQuality(quality: VideoQuality)
    suspend fun updateDefaultGridColumns(columns: Int)
    suspend fun updateAudioEnabled(enabled: Boolean)
    suspend fun updateMonitorInterval(intervalMs: Long)
    suspend fun updateThemeMode(mode: ThemeMode)
    suspend fun updateNotifyOffline(enabled: Boolean)
    suspend fun updateNotifyOnline(enabled: Boolean)
    suspend fun updateWatermarkEnabled(enabled: Boolean)
    suspend fun updateDetectionEnabled(enabled: Boolean)
    suspend fun updateDetectionConfidenceThreshold(threshold: Float)
    suspend fun updateDetectPersons(enabled: Boolean)
    suspend fun updateDetectVehicles(enabled: Boolean)
    suspend fun updateDetectAnimals(enabled: Boolean)
    suspend fun updateNotifyPersonDetected(enabled: Boolean)
    suspend fun updateDetectionIntervalMs(intervalMs: Long)
    suspend fun updateTalkbackEnabled(enabled: Boolean)
    suspend fun updateBiometricLockEnabled(enabled: Boolean)
    suspend fun updateGeofencingEnabled(enabled: Boolean)
    suspend fun updateDefaultGeofenceRadius(radius: Float)
    suspend fun updatePrivacyMaskingEnabled(enabled: Boolean)
    suspend fun updateAlertDigestEnabled(enabled: Boolean)
    suspend fun updateAlertDigestInterval(minutes: Int)
    suspend fun updateAlertDigestQuietHoursStart(hour: Int)
    suspend fun updateAlertDigestQuietHoursEnd(hour: Int)
    suspend fun clearCache()
}

@Singleton
class LocalUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private object Keys {
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val DEFAULT_GRID_COLUMNS = intPreferencesKey("default_grid_columns")
        val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled_default")
        val MONITOR_INTERVAL = longPreferencesKey("monitor_interval_ms")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFY_OFFLINE = booleanPreferencesKey("notify_offline")
        val NOTIFY_ONLINE = booleanPreferencesKey("notify_online")
        val WATERMARK_ENABLED = booleanPreferencesKey("watermark_enabled")
        val DETECTION_ENABLED = booleanPreferencesKey("detection_enabled")
        val DETECTION_CONFIDENCE = floatPreferencesKey("detection_confidence")
        val DETECT_PERSONS = booleanPreferencesKey("detect_persons")
        val DETECT_VEHICLES = booleanPreferencesKey("detect_vehicles")
        val DETECT_ANIMALS = booleanPreferencesKey("detect_animals")
        val NOTIFY_PERSON_DETECTED = booleanPreferencesKey("notify_person_detected")
        val DETECTION_INTERVAL = longPreferencesKey("detection_interval_ms")
        val TALKBACK_ENABLED = booleanPreferencesKey("talkback_enabled")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val GEOFENCING_ENABLED = booleanPreferencesKey("geofencing_enabled")
        val DEFAULT_GEOFENCE_RADIUS = floatPreferencesKey("default_geofence_radius")
        val PRIVACY_MASKING_ENABLED = booleanPreferencesKey("privacy_masking_enabled")
        val ALERT_DIGEST_ENABLED = booleanPreferencesKey("alert_digest_enabled")
        val ALERT_DIGEST_INTERVAL = intPreferencesKey("alert_digest_interval_minutes")
        val ALERT_DIGEST_QUIET_START = intPreferencesKey("alert_digest_quiet_hours_start")
        val ALERT_DIGEST_QUIET_END = intPreferencesKey("alert_digest_quiet_hours_end")
    }

    override val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserPreferences(
                videoQuality = prefs[Keys.VIDEO_QUALITY]?.let {
                    runCatching { VideoQuality.valueOf(it) }.getOrNull()
                } ?: VideoQuality.AUTO,
                defaultGridColumns = prefs[Keys.DEFAULT_GRID_COLUMNS] ?: 2,
                audioEnabledByDefault = prefs[Keys.AUDIO_ENABLED] ?: false,
                statusMonitorIntervalMs = prefs[Keys.MONITOR_INTERVAL] ?: 60_000L,
                themeMode = prefs[Keys.THEME_MODE]?.let {
                    runCatching { ThemeMode.valueOf(it) }.getOrNull()
                } ?: ThemeMode.SYSTEM,
                notifyOffline = prefs[Keys.NOTIFY_OFFLINE] ?: true,
                notifyOnline = prefs[Keys.NOTIFY_ONLINE] ?: false,
                watermarkEnabled = prefs[Keys.WATERMARK_ENABLED] ?: true,
                detectionEnabled = prefs[Keys.DETECTION_ENABLED] ?: false,
                detectionConfidenceThreshold = prefs[Keys.DETECTION_CONFIDENCE] ?: 0.5f,
                detectPersons = prefs[Keys.DETECT_PERSONS] ?: true,
                detectVehicles = prefs[Keys.DETECT_VEHICLES] ?: true,
                detectAnimals = prefs[Keys.DETECT_ANIMALS] ?: false,
                notifyPersonDetected = prefs[Keys.NOTIFY_PERSON_DETECTED] ?: false,
                detectionIntervalMs = prefs[Keys.DETECTION_INTERVAL] ?: 750L,
                talkbackEnabled = prefs[Keys.TALKBACK_ENABLED] ?: true,
                biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: false,
                geofencingEnabled = prefs[Keys.GEOFENCING_ENABLED] ?: false,
                defaultGeofenceRadius = prefs[Keys.DEFAULT_GEOFENCE_RADIUS] ?: 200f,
                privacyMaskingEnabled = prefs[Keys.PRIVACY_MASKING_ENABLED] ?: true,
                alertDigestEnabled = prefs[Keys.ALERT_DIGEST_ENABLED] ?: false,
                alertDigestIntervalMinutes = prefs[Keys.ALERT_DIGEST_INTERVAL] ?: 15,
                alertDigestQuietHoursStart = prefs[Keys.ALERT_DIGEST_QUIET_START] ?: 22,
                alertDigestQuietHoursEnd = prefs[Keys.ALERT_DIGEST_QUIET_END] ?: 7,
            )
        }

    private suspend fun safeEdit(transform: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        try {
            dataStore.edit(transform)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write preferences", e)
        }
    }

    override suspend fun updateVideoQuality(quality: VideoQuality) {
        safeEdit { it[Keys.VIDEO_QUALITY] = quality.name }
    }

    override suspend fun updateDefaultGridColumns(columns: Int) {
        val safeColumns = columns.coerceIn(1, 4)
        safeEdit { it[Keys.DEFAULT_GRID_COLUMNS] = safeColumns }
    }

    override suspend fun updateAudioEnabled(enabled: Boolean) {
        safeEdit { it[Keys.AUDIO_ENABLED] = enabled }
    }

    override suspend fun updateMonitorInterval(intervalMs: Long) {
        val safeInterval = intervalMs.coerceAtLeast(10_000L)
        safeEdit { it[Keys.MONITOR_INTERVAL] = safeInterval }
    }

    override suspend fun updateThemeMode(mode: ThemeMode) {
        safeEdit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun updateNotifyOffline(enabled: Boolean) {
        safeEdit { it[Keys.NOTIFY_OFFLINE] = enabled }
    }

    override suspend fun updateNotifyOnline(enabled: Boolean) {
        safeEdit { it[Keys.NOTIFY_ONLINE] = enabled }
    }

    override suspend fun updateWatermarkEnabled(enabled: Boolean) {
        safeEdit { it[Keys.WATERMARK_ENABLED] = enabled }
    }

    override suspend fun updateDetectionEnabled(enabled: Boolean) {
        safeEdit { it[Keys.DETECTION_ENABLED] = enabled }
    }

    override suspend fun updateDetectionConfidenceThreshold(threshold: Float) {
        safeEdit { it[Keys.DETECTION_CONFIDENCE] = threshold.coerceIn(0.1f, 0.95f) }
    }

    override suspend fun updateDetectPersons(enabled: Boolean) {
        safeEdit { it[Keys.DETECT_PERSONS] = enabled }
    }

    override suspend fun updateDetectVehicles(enabled: Boolean) {
        safeEdit { it[Keys.DETECT_VEHICLES] = enabled }
    }

    override suspend fun updateDetectAnimals(enabled: Boolean) {
        safeEdit { it[Keys.DETECT_ANIMALS] = enabled }
    }

    override suspend fun updateNotifyPersonDetected(enabled: Boolean) {
        safeEdit { it[Keys.NOTIFY_PERSON_DETECTED] = enabled }
    }

    override suspend fun updateDetectionIntervalMs(intervalMs: Long) {
        safeEdit { it[Keys.DETECTION_INTERVAL] = intervalMs.coerceIn(500L, 2000L) }
    }

    override suspend fun updateTalkbackEnabled(enabled: Boolean) {
        safeEdit { it[Keys.TALKBACK_ENABLED] = enabled }
    }

    override suspend fun updateBiometricLockEnabled(enabled: Boolean) {
        safeEdit { it[Keys.BIOMETRIC_LOCK_ENABLED] = enabled }
    }

    override suspend fun updateGeofencingEnabled(enabled: Boolean) {
        safeEdit { it[Keys.GEOFENCING_ENABLED] = enabled }
    }

    override suspend fun updateDefaultGeofenceRadius(radius: Float) {
        safeEdit { it[Keys.DEFAULT_GEOFENCE_RADIUS] = radius.coerceIn(50f, 5000f) }
    }

    override suspend fun updatePrivacyMaskingEnabled(enabled: Boolean) {
        safeEdit { it[Keys.PRIVACY_MASKING_ENABLED] = enabled }
    }

    override suspend fun updateAlertDigestEnabled(enabled: Boolean) {
        safeEdit { it[Keys.ALERT_DIGEST_ENABLED] = enabled }
    }

    override suspend fun updateAlertDigestInterval(minutes: Int) {
        safeEdit { it[Keys.ALERT_DIGEST_INTERVAL] = minutes.coerceIn(5, 60) }
    }

    override suspend fun updateAlertDigestQuietHoursStart(hour: Int) {
        safeEdit { it[Keys.ALERT_DIGEST_QUIET_START] = hour.coerceIn(0, 23) }
    }

    override suspend fun updateAlertDigestQuietHoursEnd(hour: Int) {
        safeEdit { it[Keys.ALERT_DIGEST_QUIET_END] = hour.coerceIn(0, 23) }
    }

    override suspend fun clearCache() {
        safeEdit { it.clear() }
    }

    companion object {
        private const val TAG = "UserPreferences"
    }
}
