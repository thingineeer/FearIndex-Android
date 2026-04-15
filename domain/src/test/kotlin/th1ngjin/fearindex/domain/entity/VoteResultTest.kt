package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoteResultTest {

    @Test
    fun `buyPercentage - 정상 계산`() {
        val result = VoteResult(buyCount = 30, holdCount = 50, sellCount = 20, totalCount = 100, myVote = null)
        assertEquals(30.0, result.buyPercentage, 0.01)
    }

    @Test
    fun `holdPercentage - 정상 계산`() {
        val result = VoteResult(buyCount = 30, holdCount = 50, sellCount = 20, totalCount = 100, myVote = null)
        assertEquals(50.0, result.holdPercentage, 0.01)
    }

    @Test
    fun `sellPercentage - 정상 계산`() {
        val result = VoteResult(buyCount = 30, holdCount = 50, sellCount = 20, totalCount = 100, myVote = null)
        assertEquals(20.0, result.sellPercentage, 0.01)
    }

    @Test
    fun `totalCount 0이면 모든 비율이 0`() {
        val result = VoteResult.EMPTY
        assertEquals(0.0, result.buyPercentage, 0.01)
        assertEquals(0.0, result.holdPercentage, 0.01)
        assertEquals(0.0, result.sellPercentage, 0.01)
    }

    @Test
    fun `EMPTY는 모든 값이 0이고 myVote가 null`() {
        val empty = VoteResult.EMPTY
        assertEquals(0, empty.buyCount)
        assertEquals(0, empty.holdCount)
        assertEquals(0, empty.sellCount)
        assertEquals(0, empty.totalCount)
        assertNull(empty.myVote)
    }
}
