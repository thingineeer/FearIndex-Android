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
import th1ngjin.fearindex.data.datasource.StuckCounterDataSource
import th1ngjin.fearindex.data.storage.StuckCounterStorage
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckStatus

class StuckCounterRepositoryImplTest {

    private val dataSource = mockk<StuckCounterDataSource>(relaxed = true)
    private val storage = mockk<StuckCounterStorage>(relaxed = true)
    private val repository = StuckCounterRepositoryImpl(dataSource, storage)

    @Test
    fun `screenshot mode - fetchResult는 서버 없이 fixture를 반환한다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)
        every { storage.loadStatus(FearIndexType.KOSPI) } returns StuckStatus.SAFE

        try {
            val result = repository.fetchResult(FearIndexType.KOSPI)

            assertEquals(183, result.totalResponded)
            assertEquals(StuckStatus.SAFE, result.myStatus)
            coVerify(exactly = 0) { dataSource.fetchResult(any(), any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }

    @Test
    fun `screenshot mode - stream은 Firestore 없이 fixture를 반환한다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)
        every { storage.loadStatus(FearIndexType.MARKET) } returns StuckStatus.NONE

        try {
            val result = repository.stuckCounterStream(FearIndexType.MARKET).first()

            assertEquals(112, result.stuckCount)
            assertEquals(71, result.safeCount)
            verify(exactly = 0) { dataSource.resultStream(any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }

    @Test
    fun `screenshot mode - submit은 로컬 상태만 저장하고 callable을 호출하지 않는다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)

        try {
            val result = repository.submitStuckStatus(FearIndexType.CRYPTO, StuckStatus.STUCK)

            assertEquals(StuckStatus.STUCK, result.myStatus)
            verify(exactly = 1) { storage.saveStatus(FearIndexType.CRYPTO, StuckStatus.STUCK) }
            coVerify(exactly = 0) { dataSource.submitStatus(any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }
}
