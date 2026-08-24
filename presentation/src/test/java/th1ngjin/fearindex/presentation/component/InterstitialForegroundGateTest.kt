package th1ngjin.fearindex.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstitialForegroundGateTest {

    @Test
    fun `세션 리셋 기준은 백그라운드 10분이다`() {
        assertEquals(10L * 60L * 1_000L, InterstitialForegroundGate.SESSION_RESET_BACKGROUND_MILLIS)
    }

    @Test
    fun `백그라운드 10분 이상이면 새 세션으로 판정한다`() {
        assertTrue(InterstitialForegroundGate.shouldResetSession(backgroundMillis = 10L * 60L * 1_000L))
        assertTrue(InterstitialForegroundGate.shouldResetSession(backgroundMillis = 2L * 60L * 60L * 1_000L))
    }

    @Test
    fun `백그라운드 10분 미만이면 같은 세션이다`() {
        assertFalse(InterstitialForegroundGate.shouldResetSession(backgroundMillis = 10L * 60L * 1_000L - 1L))
        assertFalse(InterstitialForegroundGate.shouldResetSession(backgroundMillis = 0L))
    }
}
