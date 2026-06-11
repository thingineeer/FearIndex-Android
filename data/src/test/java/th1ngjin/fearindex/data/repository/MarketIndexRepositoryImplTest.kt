package th1ngjin.fearindex.data.repository

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.MarketIndexDataSource

class MarketIndexRepositoryImplTest {

    private val dataSource = mockk<MarketIndexDataSource>()
    private val repository = MarketIndexRepositoryImpl(dataSource)

    @Test
    fun `screenshot mode - network 없이 ticker fixture를 반환한다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)
        try {
            val result = repository.getMarketIndices()

            assertEquals(5, result.size)
            assertEquals("^KS11", result.first().symbol)
            assertEquals("KOSPI", result.first().name)
            assertEquals("Dow Jones", result.last().name)
            assertFalse(result.any { it.name.any(::isHangul) })
            coVerify(exactly = 0) { dataSource.fetch(any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }

    private fun isHangul(char: Char): Boolean = char in '\uAC00'..'\uD7A3'
}
