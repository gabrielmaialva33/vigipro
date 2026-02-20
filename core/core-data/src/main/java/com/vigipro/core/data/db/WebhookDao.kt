package com.vigipro.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WebhookDao {
    @Query("SELECT * FROM webhooks WHERE camera_id = :cameraId")
    fun getWebhooksByCamera(cameraId: String): Flow<List<WebhookEntity>>

    @Query("SELECT * FROM webhooks WHERE id = :id")
    suspend fun getById(id: String): WebhookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(webhook: WebhookEntity)

    @Query("DELETE FROM webhooks WHERE id = :id")
    suspend fun deleteById(id: String)
}
