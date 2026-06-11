package th1ngjin.fearindex.data.repository

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.VoteDataSource
import th1ngjin.fearindex.data.storage.StuckCounterStorage
import th1ngjin.fearindex.data.storage.VoteStorage
import th1ngjin.fearindex.domain.entity.VoteChoice
import th1ngjin.fearindex.domain.entity.VoteResult

class VoteRepositoryImplTest {

    private val dataSource = mockk<VoteDataSource>(relaxed = true)
    private val voteStorage = mockk<VoteStorage>(relaxed = true)
    private val deviceStorage = mockk<StuckCounterStorage>(relaxed = true)
    private val repository = VoteRepositoryImpl(dataSource, voteStorage, deviceStorage)

    @Test
    fun `screenshot mode - legacy submit은 callable을 호출하지 않는다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)

        try {
            val result = repository.submitVote("market", VoteChoice.BUY, 42)

            assertEquals(VoteResult.EMPTY, result)
            verify(exactly = 0) { deviceStorage.loadDeviceId() }
            verify(exactly = 0) { voteStorage.saveMyVote(any(), any()) }
            coVerify(exactly = 0) { dataSource.submitVote(any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }

    @Test
    fun `screenshot mode - legacy result 조회는 callable을 호출하지 않는다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)

        try {
            val result = repository.getVoteResult("crypto")

            assertEquals(VoteResult.EMPTY, result)
            verify(exactly = 0) { deviceStorage.loadDeviceId() }
            verify(exactly = 0) { voteStorage.saveMyVote(any(), any()) }
            coVerify(exactly = 0) { dataSource.getVoteResult(any(), any(), any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }

    @Test
    fun `screenshot mode - legacy stream은 Firestore listener를 열지 않는다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)

        try {
            val result = repository.voteResultStream("kospi").first()

            assertEquals(VoteResult.EMPTY, result)
            verify(exactly = 0) { dataSource.voteResultStream(any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }

    @Test
    fun `loadMyVote - 로컬 캐시를 domain choice로 변환한다`() {
        every { voteStorage.loadMyVote("market") } returns "sell"

        assertEquals(VoteChoice.SELL, repository.loadMyVote("market"))
    }
}
