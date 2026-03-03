package com.vigipro.feature.sites

import com.vigipro.core.data.repository.AuthRepository
import com.vigipro.core.data.repository.AuthSessionState
import com.vigipro.core.data.repository.SiteRepository
import com.vigipro.core.model.Site
import com.vigipro.core.model.SiteMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.test

// ── Fakes ──────────────────────────────────────────────────────────

private class FakeSiteRepository : SiteRepository {
    val sitesFlow = MutableSharedFlow<List<Site>>()
    var createResult: Result<Site> = Result.success(
        Site("new-id", "Novo Local", null, "user-1"),
    )
    var deleteResult: Result<Unit> = Result.success(Unit)
    var membersResult: Result<List<SiteMember>> = Result.success(emptyList())
    var removeMemberResult: Result<Unit> = Result.success(Unit)
    var syncCalled = false
    val deletedIds = mutableListOf<String>()

    override fun getUserSites(): Flow<List<Site>> = sitesFlow
    override suspend fun createSite(name: String, address: String?): Result<Site> = createResult
    override suspend fun deleteSite(siteId: String): Result<Unit> {
        deletedIds += siteId
        return deleteResult
    }
    override suspend fun getSiteMembers(siteId: String) = membersResult
    override suspend fun removeMember(memberId: String) = removeMemberResult
    override suspend fun syncSites() { syncCalled = true }
}

private class FakeAuthRepository : AuthRepository {
    override val sessionState: Flow<AuthSessionState> =
        MutableStateFlow(AuthSessionState.Authenticated("user-1", "u@e.com"))
    override val currentUserId: String? = "user-1"
    override val currentUserEmail: String? = "u@e.com"
    override suspend fun signIn(email: String, password: String) = Result.success(Unit)
    override suspend fun signUp(email: String, password: String) = Result.success(Unit)
    override suspend fun signInWithGoogle(idToken: String) = Result.success(Unit)
    override suspend fun sendPasswordResetEmail(email: String) = Result.success(Unit)
    override suspend fun signOut() {}
}

// ── Tests ──────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class SitesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val testSite = Site("site-1", "Meu Local", "Rua A", "user-1")

    private val loadedState = SitesState(
        sites = listOf(testSite),
        currentUserId = "user-1",
        isLoading = false,
    )

    private lateinit var siteRepo: FakeSiteRepository
    private lateinit var authRepo: FakeAuthRepository

    @Before
    fun createFakes() {
        siteRepo = FakeSiteRepository()
        authRepo = FakeAuthRepository()
    }

    private fun createViewModel() = SitesViewModel(siteRepo, authRepo)

    // --- Initial state ---

    @Test
    fun `initial state defaults`() {
        val state = SitesState()
        assertTrue(state.isLoading)
        assertTrue(state.sites.isEmpty())
        assertNull(state.currentUserId)
        assertFalse(state.showCreateSheet)
        assertNull(state.editingSite)
        assertEquals("", state.siteName)
        assertEquals("", state.siteAddress)
        assertFalse(state.isSaving)
        assertNull(state.siteToDelete)
    }

    // --- Create sheet ---

    @Test
    fun `onShowCreateSheet opens sheet with empty fields`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onShowCreateSheet()
            expectState {
                copy(showCreateSheet = true, editingSite = null, siteName = "", siteAddress = "")
            }
        }
    }

    @Test
    fun `onShowEditSheet opens sheet with site data`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onShowEditSheet(testSite)
            expectState {
                copy(
                    showCreateSheet = true,
                    editingSite = testSite,
                    siteName = "Meu Local",
                    siteAddress = "Rua A",
                )
            }
        }
    }

    @Test
    fun `onDismissSheet closes sheet`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState.copy(showCreateSheet = true)) {
            expectInitialState()
            containerHost.onDismissSheet()
            expectState { copy(showCreateSheet = false, editingSite = null) }
        }
    }

    // --- Name/Address changes ---

    @Test
    fun `onSiteNameChange updates name`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onSiteNameChange("Casa")
            expectState { copy(siteName = "Casa") }
        }
    }

    @Test
    fun `onSiteAddressChange updates address`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onSiteAddressChange("Rua B, 456")
            expectState { copy(siteAddress = "Rua B, 456") }
        }
    }

    // --- Save site ---

    @Test
    fun `onSaveSite with blank name shows error`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState.copy(siteName = "  ")) {
            expectInitialState()
            containerHost.onSaveSite()
            expectSideEffect(SitesSideEffect.ShowSnackbar("Informe o nome do local"))
        }
    }

    @Test
    fun `onSaveSite success creates site and closes sheet`() = runTest {
        val readyState = loadedState.copy(siteName = "Escritorio", siteAddress = "Centro")
        val vm = createViewModel()
        vm.test(this, initialState = readyState) {
            expectInitialState()
            containerHost.onSaveSite()
            expectState { copy(isSaving = true) }
            expectState { copy(isSaving = false, showCreateSheet = false) }
            expectSideEffect(SitesSideEffect.ShowSnackbar("Local criado"))
        }
    }

    @Test
    fun `onSaveSite failure shows error snackbar`() = runTest {
        siteRepo.createResult = Result.failure(RuntimeException("Network error"))
        val readyState = loadedState.copy(siteName = "Novo")
        val vm = createViewModel()
        vm.test(this, initialState = readyState) {
            expectInitialState()
            containerHost.onSaveSite()
            expectState { copy(isSaving = true) }
            expectState { copy(isSaving = false) }
            expectSideEffect(SitesSideEffect.ShowSnackbar("Erro ao criar local. Tente novamente"))
        }
    }

    // --- Delete ---

    @Test
    fun `onDeleteClick sets siteToDelete`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onDeleteClick(testSite)
            expectState { copy(siteToDelete = testSite) }
        }
    }

    @Test
    fun `onConfirmDelete success removes site and shows snackbar`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState.copy(siteToDelete = testSite)) {
            expectInitialState()
            containerHost.onConfirmDelete()
            expectState { copy(siteToDelete = null) }
            expectSideEffect(SitesSideEffect.ShowSnackbar("\"Meu Local\" removido"))
        }
        assertEquals(listOf("site-1"), siteRepo.deletedIds)
    }

    @Test
    fun `onConfirmDelete failure shows error snackbar`() = runTest {
        siteRepo.deleteResult = Result.failure(RuntimeException("Erro"))
        val vm = createViewModel()
        vm.test(this, initialState = loadedState.copy(siteToDelete = testSite)) {
            expectInitialState()
            containerHost.onConfirmDelete()
            expectState { copy(siteToDelete = null) }
            expectSideEffect(SitesSideEffect.ShowSnackbar("Erro ao remover local"))
        }
    }

    @Test
    fun `onDismissDelete clears siteToDelete`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState.copy(siteToDelete = testSite)) {
            expectInitialState()
            containerHost.onDismissDelete()
            expectState { copy(siteToDelete = null) }
        }
    }

    // --- Navigation ---

    @Test
    fun `onBack emits NavigateBack`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onBack()
            expectSideEffect(SitesSideEffect.NavigateBack)
        }
    }
}
