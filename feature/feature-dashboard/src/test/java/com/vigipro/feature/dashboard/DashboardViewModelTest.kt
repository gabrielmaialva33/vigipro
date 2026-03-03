package com.vigipro.feature.dashboard

import com.vigipro.core.data.monitor.CameraStatusMonitor
import com.vigipro.core.data.repository.AuthRepository
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.repository.CloudRepository
import com.vigipro.core.data.repository.SiteRepository
import com.vigipro.core.data.seed.DevSeedHelper
import com.vigipro.core.data.sync.CloudSyncManager
import com.vigipro.core.model.Site
import com.vigipro.core.ui.components.GridLayout
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

class DashboardViewModelTest {

    private val cameraRepository: CameraRepository = mockk(relaxed = true) {
        every { getAllCameras() } returns flowOf(emptyList())
    }
    private val siteRepository: SiteRepository = mockk(relaxed = true) {
        every { getUserSites() } returns flowOf(
            listOf(
                Site(id = "site-1", name = "Casa", ownerId = "owner-1"),
                Site(id = "site-2", name = "Escritorio", ownerId = "owner-1"),
            ),
        )
    }
    private val statusMonitor: CameraStatusMonitor = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true) {
        every { currentUserEmail } returns "test@vigipro.com"
    }
    private val cloudRepository: CloudRepository = mockk(relaxed = true)
    private val devSeedHelper: DevSeedHelper = mockk(relaxed = true)
    private val cloudSyncManager: CloudSyncManager = mockk(relaxed = true)

    private fun createViewModel() = DashboardViewModel(
        cameraRepository, siteRepository, statusMonitor, authRepository, cloudRepository, devSeedHelper, cloudSyncManager,
    )

    @Test
    fun `initial state has correct defaults`() {
        val state = DashboardState()
        assertTrue(state.cameras.isEmpty())
        assertTrue(state.sites.isEmpty())
        assertNull(state.selectedSiteId)
        assertEquals(GridLayout.GRID_2X2, state.gridLayout)
        assertTrue(state.isLoading)
        assertNull(state.cameraToDelete)
    }

    @Test
    fun `onCameraClick emits NavigateToPlayer`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onCameraClick("cam-1")
            expectSideEffect(DashboardSideEffect.NavigateToPlayer("cam-1"))
        }
    }

    @Test
    fun `onAddCameraClick emits NavigateToAddCamera`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onAddCameraClick()
            expectSideEffect(DashboardSideEffect.NavigateToAddCamera)
        }
    }

    @Test
    fun `onSettingsClick emits NavigateToSettings`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onSettingsClick()
            expectSideEffect(DashboardSideEffect.NavigateToSettings)
        }
    }

    @Test
    fun `onAccessControlClick emits NavigateToAccessControl`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onAccessControlClick()
            expectSideEffect(DashboardSideEffect.NavigateToAccessControl)
        }
    }

    @Test
    fun `onGridLayoutChange updates layout in state`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onGridLayoutChange(GridLayout.SINGLE)
            expectState { copy(gridLayout = GridLayout.SINGLE) }
        }
    }

    @Test
    fun `onGridLayoutChange to GRID_3X3`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onGridLayoutChange(GridLayout.GRID_3X3)
            expectState { copy(gridLayout = GridLayout.GRID_3X3) }
        }
    }

    @Test
    fun `onEditCameraClick emits NavigateToEditCamera`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onEditCameraClick("cam-2")
            expectSideEffect(DashboardSideEffect.NavigateToEditCamera("cam-2"))
        }
    }
}
