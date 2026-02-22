package com.vigipro.core.data.seed

import com.vigipro.core.data.db.CameraDao
import com.vigipro.core.data.db.CameraEntity
import com.vigipro.core.data.db.CameraEventDao
import com.vigipro.core.data.db.CameraEventEntity
import com.vigipro.core.data.db.PrivacyZoneDao
import com.vigipro.core.data.db.PrivacyZoneEntity
import com.vigipro.core.data.db.SiteDao
import com.vigipro.core.data.db.WebhookDao
import com.vigipro.core.data.db.WebhookEntity
import android.util.Log
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevSeedHelper @Inject constructor(
    private val siteDao: SiteDao,
    private val cameraDao: CameraDao,
    private val cameraEventDao: CameraEventDao,
    private val webhookDao: WebhookDao,
    private val privacyZoneDao: PrivacyZoneDao,
) {
    private var seeded = false

    suspend fun seedIfEmpty() {
        if (seeded) return
        seeded = true

        val cameras = cameraDao.getAllCameras().first()
        if (cameras.isNotEmpty()) {
            Log.d(TAG,"DevSeed: cameras already exist (${cameras.size}), skipping")
            return
        }

        val sites = siteDao.getAllSites().first()
        if (sites.isEmpty()) {
            Log.d(TAG,"DevSeed: no sites yet, skipping seed")
            return
        }

        val siteId = sites.first().id
        Log.d(TAG,"DevSeed: seeding data for site $siteId")

        seedCameras(siteId)
        seedEvents()
        seedWebhooks()
        seedPrivacyZones()

        Log.d(TAG,"DevSeed: seed complete")
    }

    private suspend fun seedCameras(siteId: String) {
        val cameras = listOf(
            CameraEntity(
                id = CAM_1_ID,
                siteId = siteId,
                name = "Camera Teste RTSP",
                rtspUrl = "rtsp://10.42.0.1:8555/camera1",
                username = null,
                status = "online",
                ptzCapable = false,
                audioCapable = false,
                sortOrder = 0,
            ),
            CameraEntity(
                id = CAM_2_ID,
                siteId = siteId,
                name = "Entrada Principal",
                rtspUrl = "rtsp://10.42.0.1:8555/cam1",
                username = null,
                status = "online",
                ptzCapable = true,
                audioCapable = true,
                sortOrder = 1,
            ),
            CameraEntity(
                id = CAM_3_ID,
                siteId = siteId,
                name = "Estacionamento",
                rtspUrl = "rtsp://192.168.1.200:554/stream1",
                username = "admin",
                status = "offline",
                ptzCapable = false,
                audioCapable = false,
                sortOrder = 2,
            ),
        )
        cameraDao.insertAll(cameras)
    }

    private suspend fun seedEvents() {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val day = 24 * hour

        val events = listOf(
            // Camera 1 — online agora
            event(CAM_1_ID, "Camera Teste RTSP", "CAMERA_ADDED", now - 7 * day, "Camera adicionada ao sistema"),
            event(CAM_1_ID, "Camera Teste RTSP", "CAME_ONLINE", now - 7 * day + hour, null),
            event(CAM_1_ID, "Camera Teste RTSP", "OBJECT_DETECTED", now - 5 * day, "Pessoa detectada"),
            event(CAM_1_ID, "Camera Teste RTSP", "SNAPSHOT_TAKEN", now - 4 * day, "Snapshot salvo"),
            event(CAM_1_ID, "Camera Teste RTSP", "WENT_OFFLINE", now - 3 * day, "Timeout de conexao"),
            event(CAM_1_ID, "Camera Teste RTSP", "CAME_ONLINE", now - 3 * day + 30 * 60_000, null),
            event(CAM_1_ID, "Camera Teste RTSP", "OBJECT_DETECTED", now - 2 * day, "Veiculo detectado"),
            event(CAM_1_ID, "Camera Teste RTSP", "SNAPSHOT_TAKEN", now - day, "Snapshot automatico"),
            event(CAM_1_ID, "Camera Teste RTSP", "OBJECT_DETECTED", now - 6 * hour, "Pessoa detectada"),
            event(CAM_1_ID, "Camera Teste RTSP", "CAME_ONLINE", now - 2 * hour, null),

            // Camera 2 — ativa com PTZ
            event(CAM_2_ID, "Entrada Principal", "CAMERA_ADDED", now - 6 * day, "Camera adicionada ao sistema"),
            event(CAM_2_ID, "Entrada Principal", "CAME_ONLINE", now - 6 * day + hour, null),
            event(CAM_2_ID, "Entrada Principal", "OBJECT_DETECTED", now - 4 * day, "Pessoa detectada na entrada"),
            event(CAM_2_ID, "Entrada Principal", "OBJECT_DETECTED", now - 3 * day, "Veiculo detectado"),
            event(CAM_2_ID, "Entrada Principal", "SNAPSHOT_TAKEN", now - 2 * day, "Snapshot de patrulha"),
            event(CAM_2_ID, "Entrada Principal", "WENT_OFFLINE", now - day - 2 * hour, "Falha na rede"),
            event(CAM_2_ID, "Entrada Principal", "CAME_ONLINE", now - day, null),
            event(CAM_2_ID, "Entrada Principal", "OBJECT_DETECTED", now - 4 * hour, "Animal detectado"),

            // Camera 3 — offline
            event(CAM_3_ID, "Estacionamento", "CAMERA_ADDED", now - 5 * day, "Camera adicionada ao sistema"),
            event(CAM_3_ID, "Estacionamento", "CAME_ONLINE", now - 5 * day + hour, null),
            event(CAM_3_ID, "Estacionamento", "OBJECT_DETECTED", now - 4 * day, "Veiculo detectado"),
            event(CAM_3_ID, "Estacionamento", "SNAPSHOT_TAKEN", now - 3 * day, "Snapshot salvo"),
            event(CAM_3_ID, "Estacionamento", "WENT_OFFLINE", now - 2 * day, "Camera desconectada"),
        )
        events.forEach { cameraEventDao.insert(it) }
    }

    private suspend fun seedWebhooks() {
        val webhooks = listOf(
            WebhookEntity(
                id = "wh-001",
                cameraId = CAM_1_ID,
                name = "Alerta Telegram",
                url = "https://api.telegram.org/bot123/sendMessage",
                method = "POST",
                headers = """{"Content-Type": "application/json"}""",
                body = """{"chat_id": "123456", "text": "Movimento detectado na Camera Teste"}""",
                icon = "notifications",
            ),
            WebhookEntity(
                id = "wh-002",
                cameraId = CAM_2_ID,
                name = "Acender Luz",
                url = "http://10.42.0.1:8080/api/light/on",
                method = "POST",
                headers = "{}",
                body = null,
                icon = "power",
            ),
            WebhookEntity(
                id = "wh-003",
                cameraId = CAM_2_ID,
                name = "Sirene",
                url = "http://10.42.0.1:8080/api/alarm/trigger",
                method = "POST",
                headers = "{}",
                body = null,
                icon = "warning",
            ),
        )
        webhooks.forEach { webhookDao.insert(it) }
    }

    private suspend fun seedPrivacyZones() {
        val zones = listOf(
            PrivacyZoneEntity(
                id = "pz-001",
                cameraId = CAM_1_ID,
                label = "Janela do Vizinho",
                left = 0.65f,
                top = 0.1f,
                right = 0.95f,
                bottom = 0.45f,
            ),
            PrivacyZoneEntity(
                id = "pz-002",
                cameraId = CAM_1_ID,
                label = "Placa do Carro",
                left = 0.2f,
                top = 0.6f,
                right = 0.5f,
                bottom = 0.8f,
            ),
        )
        zones.forEach { privacyZoneDao.insertZone(it) }
    }

    private fun event(cameraId: String, cameraName: String, type: String, ts: Long, msg: String?) =
        CameraEventEntity(cameraId = cameraId, cameraName = cameraName, eventType = type, timestamp = ts, message = msg)

    companion object {
        private const val TAG = "DevSeed"
        private const val CAM_1_ID = "seed-cam-001-rtsp-test"
        private const val CAM_2_ID = "seed-cam-002-entrada"
        private const val CAM_3_ID = "seed-cam-003-estacionamento"
    }
}
