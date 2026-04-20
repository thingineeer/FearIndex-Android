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
 * - init 시 loadInitialStuckResults → fetchOnce(MARKET) + fetchOnce(CRYPTO) 호출
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
    fun `init - MARKET과 CRYPTO 초기 fetchOnce 호출 (QA#3 해결)`() {
        coEvery { observeStuckCounter.fetchOnce(any()) } returns StuckCounterResult.EMPTY

        createViewModel()

        coVerify(exactly = 1) { observeStuckCounter.fetchOnce(FearIndexType.MARKET) }
        coVerify(exactly = 1) { observeStuckCounter.fetchOnce(FearIndexType.CRYPTO) }
    }

    @Test
    fun `init - 초기 fetch 결과로 marketResult와 cryptoResult StateFlow 업데이트`() {
        val marketInit = StuckCounterResult(
            stuckCount = 3, safeCount = 7, totalResponded = 10,
            stuckPercentage = 30.0, safePercentage = 70.0, myStatus = StuckStatus.NONE,
        )
        val cryptoInit = StuckCounterResult(
            stuckCount = 5, safeCount = 5, totalResponded = 10,
            stuckPercentage = 50.0, safePercentage = 50.0, myStatus = StuckStatus.NONE,
        )
        coEvery { observeStuckCounter.fetchOnce(FearIndexType.MARKET) } returns marketInit
        coEvery { observeStuckCounter.fetchOnce(FearIndexType.CRYPTO) } returns cryptoInit

        val viewModel = createViewModel()

        assertEquals(3, viewModel.marketResult.value.stuckCount)
        assertEquals(10, viewModel.marketResult.value.totalResponded)
        assertEquals(5, viewModel.cryptoResult.value.stuckCount)
    }

    @Test
    fun `init - fetchOnce 실패해도 ViewModel 정상 생성 (EMPTY 유지)`() {
        coEvery { observeStuckCounter.fetchOnce(any()) } throws RuntimeException("network")

        val viewModel = createViewModel()

        assertEquals(StuckCounterResult.EMPTY, viewModel.marketResult.value)
        assertEquals(StuckCounterResult.EMPTY, viewModel.cryptoResult.value)
    }

    @Test
    fun `toggleStuckStatus - 즉시 myStatus StateFlow 반영 (낙관적 업데이트)`() {
        coEvery { observeStuckCounter.fetchOnce(any()) } returns StuckCounterResult.EMPTY

        val viewModel = createViewModel()
        viewModel.toggleStuckStatus(FearIndexType.MARKET, StuckStatus.STUCK)

        assertEquals(StuckStatus.STUCK, viewModel.myMarketStatus.value)
    }

    @Test
    fun `toggleStuckStatus - debouncer schedule 호출`() {
        coEvery { observeStuckCounter.fetchOnce(any()) } returns StuckCounterResult.EMPTY

        val viewModel = createViewModel()
        viewModel.toggleStuckStatus(FearIndexType.CRYPTO, StuckStatus.SAFE)

        coVerify(exactly = 1) { debouncer.schedule(FearIndexType.CRYPTO, StuckStatus.SAFE) }
    }
}
