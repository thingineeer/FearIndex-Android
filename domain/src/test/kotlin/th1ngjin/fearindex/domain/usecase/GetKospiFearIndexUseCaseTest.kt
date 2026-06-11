package th1ngjin.fearindex.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.KospiConfidence
import th1ngjin.fearindex.domain.entity.KospiFearIndex
import th1ngjin.fearindex.domain.entity.KospiSnapshotType
import th1ngjin.fearindex.domain.repository.KospiFearIndexRepository
import java.io.IOException
import java.time.Instant

class GetKospiFearIndexUseCaseTest {

    private val repository = mockk<KospiFearIndexRepository>()
    private val useCase = GetKospiFearIndexUseCase(repository)

    @Test
    fun `invoke - 성공 시 KospiFearIndex 반환`() = runTest {
        val expected = createKospiFearIndex(score = 61.0)
        coEvery { repository.fetchCurrent(false) } returns expected

        val result = useCase()

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.fetchCurrent(false) }
    }

    @Test
    fun `invoke - forceRefresh true 전달`() = runTest {
        val expected = createKospiFearIndex(score = 72.0)
        coEvery { repository.fetchCurrent(true) } returns expected

        val result = useCase(forceRefresh = true)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.fetchCurrent(true) }
    }

    @Test(expected = IOException::class)
    fun `invoke - repository 예외 시 throw 전파`() = runTest {
        coEvery { repository.fetchCurrent(any()) } throws IOException("Network error")

        useCase()
    }

    private fun createKospiFearIndex(score: Double) = KospiFearIndex(
        fearIndex = FearIndex(
            score = score,
            rating = FearIndex.Rating.from(score),
            timestamp = Instant.parse("2026-06-10T06:00:00Z"),
        ),
        snapshotType = KospiSnapshotType.INTRADAY,
        isFinal = false,
        dataDate = "2026-06-10",
        generatedAt = Instant.parse("2026-06-10T06:00:00Z"),
        confidence = KospiConfidence.HIGH,
        signals = emptyList(),
        missingSignals = emptyList(),
        clusterScores = emptyMap(),
        clusterDivergence = 0.0,
    )
}
