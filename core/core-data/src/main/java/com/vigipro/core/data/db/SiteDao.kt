package com.vigipro.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    @Query("SELECT * FROM sites ORDER BY name ASC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getSiteById(id: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sites: List<SiteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: SiteEntity)

    @Query("DELETE FROM sites WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sites")
    suspend fun deleteAll()
}
