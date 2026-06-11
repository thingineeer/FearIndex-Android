package th1ngjin.fearindex.presentation.feature.vote

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.entity.VoteResult
import th1ngjin.fearindex.domain.service.StuckStatusDebouncer
import th1ngjin.fearindex.domain.usecase.GetVoteResultUseCase
import th1ngjin.fearindex.domain.usecase.ObserveStuckCounterUseCase
import th1ngjin.fearindex.domain.usecase.ObserveVoteResultUseCase
import th1ngjin.fearindex.domain.usecase.SubmitVoteUseCase

/**
 * VoteViewModel 무결성 테스트.
 *
 * QA#3 해결 근거:
 * - ensureStuckCounterStarted(indexType) 호출 시 해당 index만 fetchOnce/stream 시작
 * - 초기 result StateFlow 업데이트로 진입 즉시 카운터 표시
 *
 * Note: VoteViewModel.init이 startCountdown() (1초 주기 무한 loop)을 호출하므로
 * `runTest` 블록은 countdown job이 영원히 끝나지 않아 hang 된다.
 * 이를 피하기 위해 runTest 를 쓰지 않고, Dispatchers.setMain(UnconfinedTestDispatcher) 환경에서
 * 동기적으로 StateFlow.value 만 관찰하여 검증한다.
 * 테스트 종료 시 countdown 코루틴은 JUnit 프로세스 종료로 자연 정리.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoteViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val observeStuckCounter = mockk<ObserveStuckCounterUseCase>()
    private val debouncer = mockk<StuckStatusDebouncer>(relaxed = true)
    private val submitVoteUseCase = mockk<SubmitVoteUseCase>()
    private val getVoteResultUseCase = mockk<GetVoteResultUseCase>()
    private val observeVoteResultUseCase = mockk<ObserveVoteResultUseCase>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { observeStuckCounter.loadLocalStatus(any()) } returns StuckStatus.NONE
        every { observeStuckCounter.stream(any()) } returns emptyFlow()
        every { observeVoteResultUseCase(any()) } returns emptyFlow()
        coEvery { getVoteResultUseCase(any()) } returns VoteResult.EMPTY
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): VoteViewModel = VoteViewModel(
        observeStuckCounter = observeStuckCounter,
        debouncer = debouncer,
        submitVoteUseCase = submitVoteUseCase,
        getVoteResultUseCase = getVoteResultUseCase,
        observeVoteResultUseCase = observeVoteResultUseCase,
    )

    @Test
    fun `init - 서버 비용 절감을 위해 fetchOnce를 선제 호출하지 않는다`() {
        coEvery { observeStuckCounter.fetchOnce(any()) } returns StuckCounterResult.EMPTY

        createViewModel()

        coVerify(exactly = 0) { observeStuckCounter.fetchOnce(any()) }
        coVerify(exactly = 0) { observeStuckCounter.stream(any()) }
    }

    @Test
    fun `ensureStuckCounterStarted - 요청한 index만 초기 fetch와 stream 시작`() {
        val marketInit = StuckCounterResult(
            stuckCount = 3, safeCount = 7, totalResponded = 10,
            stuckPercentage = 30.0, safePercentage = 70.0, myStatus = StuckStatus.NONE,
        )
        coEvery { observeStuckCounter.fetchOnce(FearIndexType.MARKET) } returns marketInit

        val viewModel = createViewModel()
        viewModel.ensureStuckCounterStarted(FearIndexType.MARKET)

        assertEquals(3, viewModel.marketResult.value.stuckCount)
        assertEquals(10, viewModel.marketResult.value.totalResponded)
        assertEquals(StuckCounterResult.EMPTY, viewModel.kospiResult.value)
        assertEquals(StuckCounterResult.EMPTY, viewModel.cryptoResult.value)
        coVerify(exactly = 1) { observeStuckCounter.fetchOnce(FearIndexType.MARKET) }
        coVerify(exactly = 1) { observeStuckCounter.stream(FearIndexType.MARKET) }
        coVerify(exactly = 0) { observeStuckCounter.fetchOnce(FearIndexType.KOSPI) }
        coVerify(exactly = 0) { observeStuckCounter.fetchOnce(FearIndexType.CRYPTO) }
    }

    @Test
    fun `ensureStuckCounterStarted - fetchOnce 실패해도 ViewModel 정상 유지`() {
        coEvery { observeStuckCounter.fetchOnce(any()) } throws RuntimeException("network")

        val viewModel = createViewModel()
        viewModel.ensureStuckCounterStarted(FearIndexType.MARKET)

        assertEquals(StuckCounterResult.EMPTY, viewModel.marketResult.value)
        assertEquals(StuckCounterResult.EMPTY, viewModel.kospiResult.value)
        assertEquals(StuckCounterResult.EMPTY, viewModel.cryptoResult.value)
    }

    @Test
    fun `init - iOS v1_8_0 Vote 탭은 deprecated Buy Hold Sell callable을 시작하지 않음`() {
        coEvery { observeStuckCounter.fetchOnce(any()) } returns StuckCounterResult.EMPTY

        createViewModel()

        coVerify(exactly = 0) { getVoteResultUseCase(any()) }
        coVerify(exactly = 0) { observeVoteResultUseCase(any()) }
    }

    @Test
    fun `toggleStuckStatus - 즉시 myStatus StateFlow 반영 (낙관적 업데이트)`() {
        coEvery { observeStuckCounter.fetchOnce(any()) } returns StuckCounterResult.EMPTY

        val viewModel = createViewModel()
        viewModel.toggleStuckStatus(FearIndexType.MARKET, StuckStatus.STUCK)

        assertEquals(StuckStatus.STUCK, viewModel.myMarketStatus.value)
    }

    @Test
    fun `toggleStuckStatus - KOSPI는 market과 별도 StateFlow에 반영`() {
        coEvery { observeStuckCounter.fetchOnce(any()) } returns StuckCounterResult.EMPTY

        val viewModel = createViewModel()
        viewModel.toggleStuckStatus(FearIndexType.KOSPI, StuckStatus.STUCK)

        assertEquals(StuckStatus.NONE, viewModel.myMarketStatus.value)
        assertEquals(StuckStatus.STUCK, viewModel.myKospiStatus.value)
        assertEquals(1, viewModel.kospiResult.value.stuckCount)
        assertEquals(0, viewModel.marketResult.value.stuckCount)
    }

    @Test
    fun `toggleStuckStatus - debouncer schedule 호출`() {
        coEvery { observeStuckCounter.fetchOnce(any()) } returns StuckCounterResult.EMPTY

        val viewModel = createViewModel()
        viewModel.toggleStuckStatus(FearIndexType.CRYPTO, StuckStatus.SAFE)

        coVerify(exactly = 1) { debouncer.schedule(FearIndexType.CRYPTO, StuckStatus.SAFE) }
    }
}
