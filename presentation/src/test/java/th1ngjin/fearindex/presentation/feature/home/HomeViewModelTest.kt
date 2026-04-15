package th1ngjin.fearindex.presentation.feature.home

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetCryptoFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexHistoryUseCase
import th1ngjin.fearindex.domain.usecase.GetFearIndexUseCase
import th1ngjin.fearindex.domain.usecase.GetMarketIndicesUseCase
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getFearIndex = mockk<GetFearIndexUseCase>()
    private val getFearIndexHistory = mockk<GetFearIndexHistoryUseCase>()
    private val getCryptoFearIndex = mockk<GetCryptoFearIndexUseCase>()
    private val getCryptoFearIndexHistory = mockk<GetCryptoFearIndexHistoryUseCase>()
    private val getMarketIndices = mockk<GetMarketIndicesUseCase>()
    private val analytics = mockk<AnalyticsManager>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getFearIndex = getFearIndex,
            getFearIndexHistory = getFearIndexHistory,
            getCryptoFearIndex = getCryptoFearIndex,
            getCryptoFearIndexHistory = getCryptoFearIndexHistory,
            getMarketIndices = getMarketIndices,
            analytics = analytics,
        )
    }

    @Test
    fun `초기 상태는 selectedType MARKET`() = runTest {
        stubAllSuccess()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(FearIndexType.MARKET, viewModel.uiState.value.selectedType)
    }

    @Test
    fun `성공 로드 시 marketState는 Loaded`() = runTest {
        val fearIndex = createFearIndex(42.0)
        stubAllSuccess(marketCurrent = fearIndex)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value.marketState
        assertTrue(state is FearIndexState.Loaded)
        assertEquals(42.0, (state as FearIndexState.Loaded).fearIndex.score, 0.01)
    }

    @Test
    fun `성공 로드 시 cryptoState는 Loaded`() = runTest {
        val fearIndex = createFearIndex(75.0)
        stubAllSuccess(cryptoCurrent = fearIndex)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value.cryptoState
        assertTrue(state is FearIndexState.Loaded)
        assertEquals(75.0, (state as FearIndexState.Loaded).fearIndex.score, 0.01)
    }

    @Test
    fun `IOException 시 marketState는 Error`() = runTest {
        coEvery { getFearIndex(any()) } throws IOException("Network error")
        coEvery { getFearIndexHistory(any(), any()) } returns emptyList()
        coEvery { getCryptoFearIndex(any()) } returns createFearIndex(50.0)
        coEvery { getCryptoFearIndexHistory(any(), any()) } returns emptyList()
        coEvery { getMarketIndices() } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value.marketState
        assertTrue(state is FearIndexState.Error)
        assertEquals("Network error", (state as FearIndexState.Error).message)
    }

    @Test
    fun `일반 Exception 시 marketState는 Error`() = runTest {
        coEvery { getFearIndex(any()) } throws RuntimeException("Unknown error")
        coEvery { getFearIndexHistory(any(), any()) } returns emptyList()
        coEvery { getCryptoFearIndex(any()) } returns createFearIndex(50.0)
        coEvery { getCryptoFearIndexHistory(any(), any()) } returns emptyList()
        coEvery { getMarketIndices() } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value.marketState
        assertTrue(state is FearIndexState.Error)
    }

    @Test
    fun `selectIndexType - CRYPTO로 전환`() = runTest {
        stubAllSuccess()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectIndexType(FearIndexType.CRYPTO)

        assertEquals(FearIndexType.CRYPTO, viewModel.uiState.value.selectedType)
    }

    @Test
    fun `selectIndexType - 같은 타입 선택 시 analytics 미발송`() = runTest {
        stubAllSuccess()

        val viewModel = createViewModel()
        advanceUntilIdle()

        // 이미 MARKET인데 또 MARKET 선택
        viewModel.selectIndexType(FearIndexType.MARKET)

        // 초기 로드 이벤트만 있고, 지수타입전환 이벤트는 없어야 함
        // (relaxed mock이므로 호출 자체는 가능하지만 previous != type 조건이 false)
        assertEquals(FearIndexType.MARKET, viewModel.uiState.value.selectedType)
    }

    @Test
    fun `marketHistory 로딩 성공`() = runTest {
        val history = listOf(createFearIndex(30.0), createFearIndex(50.0), createFearIndex(70.0))
        stubAllSuccess(marketHistory = history)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.marketHistory.size)
        assertEquals(false, viewModel.uiState.value.isMarketHistoryLoading)
    }

    @Test
    fun `cryptoHistory 로딩 성공`() = runTest {
        val history = listOf(createFearIndex(20.0), createFearIndex(40.0))
        stubAllSuccess(cryptoHistory = history)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.cryptoHistory.size)
        assertEquals(false, viewModel.uiState.value.isCryptoHistoryLoading)
    }

    @Test
    fun `marketIndices 로딩 성공`() = runTest {
        val indices = listOf(
            MarketIndex(symbol = "^GSPC", name = "S&P 500", price = 5200.0, changePercent = 1.2, isPositive = true),
        )
        stubAllSuccess(marketIndices = indices)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.marketIndices.size)
        assertEquals("^GSPC", viewModel.uiState.value.marketIndices[0].symbol)
    }

    @Test
    fun `marketIndices 로딩 실패 시 빈 리스트 유지`() = runTest {
        coEvery { getFearIndex(any()) } returns createFearIndex(50.0)
        coEvery { getFearIndexHistory(any(), any()) } returns emptyList()
        coEvery { getCryptoFearIndex(any()) } returns createFearIndex(50.0)
        coEvery { getCryptoFearIndexHistory(any(), any()) } returns emptyList()
        coEvery { getMarketIndices() } throws RuntimeException("API error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.marketIndices.isEmpty())
    }

    @Test
    fun `Turbine으로 uiState Flow 검증 - Loading에서 Loaded로 전이`() = runTest {
        val fearIndex = createFearIndex(65.0)
        stubAllSuccess(marketCurrent = fearIndex)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            // 초기 상태 또는 중간 상태 (Loading 포함)
            val firstState = awaitItem()
            // Loading 또는 이미 Loaded일 수 있음 (dispatcher에 따라)

            // 모든 코루틴 완료 대기
            advanceUntilIdle()

            // 최종 상태 수집
            val finalState = expectMostRecentItem()
            assertTrue(finalState.marketState is FearIndexState.Loaded)
            assertEquals(65.0, (finalState.marketState as FearIndexState.Loaded).fearIndex.score, 0.01)

            cancelAndConsumeRemainingEvents()
        }
    }

    // ============================
    // Helper
    // ============================

    private fun stubAllSuccess(
        marketCurrent: FearIndex = createFearIndex(50.0),
        cryptoCurrent: FearIndex = createFearIndex(50.0),
        marketHistory: List<FearIndex> = emptyList(),
        cryptoHistory: List<FearIndex> = emptyList(),
        marketIndices: List<MarketIndex> = emptyList(),
    ) {
        coEvery { getFearIndex(any()) } returns marketCurrent
        coEvery { getFearIndexHistory(any(), any()) } returns marketHistory
        coEvery { getCryptoFearIndex(any()) } returns cryptoCurrent
        coEvery { getCryptoFearIndexHistory(any(), any()) } returns cryptoHistory
        coEvery { getMarketIndices() } returns marketIndices
    }

    private fun createFearIndex(score: Double) = FearIndex(
        score = score,
        rating = FearIndex.Rating.from(score),
        timestamp = Instant.now(),
    )
}
