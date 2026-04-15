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

class GetVoteResultUseCaseTest {

    private val repository = mockk<VoteRepository>()
    private val useCase = GetVoteResultUseCase(repository)

    @Test
    fun `invoke - 투표 결과 조회 성공`() = runTest {
        val expected = VoteResult(
            buyCount = 20,
            holdCount = 10,
            sellCount = 5,
            totalCount = 35,
            myVote = VoteChoice.HOLD,
        )
        coEvery { repository.getVoteResult("market") } returns expected

        val result = useCase("market")

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.getVoteResult("market") }
    }

    @Test
    fun `invoke - EMPTY 결과 반환`() = runTest {
        coEvery { repository.getVoteResult("crypto") } returns VoteResult.EMPTY

        val result = useCase("crypto")

        assertEquals(VoteResult.EMPTY, result)
    }

    @Test(expected = RuntimeException::class)
    fun `invoke - repository 예외 시 throw 전파`() = runTest {
        coEvery { repository.getVoteResult(any()) } throws RuntimeException("Not found")

        useCase("market")
    }
}
