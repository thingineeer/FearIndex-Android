package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StuckCounterResultTest {

    @Test
    fun `shouldShowAbsoluteCount - 100명 이상이면 true`() {
        val result = StuckCounterResult(
            stuckCount = 60,
            safeCount = 40,
            totalResponded = 100,
            stuckPercentage = 60.0,
            safePercentage = 40.0,
            myStatus = StuckStatus.NONE,
        )
        assertTrue(result.shouldShowAbsoluteCount)
    }

    @Test
    fun `shouldShowAbsoluteCount - 100명 미만이면 false`() {
        val result = StuckCounterResult(
            stuckCount = 0,
            safeCount = 0,
            totalResponded = 99,
            stuckPercentage = 60.0,
            safePercentage = 40.0,
            myStatus = StuckStatus.NONE,
        )
        assertFalse(result.shouldShowAbsoluteCount)
    }

    @Test
    fun `EMPTY 기본값 확인`() {
        val empty = StuckCounterResult.EMPTY
        assertEquals(0, empty.stuckCount)
        assertEquals(0, empty.safeCount)
        assertEquals(0, empty.totalResponded)
        assertEquals(0.0, empty.stuckPercentage, 0.01)
        assertEquals(0.0, empty.safePercentage, 0.01)
        assertEquals(StuckStatus.NONE, empty.myStatus)
        assertFalse(empty.shouldShowAbsoluteCount)
    }
}
