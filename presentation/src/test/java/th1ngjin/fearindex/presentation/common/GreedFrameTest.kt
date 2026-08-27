package th1ngjin.fearindex.presentation.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GreedFrameTest {
    @Test
    fun `70 이상은 탐욕(과열) 프레임`() {
        assertTrue(GreedFrame.isGreed(70))
        assertTrue(GreedFrame.isGreed(71)) // 회장님 예시: 암호화폐 71 탐욕 알림
        assertTrue(GreedFrame.isGreed(100))
    }

    @Test
    fun `69 이하는 기존 매수 프레임 유지`() {
        assertFalse(GreedFrame.isGreed(69))
        assertFalse(GreedFrame.isGreed(50))
        assertFalse(GreedFrame.isGreed(0))
    }

    @Test
    fun `임계값은 알림 상한 기본값 70 과 같다`() {
        org.junit.Assert.assertEquals(70, GreedFrame.THRESHOLD)
    }
}
