package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class StuckStatusTest {

    @Test
    fun `fromServer - stuck이면 STUCK`() {
        assertEquals(StuckStatus.STUCK, StuckStatus.fromServer("stuck"))
    }

    @Test
    fun `fromServer - safe이면 SAFE`() {
        assertEquals(StuckStatus.SAFE, StuckStatus.fromServer("safe"))
    }

    @Test
    fun `fromServer - none이면 NONE`() {
        assertEquals(StuckStatus.NONE, StuckStatus.fromServer("none"))
    }

    @Test
    fun `fromServer - 알 수 없는 문자열이면 NONE`() {
        assertEquals(StuckStatus.NONE, StuckStatus.fromServer("unknown"))
    }

    @Test
    fun `fromServer - null이면 NONE`() {
        assertEquals(StuckStatus.NONE, StuckStatus.fromServer(null))
    }

    @Test
    fun `serverValue - 서버 전송 값 확인`() {
        assertEquals("stuck", StuckStatus.STUCK.serverValue)
        assertEquals("safe", StuckStatus.SAFE.serverValue)
        assertEquals("none", StuckStatus.NONE.serverValue)
    }
}
