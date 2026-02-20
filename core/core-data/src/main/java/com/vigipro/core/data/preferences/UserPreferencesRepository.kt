package com.vigipro.core.data.preferences

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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

    override suspend fun clearCache() {
        safeEdit { it.clear() }
    }

    companion object {
        private const val TAG = "UserPreferences"
    }
}
