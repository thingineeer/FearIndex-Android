package th1ngjin.fearindex.presentation.feature.history

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.domain.entity.NotificationKind
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.domain.usecase.NotificationHistoryUseCase
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationHistoryBadgeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now: Instant = Instant.parse("2027-01-15T03:00:00Z")
    private val premiumFlow = MutableStateFlow(false)
    private val purchaseManager = mockk<PurchaseManager>().also { pm ->
        every { pm.isPremium } returns premiumFlow
    }

    private fun record(id: String, daysAgo: Long) = NotificationRecord(
        id = id, kind = NotificationKind.MARKET, title = "t", body = "b", score = null,
        receivedAt = now.minus(Duration.ofDays(daysAgo)),
    )

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `미확인 기록 있으면 true, 확인 처리 후 updates 로 false`() = runTest {
        val repository = FakeNotificationHistoryRepository(listOf(record("a", 1)))
        val useCase = NotificationHistoryUseCase(repository) { now }
        val vm = NotificationHistoryBadgeViewModel(useCase, purchaseManager)
        advanceUntilIdle()
        assertTrue(vm.hasUnread.value)

        useCase.markSeen(now)
        advanceUntilIdle()
        assertFalse(vm.hasUnread.value)

        repository.append(record("b", 0).copy(receivedAt = now.plusSeconds(1)))
        advanceUntilIdle()
        assertTrue(vm.hasUnread.value)
    }

    @Test
    fun `보관 기간 밖 기록만 있으면 무료는 false, 프리미엄 전환 시 true`() = runTest {
        val repository = FakeNotificationHistoryRepository(listOf(record("old", 45)))
        val useCase = NotificationHistoryUseCase(repository) { now }
        val vm = NotificationHistoryBadgeViewModel(useCase, purchaseManager)
        advanceUntilIdle()
        assertFalse(vm.hasUnread.value)

        premiumFlow.value = true
        advanceUntilIdle()
        assertTrue(vm.hasUnread.value)
    }

    @Test
    fun `refresh - 명시 호출로 재계산`() = runTest {
        val repository = FakeNotificationHistoryRepository()
        val useCase = NotificationHistoryUseCase(repository) { now }
        val vm = NotificationHistoryBadgeViewModel(useCase, purchaseManager)
        advanceUntilIdle()
        assertFalse(vm.hasUnread.value)
        repository.replaceAll(listOf(record("a", 0))) // updates 미발행 경로
        vm.refresh()
        advanceUntilIdle()
        assertTrue(vm.hasUnread.value)
    }
}
