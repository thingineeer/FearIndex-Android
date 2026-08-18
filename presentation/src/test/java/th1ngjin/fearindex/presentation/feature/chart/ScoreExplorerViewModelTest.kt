package th1ngjin.fearindex.presentation.feature.chart

import android.app.Activity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.analytics.PremiumPurchaseSource
import th1ngjin.fearindex.core.purchases.PurchaseEvent
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.domain.defaults.DefaultReturnData
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.ReturnDataTable
import th1ngjin.fearindex.domain.entity.ReturnHorizon
import th1ngjin.fearindex.domain.repository.ReturnDataRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ScoreExplorerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val isAdFree = MutableStateFlow(false)
    private val priceText = MutableStateFlow<String?>(null)
    private val purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    private val purchaseManager = mockk<PurchaseManager>()
    private val analytics = mockk<AnalyticsManager>(relaxed = true)
    private val fetchCounts = mutableMapOf<FearIndexType, Int>()

    private val repository = object : ReturnDataRepository {
        override suspend fun fetch(indexType: FearIndexType): ReturnDataTable {
            fetchCounts[indexType] = (fetchCounts[indexType] ?: 0) + 1
            return when (indexType) {
                FearIndexType.MARKET -> DefaultReturnData.market
                FearIndexType.KOSPI -> DefaultReturnData.kospi
                FearIndexType.CRYPTO -> DefaultReturnData.crypto
            }
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { purchaseManager.isAdFree } returns isAdFree
        every { purchaseManager.priceText } returns priceText
        every { purchaseManager.purchaseEvents } returns purchaseEvents
        every { purchaseManager.purchaseRemoveAds(any(), any()) } just runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ScoreExplorerViewModel(repository, purchaseManager, analytics)

    // MARK: - 바인딩 / 로드

    @Test
    fun `bind loads table once and selects current score`() = runTest {
        val vm = createViewModel()
        vm.bind(FearIndexType.MARKET, 46)
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0..97, state.range)
        assertEquals(46, state.selectedScore)
        assertTrue(state.isAtCurrent)
        assertEquals(46, state.point?.score)
        assertNotNull(state.sourceRange)
        assertEquals(1, fetchCounts[FearIndexType.MARKET])

        vm.bind(FearIndexType.MARKET, 47)
        advanceUntilIdle()
        assertEquals(47, vm.uiState.value.selectedScore)
        assertEquals(1, fetchCounts[FearIndexType.MARKET]) // 캐시 — 재조회 없음
    }

    @Test
    fun `switching asset shows that asset current score and keeps moved selection`() = runTest {
        val vm = createViewModel()
        vm.bind(FearIndexType.MARKET, 46)
        advanceUntilIdle()
        vm.move(20)

        vm.bind(FearIndexType.CRYPTO, 35)
        advanceUntilIdle()
        assertEquals(FearIndexType.CRYPTO, vm.uiState.value.indexType)
        assertEquals(5..95, vm.uiState.value.range)
        assertEquals(35, vm.uiState.value.selectedScore)
        assertTrue(vm.uiState.value.isAtCurrent)

        vm.bind(FearIndexType.MARKET, 46)
        advanceUntilIdle()
        assertEquals(20, vm.uiState.value.selectedScore)
        assertFalse(vm.uiState.value.isAtCurrent)
    }

    // MARK: - 이동 / 리셋 / analytics

    @Test
    fun `move clamps and exposes exact bucket, reset returns to current`() = runTest {
        val vm = createViewModel()
        vm.bind(FearIndexType.MARKET, 46)
        advanceUntilIdle()

        vm.move(999)
        assertEquals(97, vm.uiState.value.selectedScore)
        assertEquals(97, vm.uiState.value.point?.score)

        vm.move(96) // n==0 버킷 → 기록 없음
        assertEquals(96, vm.uiState.value.selectedScore)
        assertNull(vm.uiState.value.point)

        vm.reset()
        assertEquals(46, vm.uiState.value.selectedScore)
        assertTrue(vm.uiState.value.isAtCurrent)
    }

    @Test
    fun `moveEnded logs one analytics event with asset, score and horizon`() = runTest {
        val vm = createViewModel()
        vm.bind(FearIndexType.KOSPI, 32)
        advanceUntilIdle()
        vm.move(40)
        vm.moveEnded(ReturnHorizon.THREE_MONTH)

        verify(exactly = 1) {
            analytics.log(AnalyticsEvent.점수탐색기조작(indexType = "kospi", score = 40, period = "threeMonth"))
        }
    }

    // MARK: - 프리미엄 / 구매 / 복원

    @Test
    fun `isPremium mirrors purchase manager entitlement`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isPremium)

        isAdFree.value = true
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isPremium)
    }

    @Test
    fun `purchase logs lock tap, starts billing with explorer source and clears busy on failure`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        val activity = mockk<Activity>(relaxed = true)

        vm.purchase(activity)
        assertTrue(vm.uiState.value.isBusy)
        verify(exactly = 1) { analytics.log(AnalyticsEvent.프리미엄잠금탭(feature = "score_explorer")) }
        verify(exactly = 1) { purchaseManager.purchaseRemoveAds(activity, PremiumPurchaseSource.SCORE_EXPLORER) }

        purchaseEvents.emit(PurchaseEvent.Failed("boom"))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isBusy)
        assertEquals(ScoreExplorerDialog.PurchaseFailed, vm.uiState.value.dialog)

        vm.dismissDialog()
        assertNull(vm.uiState.value.dialog)
    }

    @Test
    fun `purchase busy clears on cancel and on entitlement granted`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.purchase(mockk(relaxed = true))
        purchaseEvents.emit(PurchaseEvent.Cancelled)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isBusy)
        assertNull(vm.uiState.value.dialog)

        vm.purchase(mockk(relaxed = true))
        assertTrue(vm.uiState.value.isBusy)
        isAdFree.value = true
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isBusy)
        assertTrue(vm.uiState.value.isPremium)
    }

    @Test
    fun `restore shows failure dialog only when nothing restored`() = runTest {
        coEvery { purchaseManager.restorePurchases(PremiumPurchaseSource.SCORE_EXPLORER) } returns false
        val vm = createViewModel()
        advanceUntilIdle()

        vm.restore()
        assertTrue(vm.uiState.value.isBusy)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isBusy)
        assertEquals(ScoreExplorerDialog.RestoreFailure, vm.uiState.value.dialog)

        coEvery { purchaseManager.restorePurchases(PremiumPurchaseSource.SCORE_EXPLORER) } returns true
        vm.dismissDialog()
        vm.restore()
        advanceUntilIdle()
        assertNull(vm.uiState.value.dialog)
        assertFalse(vm.uiState.value.isBusy)
    }
}
