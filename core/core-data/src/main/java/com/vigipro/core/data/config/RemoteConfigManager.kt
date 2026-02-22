package com.vigipro.core.data.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acesso tipado aos valores do Firebase Remote Config.
 * Valores sao buscados e ativados no VigiProApp.onCreate().
 * Pra alterar remotamente, use o console do Firebase.
 */
@Singleton
class RemoteConfigManager @Inject constructor() {

    private val config: FirebaseRemoteConfig
        get() = FirebaseRemoteConfig.getInstance()

    // Feature flags
    val isMultiviewEnabled: Boolean get() = config.getBoolean("feature_multiview_enabled")
    val isPtzEnabled: Boolean get() = config.getBoolean("feature_ptz_enabled")
    val isTalkbackEnabled: Boolean get() = config.getBoolean("feature_talkback_enabled")
    val isPatrolEnabled: Boolean get() = config.getBoolean("feature_patrol_enabled")
    val isPrivacyZonesEnabled: Boolean get() = config.getBoolean("feature_privacy_zones_enabled")
    val isWebhooksEnabled: Boolean get() = config.getBoolean("feature_webhooks_enabled")
    val isGeofenceEnabled: Boolean get() = config.getBoolean("feature_geofence_enabled")

    // Limites
    val maxCamerasPerSite: Int get() = config.getLong("max_cameras_per_site").toInt()
    val maxSitesPerUser: Int get() = config.getLong("max_sites_per_user").toInt()
    val cameraReconnectIntervalSeconds: Int get() = config.getLong("camera_reconnect_interval_seconds").toInt()
    val multiviewMaxCameras: Int get() = config.getLong("multiview_max_cameras").toInt()

    // Manutencao
    val isMaintenanceMode: Boolean get() = config.getBoolean("maintenance_mode")
    val maintenanceMessage: String get() = config.getString("maintenance_message")

    // Versao
    val minAppVersion: Int get() = config.getLong("min_app_version").toInt()
    val forceUpdateMessage: String get() = config.getString("force_update_message")
}
