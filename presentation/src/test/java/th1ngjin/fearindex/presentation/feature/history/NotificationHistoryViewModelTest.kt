package th1ngjin.fearindex.presentation.feature.history

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.analytics.PremiumPurchaseSource
import th1ngjin.fearindex.core.purchases.PurchaseEvent
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.domain.entity.NotificationKind
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.domain.usecase.NotificationHistoryUseCase
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now: Instant = Instant.parse("2027-01-15T03:00:00Z")

    private val premiumFlow = MutableStateFlow(false)
    private val priceFlow = MutableStateFlow<String?>("₩7,500")
    private val eventsFlow = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    private val purchaseManager = mockk<PurchaseManager>().also { pm ->
        every { pm.isPremium } returns premiumFlow
        every { pm.priceText } returns priceFlow
        every { pm.purchaseEvents } returns eventsFlow
    }
    private val analytics = mockk<AnalyticsManager>(relaxed = true)

    private fun record(id: String, daysAgo: Long) = NotificationRecord(
        id = id, kind = NotificationKind.KOSPI, title = "t$id", body = "b", score = 20,
        receivedAt = now.minus(Duration.ofDays(daysAgo)),
    )

    private lateinit var repository: FakeNotificationHistoryRepository

    private fun viewModel(initial: List<NotificationRecord>): NotificationHistoryViewModel {
        repository = FakeNotificationHistoryRepository(initial)
        return NotificationHistoryViewModel(
            useCase = NotificationHistoryUseCase(repository) { now },
            purchaseManager = purchaseManager,
            analytics = analytics,
        )
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `init - 무료 유저는 30일 안 레코드만 로드, isLoaded=true`() = runTest {
        val vm = viewModel(listOf(record("a", 1), record("b", 10), record("old", 45)))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.isLoaded)
        assertEquals(listOf("a", "b"), state.records.map { it.id })
        assertFalse(state.isPremium)
        assertEquals("₩7,500", state.priceText)
    }

    @Test
    fun `프리미엄 전환 시 재로드 - 무료에서 숨겨졌던 30일 이전 내역이 복원된다`() = runTest {
        val vm = viewModel(listOf(record("a", 1), record("old", 45)))
        advanceUntilIdle()
        // 무료 상태: 30일 초과분은 화면에서만 숨겨진다(저장소는 무손실)
        assertEquals(listOf("a"), vm.uiState.value.records.map { it.id })

        premiumFlow.value = true
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isPremium)
        // 구매 즉시 과거 내역이 그대로 돌아온다 — 잠금 카피가 성립하는 조건
        assertEquals(listOf("a", "old"), vm.uiState.value.records.map { it.id })
    }

    @Test
    fun `프리미엄 유저는 30일 지난 레코드도 표시`() = runTest {
        premiumFlow.value = true
        val vm = viewModel(listOf(record("a", 1), record("old", 45)))
        advanceUntilIdle()
        assertEquals(listOf("a", "old"), vm.uiState.value.records.map { it.id })
    }

    @Test
    fun `onShown - markSeen 1회 + 알림내역조회(개수) 1회`() = runTest {
        val vm = viewModel(listOf(record("a", 1), record("b", 2)))
        advanceUntilIdle()
        vm.onShown()
        vm.onShown()
        advanceUntilIdle()
        assertEquals(now, repository.lastSeen)
        verify(exactly = 1) { analytics.log(AnalyticsEvent.알림내역조회(개수 = 2)) }
    }

    @Test
    fun `표시 중 새 기록 도착 - 리스트 갱신 + 즉시 확인 처리`() = runTest {
        val vm = viewModel(listOf(record("a", 1)))
        advanceUntilIdle()
        vm.onShown()
        advanceUntilIdle()
        val seenBefore = repository.setLastSeenCount
        repository.append(record("new", 0).copy(receivedAt = now.plusSeconds(1)))
        advanceUntilIdle()
        assertEquals(listOf("new", "a"), vm.uiState.value.records.map { it.id })
        assertTrue(repository.setLastSeenCount > seenBefore)
    }

    @Test
    fun `purchase - 프리미엄잠금탭 로그 + isBusy, 실패 이벤트로 다이얼로그`() = runTest {
        every { purchaseManager.purchaseRemoveAds(any(), PremiumPurchaseSource.NOTIFICATION_HISTORY) } returns Unit
        val vm = viewModel(emptyList())
        advanceUntilIdle()
        vm.purchase(mockk(relaxed = true))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isBusy)
        verify { analytics.log(AnalyticsEvent.프리미엄잠금탭(feature = "notification_history_unlimited")) }
        verify { purchaseManager.purchaseRemoveAds(any(), PremiumPurchaseSource.NOTIFICATION_HISTORY) }
        eventsFlow.tryEmit(PurchaseEvent.Failed("x"))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isBusy)
        assertEquals(NotificationHistoryDialog.PurchaseFailed, vm.uiState.value.dialog)
        vm.dismissDialog()
        assertEquals(null, vm.uiState.value.dialog)
    }

    @Test
    fun `restore - 결과에 따라 성공,실패 다이얼로그`() = runTest {
        coEvery { purchaseManager.restorePurchases(PremiumPurchaseSource.NOTIFICATION_HISTORY) } returns false
        val vm = viewModel(emptyList())
        advanceUntilIdle()
        vm.restore()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isBusy)
        assertNotNull(vm.uiState.value.dialog)
        assertEquals(NotificationHistoryDialog.RestoreFailure, vm.uiState.value.dialog)
    }
}
