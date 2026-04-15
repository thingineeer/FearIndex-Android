package th1ngjin.fearindex.domain.usecase

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.VoteChoice
import th1ngjin.fearindex.domain.entity.VoteResult
import th1ngjin.fearindex.domain.repository.VoteRepository

class ObserveVoteResultUseCaseTest {

    private val repository = mockk<VoteRepository>()
    private val useCase = ObserveVoteResultUseCase(repository)

    @Test
    fun `invoke - Flow 방출 확인`() = runTest {
        val result1 = VoteResult.EMPTY
        val result2 = VoteResult(
            buyCount = 5,
            holdCount = 3,
            sellCount = 2,
            totalCount = 10,
            myVote = VoteChoice.BUY,
        )
        every { repository.voteResultStream("market") } returns flowOf(result1, result2)

        useCase("market").test {
            assertEquals(result1, awaitItem())
            assertEquals(result2, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `invoke - crypto 타입 전달`() = runTest {
        val expected = VoteResult.EMPTY
        every { repository.voteResultStream("crypto") } returns flowOf(expected)

        useCase("crypto").test {
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }
}
