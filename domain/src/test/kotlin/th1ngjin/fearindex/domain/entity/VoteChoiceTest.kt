package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoteChoiceTest {

    @Test
    fun `fromServer - buy 문자열이면 BUY 반환`() {
        assertEquals(VoteChoice.BUY, VoteChoice.fromServer("buy"))
    }

    @Test
    fun `fromServer - hold 문자열이면 HOLD 반환`() {
        assertEquals(VoteChoice.HOLD, VoteChoice.fromServer("hold"))
    }

    @Test
    fun `fromServer - sell 문자열이면 SELL 반환`() {
        assertEquals(VoteChoice.SELL, VoteChoice.fromServer("sell"))
    }

    @Test
    fun `fromServer - 알 수 없는 문자열이면 null 반환`() {
        assertNull(VoteChoice.fromServer("unknown"))
    }

    @Test
    fun `fromServer - null이면 null 반환`() {
        assertNull(VoteChoice.fromServer(null))
    }

    @Test
    fun `value 프로퍼티 - 서버 전송 값 확인`() {
        assertEquals("buy", VoteChoice.BUY.value)
        assertEquals("hold", VoteChoice.HOLD.value)
        assertEquals("sell", VoteChoice.SELL.value)
    }
}
