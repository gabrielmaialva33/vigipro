package com.vigipro.core.data.repository

import com.vigipro.core.data.db.SiteDao
import com.vigipro.core.data.db.SiteEntity
import com.vigipro.core.model.Site
import com.vigipro.core.model.SiteMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class CreateSiteRequest(
    val name: String,
    val address: String? = null,
    @SerialName("owner_id") val ownerId: String,
)

@Singleton
class SupabaseSiteRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val siteDao: SiteDao,
) : SiteRepository {

    override fun getUserSites(): Flow<List<Site>> =
        siteDao.getAllSites().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createSite(name: String, address: String?): Result<Site> =
        runCatching {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: error("Usuario nao autenticado")

            val site = supabase.from("sites")
                .insert(CreateSiteRequest(name = name, address = address, ownerId = userId)) {
                    select()
                }
                .decodeSingle<Site>()

            // Cache locally
            siteDao.insert(site.toEntity())

            // Auto-create owner membership
            supabase.from("site_members")
                .insert(
                    mapOf(
                        "site_id" to site.id,
                        "user_id" to userId,
                        "role" to "owner",
                    ),
                )

            site
        }

    override suspend fun deleteSite(siteId: String): Result<Unit> =
        runCatching {
            supabase.from("sites").delete {
                filter { eq("id", siteId) }
            }
            siteDao.deleteById(siteId)
        }

    override suspend fun getSiteMembers(siteId: String): Result<List<SiteMember>> =
        runCatching {
            supabase.from("site_members")
                .select {
                    filter { eq("site_id", siteId) }
                }
                .decodeList<SiteMember>()
        }

    override suspend fun removeMember(memberId: String): Result<Unit> =
        runCatching {
            supabase.from("site_members").delete {
                filter { eq("id", memberId) }
            }
        }

    override suspend fun syncSites() {
        try {
            val sites = supabase.from("sites")
                .select()
                .decodeList<Site>()

            siteDao.deleteAll()
            siteDao.insertAll(sites.map { it.toEntity() })
        } catch (_: Exception) {
            // Offline — use cached data
        }
    }

    private fun SiteEntity.toDomain() = Site(
        id = id,
        name = name,
        address = address,
        ownerId = ownerId,
    )

    private fun Site.toEntity() = SiteEntity(
        id = id,
        name = name,
        address = address,
        ownerId = ownerId,
    )
}
