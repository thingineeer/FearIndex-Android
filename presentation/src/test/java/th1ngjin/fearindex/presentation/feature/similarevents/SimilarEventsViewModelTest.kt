package th1ngjin.fearindex.presentation.feature.similarevents

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
import org.junit.Before
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.repository.SimilarEventsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SimilarEventsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<SimilarEventsRepository>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observe(any()) } returns emptyFlow()
        coEvery { repository.triggerCallable(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init - 서버 비용 절감을 위해 listener를 선제 시작하지 않는다`() {
        SimilarEventsViewModel(repository)

        coVerify(exactly = 0) { repository.observe(any()) }
    }

    @Test
    fun `ensureObserved - 요청한 index만 listener를 시작한다`() {
        val viewModel = SimilarEventsViewModel(repository)

        viewModel.ensureObserved(FearIndexType.KOSPI)

        coVerify(exactly = 1) { repository.observe(FearIndexType.KOSPI) }
        coVerify(exactly = 0) { repository.observe(FearIndexType.MARKET) }
        coVerify(exactly = 0) { repository.observe(FearIndexType.CRYPTO) }
    }

    @Test
    fun `triggerForScore - callable 전에 해당 index listener를 시작한다`() {
        val viewModel = SimilarEventsViewModel(repository)

        viewModel.triggerForScore(FearIndexType.CRYPTO, 24)

        coVerify(exactly = 1) { repository.observe(FearIndexType.CRYPTO) }
        coVerify(exactly = 1) { repository.triggerCallable(FearIndexType.CRYPTO, 24) }
    }
}
