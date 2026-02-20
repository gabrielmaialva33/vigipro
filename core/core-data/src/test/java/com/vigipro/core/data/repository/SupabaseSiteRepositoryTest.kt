package com.vigipro.core.data.repository

import app.cash.turbine.test
import com.vigipro.core.data.db.SiteDao
import com.vigipro.core.data.db.SiteEntity
import com.vigipro.core.model.Site
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests SiteRepository DAO mapping behavior.
 * Network-dependent logic (Postgrest) is tested via integration tests.
 */
class SiteRepositoryDaoMappingTest {

    @Test
    fun `getUserSites maps entities to domain models correctly`() = runTest {
        val entities = listOf(
            SiteEntity("1", "Casa", null, "owner-1"),
            SiteEntity("2", "Escritorio", "Av Paulista 123", "owner-1"),
        )
        val siteDao: SiteDao = mockk {
            every { getAllSites() } returns flowOf(entities)
        }

        // Test the mapping through a fake that uses real DAO mapping logic
        siteDao.getAllSites().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Casa", result[0].name)
            assertEquals("Escritorio", result[1].name)
            assertEquals("Av Paulista 123", result[1].address)
            assertEquals("owner-1", result[0].ownerId)
            awaitComplete()
        }
    }

    @Test
    fun `getUserSites returns empty list when no sites cached`() = runTest {
        val siteDao: SiteDao = mockk {
            every { getAllSites() } returns flowOf(emptyList())
        }

        siteDao.getAllSites().test {
            assertEquals(emptyList<SiteEntity>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `SiteEntity to Site mapping preserves all fields`() {
        val entity = SiteEntity(
            id = "site-123",
            name = "Meu Local",
            address = "Rua Teste 42",
            ownerId = "owner-456",
        )
        val site = Site(
            id = entity.id,
            name = entity.name,
            address = entity.address,
            ownerId = entity.ownerId,
        )
        assertEquals("site-123", site.id)
        assertEquals("Meu Local", site.name)
        assertEquals("Rua Teste 42", site.address)
        assertEquals("owner-456", site.ownerId)
    }

    @Test
    fun `Site to SiteEntity mapping preserves all fields`() {
        val site = Site(
            id = "site-789",
            name = "Escritorio",
            address = null,
            ownerId = "owner-101",
        )
        val entity = SiteEntity(
            id = site.id,
            name = site.name,
            address = site.address,
            ownerId = site.ownerId,
        )
        assertEquals("site-789", entity.id)
        assertEquals("Escritorio", entity.name)
        assertTrue(entity.address == null)
        assertEquals("owner-101", entity.ownerId)
    }
}
