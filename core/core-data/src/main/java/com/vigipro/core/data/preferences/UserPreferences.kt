package com.vigipro.core.data.preferences

enum class VideoQuality { AUTO, HD, SD }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val videoQuality: VideoQuality = VideoQuality.AUTO,
    val defaultGridColumns: Int = 2,
    val audioEnabledByDefault: Boolean = false,
    val statusMonitorIntervalMs: Long = 60_000L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notifyOffline: Boolean = true,
    val notifyOnline: Boolean = false,
    val watermarkEnabled: Boolean = true,
)
