package com.vigipro.feature.accesscontrol

import com.vigipro.core.data.repository.AuthRepository
import com.vigipro.core.data.repository.AuthSessionState
import com.vigipro.core.data.repository.InvitationRepository
import com.vigipro.core.data.repository.SiteRepository
import com.vigipro.core.model.Invitation
import com.vigipro.core.model.Site
import com.vigipro.core.model.SiteMember
import com.vigipro.core.model.UserRole
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

// ── Fakes (MockK coEvery deadlocks with orbit-test's Turbine) ──────────

private class FakeSiteRepository : SiteRepository {
    val sitesFlow = MutableSharedFlow<List<Site>>()
    var membersResult: Result<List<SiteMember>> = Result.success(emptyList())
    var removeMemberResult: Result<Unit> = Result.success(Unit)
    var createSiteResult: Result<Site> = Result.success(
        Site("new", "Novo", null, "owner"),
    )
    var deleteSiteResult: Result<Unit> = Result.success(Unit)
    var syncSitesCalled = false
    val removedMemberIds = mutableListOf<String>()

    override fun getUserSites(): Flow<List<Site>> = sitesFlow
    override suspend fun createSite(name: String, address: String?) = createSiteResult
    override suspend fun deleteSite(siteId: String) = deleteSiteResult
    override suspend fun getSiteMembers(siteId: String) = membersResult
    override suspend fun removeMember(memberId: String): Result<Unit> {
        removedMemberIds += memberId
        return removeMemberResult
    }
    override suspend fun syncSites() { syncSitesCalled = true }
}

private class FakeInvitationRepository : InvitationRepository {
    var invitationsResult: Result<List<Invitation>> = Result.success(emptyList())
    var createResult: Result<Invitation> = Result.success(
        Invitation(id = "x", siteId = "x", inviteCode = "X", role = UserRole.VIEWER, expiresAt = ""),
    )
    var deleteResult: Result<Unit> = Result.success(Unit)
    var redeemResult: Result<String> = Result.success("site-1")
    val deletedIds = mutableListOf<String>()
    val redeemedCodes = mutableListOf<String>()

    override suspend fun getInvitationsForSite(siteId: String) = invitationsResult
    override suspend fun createInvitation(
        siteId: String, role: UserRole, cameraIds: List<String>?,
        timeStart: String?, timeEnd: String?, daysOfWeek: List<Int>?,
        maxUses: Int, expiresInHours: Int,
    ) = createResult
    override suspend fun deleteInvitation(invitationId: String): Result<Unit> {
        deletedIds += invitationId
        return deleteResult
    }
    override suspend fun redeemInvitation(code: String): Result<String> {
        redeemedCodes += code
        return redeemResult
    }
}

private class FakeAuthRepository : AuthRepository {
    override val sessionState: Flow<AuthSessionState> =
        MutableStateFlow(AuthSessionState.Authenticated("user-1", "u@e.com"))
    override val currentUserId: String? = "user-1"
    override val currentUserEmail: String? = "u@e.com"
    override suspend fun signIn(email: String, password: String) = Result.success(Unit)
    override suspend fun signUp(email: String, password: String) = Result.success(Unit)
    override suspend fun signOut() {}
}

// ── Test Suite ──────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class AccessControlViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private val testSite = Site("site-1", "Meu Local", null, "owner-1")
    private val testMember = SiteMember(
        id = "m1", siteId = "site-1", userId = "user-1", role = UserRole.VIEWER,
    )
    private val testInvitation = Invitation(
        id = "inv-1", siteId = "site-1", inviteCode = "ABC123",
        role = UserRole.VIEWER, maxUses = 1, usesCount = 0,
        expiresAt = "2026-12-31T23:59:59Z",
    )

    private val loadedState = AccessControlState(
        sites = listOf(testSite),
        selectedSiteId = "site-1",
        members = listOf(testMember),
        invitations = listOf(testInvitation),
        isLoading = false,
    )

    private lateinit var siteRepo: FakeSiteRepository
    private lateinit var invRepo: FakeInvitationRepository
    private lateinit var authRepo: FakeAuthRepository

    @Before
    fun createFakes() {
        siteRepo = FakeSiteRepository()
        invRepo = FakeInvitationRepository()
        authRepo = FakeAuthRepository()
    }

    private fun createViewModel() = AccessControlViewModel(siteRepo, invRepo, authRepo)

    // --- Initial state ---

    @Test
    fun `initial state defaults are correct`() {
        val state = AccessControlState()
        assertTrue(state.isLoading)
        assertEquals(0, state.selectedTab)
        assertNull(state.selectedSiteId)
        assertTrue(state.members.isEmpty())
        assertTrue(state.invitations.isEmpty())
        assertFalse(state.showCreateInvite)
        assertNull(state.createdInviteCode)
        assertEquals("", state.redeemCode)
        assertFalse(state.isRedeeming)
        assertEquals(UserRole.VIEWER, state.inviteRole)
        assertEquals(24, state.inviteExpiresHours)
        assertEquals(1, state.inviteMaxUses)
    }

    // --- Tab navigation ---

    @Test
    fun `onTabSelected changes selected tab`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onTabSelected(1)
            expectState { copy(selectedTab = 1) }
            containerHost.onTabSelected(2)
            expectState { copy(selectedTab = 2) }
            containerHost.onTabSelected(0)
            expectState { copy(selectedTab = 0) }
        }
    }

    // --- Invite config changes ---

    @Test
    fun `onInviteRoleChange updates role`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onInviteRoleChange(UserRole.TIME_RESTRICTED)
            expectState { copy(inviteRole = UserRole.TIME_RESTRICTED) }
        }
    }

    @Test
    fun `onInviteExpiresChange updates expiry hours`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onInviteExpiresChange(168)
            expectState { copy(inviteExpiresHours = 168) }
        }
    }

    @Test
    fun `onInviteMaxUsesChange updates max uses`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onInviteMaxUsesChange(10)
            expectState { copy(inviteMaxUses = 10) }
        }
    }

    @Test
    fun `onTimeStartChange and onTimeEndChange update time window`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onTimeStartChange("08:00")
            expectState { copy(timeStart = "08:00") }
            containerHost.onTimeEndChange("18:00")
            expectState { copy(timeEnd = "18:00") }
        }
    }

    @Test
    fun `onSelectedDaysChange updates days list`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onSelectedDaysChange(listOf(6, 7))
            expectState { copy(selectedDays = listOf(6, 7)) }
        }
    }

    // --- Create invite sheet ---

    @Test
    fun `onShowCreateInvite opens sheet and clears previous code`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState.copy(createdInviteCode = "OLD")) {
            expectInitialState()
            containerHost.onShowCreateInvite()
            expectState { copy(showCreateInvite = true, createdInviteCode = null) }
        }
    }

    @Test
    fun `onDismissCreateInvite closes sheet`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState.copy(showCreateInvite = true)) {
            expectInitialState()
            containerHost.onDismissCreateInvite()
            expectState { copy(showCreateInvite = false) }
        }
    }

    @Test
    fun `onDismissInviteResult clears invite code`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState.copy(createdInviteCode = "CODE")) {
            expectInitialState()
            containerHost.onDismissInviteResult()
            expectState { copy(createdInviteCode = null) }
        }
    }

    // --- Create invite ---

    @Test
    fun `onCreateInvite success shows snackbar and adds to list`() = runTest {
        val newInvite = testInvitation.copy(id = "inv-new", inviteCode = "NEW123")
        invRepo.createResult = Result.success(newInvite)

        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onCreateInvite()
            expectState {
                copy(
                    showCreateInvite = false,
                    createdInviteCode = "NEW123",
                    invitations = invitations + newInvite,
                )
            }
            expectSideEffect(AccessControlSideEffect.ShowSnackbar("Convite criado"))
        }
    }

    @Test
    fun `onCreateInvite failure shows error snackbar`() = runTest {
        invRepo.createResult = Result.failure(RuntimeException("Falha de rede"))

        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onCreateInvite()
            expectSideEffect(AccessControlSideEffect.ShowSnackbar("Falha de rede"))
        }
    }

    @Test
    fun `onCreateInvite with no selected site does nothing`() = runTest {
        val noSiteState = loadedState.copy(selectedSiteId = null)
        val vm = createViewModel()
        vm.test(this, initialState = noSiteState) {
            expectInitialState()
            containerHost.onCreateInvite()
        }
    }

    // --- Delete invitation ---

    @Test
    fun `onDeleteInvitation success removes from list and shows snackbar`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onDeleteInvitation("inv-1")
            expectState { copy(invitations = invitations.filter { it.id != "inv-1" }) }
            expectSideEffect(AccessControlSideEffect.ShowSnackbar("Convite removido"))
        }
        assertEquals(listOf("inv-1"), invRepo.deletedIds)
    }

    @Test
    fun `onDeleteInvitation failure does not change state`() = runTest {
        invRepo.deleteResult = Result.failure(RuntimeException("Erro"))

        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onDeleteInvitation("inv-1")
        }
    }

    // --- Remove member ---

    @Test
    fun `onRemoveMember success removes from list and shows snackbar`() = runTest {
        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onRemoveMember("m1")
            expectState { copy(members = members.filter { it.id != "m1" }) }
            expectSideEffect(AccessControlSideEffect.ShowSnackbar("Membro removido"))
        }
        assertEquals(listOf("m1"), siteRepo.removedMemberIds)
    }

    @Test
    fun `onRemoveMember failure does not change state`() = runTest {
        siteRepo.removeMemberResult = Result.failure(RuntimeException("Erro"))

        val vm = createViewModel()
        vm.test(this, initialState = loadedState) {
            expectInitialState()
            containerHost.onRemoveMember("m1")
        }
    }

    // --- Redeem invite ---

    @Test
    fun `onRedeemCodeChange updates code`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onRedeemCodeChange("ABC123")
            expectState { copy(redeemCode = "ABC123") }
        }
    }

    @Test
    fun `onRedeemInvite with blank code does nothing`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onRedeemInvite()
        }
    }

    @Test
    fun `onRedeemInvite success clears code and shows snackbar`() = runTest {
        invRepo.redeemResult = Result.success("site-1")

        val readyState = loadedState.copy(redeemCode = "ABC123")
        val vm = createViewModel()
        vm.test(this, initialState = readyState) {
            expectInitialState()
            containerHost.onRedeemInvite()
            expectState { copy(isRedeeming = true) }
            expectState { copy(isRedeeming = false, redeemCode = "") }
            expectSideEffect(AccessControlSideEffect.ShowSnackbar("Convite resgatado com sucesso!"))
            // loadSiteData(siteId) is called after redeem → produces 2 more states
            expectState { copy(isLoading = true) }
            expectState { copy(members = emptyList(), invitations = emptyList(), isLoading = false) }
        }
        assertTrue(siteRepo.syncSitesCalled)
        assertEquals(listOf("ABC123"), invRepo.redeemedCodes)
    }

    @Test
    fun `onRedeemInvite failure shows error and stops loading`() = runTest {
        invRepo.redeemResult = Result.failure(RuntimeException("Convite nao encontrado"))

        val readyState = loadedState.copy(redeemCode = "INVALID")
        val vm = createViewModel()
        vm.test(this, initialState = readyState) {
            expectInitialState()
            containerHost.onRedeemInvite()
            expectState { copy(isRedeeming = true) }
            expectState { copy(isRedeeming = false) }
            expectSideEffect(
                AccessControlSideEffect.ShowSnackbar("Convite nao encontrado"),
            )
        }
    }

    // --- Navigation ---

    @Test
    fun `onBack emits NavigateBack side effect`() = runTest {
        val vm = createViewModel()
        vm.test(this) {
            expectInitialState()
            containerHost.onBack()
            expectSideEffect(AccessControlSideEffect.NavigateBack)
        }
    }
}
