package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.VoteChoice
import th1ngjin.fearindex.domain.entity.VoteResult
import th1ngjin.fearindex.domain.repository.VoteRepository

class SubmitVoteUseCaseTest {

    private val repository = mockk<VoteRepository>()
    private val useCase = SubmitVoteUseCase(repository)

    @Test
    fun `invoke - 투표 제출 성공 시 VoteResult 반환`() = runTest {
        val expected = VoteResult(
            buyCount = 10,
            holdCount = 5,
            sellCount = 3,
            totalCount = 18,
            myVote = VoteChoice.BUY,
        )
        coEvery { repository.submitVote("market", VoteChoice.BUY, 42) } returns expected

        val result = useCase(indexType = "market", choice = VoteChoice.BUY, fearScore = 42)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.submitVote("market", VoteChoice.BUY, 42) }
    }

    @Test
    fun `invoke - SELL 투표 전달`() = runTest {
        val expected = VoteResult(
            buyCount = 10,
            holdCount = 5,
            sellCount = 4,
            totalCount = 19,
            myVote = VoteChoice.SELL,
        )
        coEvery { repository.submitVote("crypto", VoteChoice.SELL, 80) } returns expected

        val result = useCase(indexType = "crypto", choice = VoteChoice.SELL, fearScore = 80)

        assertEquals(expected, result)
    }

    @Test(expected = RuntimeException::class)
    fun `invoke - repository 예외 시 throw 전파`() = runTest {
        coEvery { repository.submitVote(any(), any(), any()) } throws RuntimeException("Submit failed")

        useCase(indexType = "market", choice = VoteChoice.HOLD, fearScore = 50)
    }
}
