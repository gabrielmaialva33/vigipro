package com.vigipro.core.data.repository

import com.vigipro.core.data.db.WebhookDao
import com.vigipro.core.data.db.WebhookEntity
import com.vigipro.core.model.HttpMethod
import com.vigipro.core.model.WebhookAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface WebhookRepository {
    fun getWebhooksByCamera(cameraId: String): Flow<List<WebhookAction>>
    suspend fun getById(id: String): WebhookAction?
    suspend fun saveWebhook(webhook: WebhookAction)
    suspend fun deleteWebhook(id: String)
}

@Singleton
class LocalWebhookRepository @Inject constructor(
    private val webhookDao: WebhookDao,
) : WebhookRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getWebhooksByCamera(cameraId: String): Flow<List<WebhookAction>> =
        webhookDao.getWebhooksByCamera(cameraId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): WebhookAction? =
        webhookDao.getById(id)?.toDomain()

    override suspend fun saveWebhook(webhook: WebhookAction) {
        val entity = WebhookEntity(
            id = webhook.id.ifBlank { UUID.randomUUID().toString() },
            cameraId = webhook.cameraId,
            name = webhook.name,
            url = webhook.url,
            method = webhook.method.name,
            headers = json.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                webhook.headers,
            ),
            body = webhook.body,
            icon = webhook.icon,
        )
        webhookDao.insert(entity)
    }

    override suspend fun deleteWebhook(id: String) {
        webhookDao.deleteById(id)
    }

    private fun WebhookEntity.toDomain() = WebhookAction(
        id = id,
        cameraId = cameraId,
        name = name,
        url = url,
        method = HttpMethod.valueOf(method),
        headers = try {
            json.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                headers,
            )
        } catch (_: Exception) {
            emptyMap()
        },
        body = body,
        icon = icon,
    )
}
