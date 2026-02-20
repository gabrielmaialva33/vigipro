package com.vigipro.core.data.repository

import com.vigipro.core.model.Site
import com.vigipro.core.model.SiteMember
import kotlinx.coroutines.flow.Flow

interface SiteRepository {
    fun getUserSites(): Flow<List<Site>>
    suspend fun createSite(name: String, address: String?): Result<Site>
    suspend fun deleteSite(siteId: String): Result<Unit>
    suspend fun getSiteMembers(siteId: String): Result<List<SiteMember>>
    suspend fun removeMember(memberId: String): Result<Unit>
    suspend fun syncSites()
}
