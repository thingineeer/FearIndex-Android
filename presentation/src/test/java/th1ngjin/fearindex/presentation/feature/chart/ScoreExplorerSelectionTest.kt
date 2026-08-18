package th1ngjin.fearindex.presentation.feature.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.defaults.DefaultReturnData
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.util.ScoreExplorerStats

/**
 * iOS `ScoreExplorerSelectionTests` 포팅 — 초기값=현재 점수 / 클램프 / 리셋 / 세그먼트 전환 / 좁은 범위 도착.
 */
class ScoreExplorerSelectionTest {

    private val marketRange = ScoreExplorerStats.scoreRange(DefaultReturnData.market)!!   // 0..97
    private val cryptoRange = ScoreExplorerStats.scoreRange(DefaultReturnData.crypto)!!   // 5..95

    private fun market(current: Int? = 46) =
        ScoreExplorerSelection().bind(FearIndexType.MARKET, current, marketRange)

    // MARK: - 초기값 = 현재 점수

    @Test
    fun `initial selection equals current score and is at current`() {
        val s = market(46)
        assertEquals(46, s.selectedScore)
        assertEquals(46, s.anchorScore)
        assertTrue(s.isAtCurrent)
    }

    @Test
    fun `null current score falls back to range lower bound`() {
        val s = market(current = null)
        assertEquals(marketRange.first, s.selectedScore)
        assertTrue(s.isAtCurrent)
    }

    @Test
    fun `no range and no current score falls back to 50`() {
        val s = ScoreExplorerSelection().bind(FearIndexType.KOSPI, null, null)
        assertEquals(ScoreExplorerSelection.DEFAULT_SCORE, s.selectedScore)
        assertTrue(s.isAtCurrent)
    }

    @Test
    fun `current score outside range is clamped to the anchor`() {
        val s = ScoreExplorerSelection().bind(FearIndexType.MARKET, 46, 30..40)
        assertEquals(40, s.anchorScore)
        assertEquals(40, s.selectedScore)
        assertTrue(s.isAtCurrent)
    }

    // MARK: - 이동 + 클램프

    @Test
    fun `move clamps to range and leaves current position`() {
        var s = market(46).move(20)
        assertEquals(20, s.selectedScore)
        assertFalse(s.isAtCurrent)

        s = s.move(999)
        assertEquals(97, s.selectedScore)

        s = s.move(-10)
        assertEquals(0, s.selectedScore)
    }

    @Test
    fun `moving back onto the current score counts as reset`() {
        val s = market(46).move(10).move(46)
        assertTrue(s.isAtCurrent)
        assertTrue(s.userSelections.isEmpty())
    }

    // MARK: - 리셋

    @Test
    fun `reset returns to current score`() {
        val s = market(46).move(10).reset()
        assertEquals(46, s.selectedScore)
        assertTrue(s.isAtCurrent)
    }

    @Test
    fun `untouched selection follows current score updates`() {
        val s = market(current = null)
        assertEquals(marketRange.first, s.selectedScore)

        val loaded = s.bind(FearIndexType.MARKET, 46, marketRange)
        assertEquals(46, loaded.selectedScore)
        assertTrue(loaded.isAtCurrent)
    }

    @Test
    fun `moved selection does not follow current score updates`() {
        val s = market(46).move(20).bind(FearIndexType.MARKET, 60, marketRange)
        assertEquals(20, s.selectedScore)
        assertEquals(60, s.anchorScore)
        assertFalse(s.isAtCurrent)
    }

    // MARK: - 세그먼트 전환

    @Test
    fun `switching asset shows that asset current score and keeps the moved asset selection`() {
        val moved = market(46).move(20)

        val crypto = moved.bind(FearIndexType.CRYPTO, 35, cryptoRange)
        assertEquals(35, crypto.selectedScore)
        assertTrue(crypto.isAtCurrent)

        val back = crypto.bind(FearIndexType.MARKET, 46, marketRange)
        assertEquals(20, back.selectedScore)
        assertFalse(back.isAtCurrent)
    }

    // MARK: - 좁은 테이블 도착

    @Test
    fun `earlier selection is clamped when a narrower table arrives`() {
        val s = market(46).move(90)
        assertEquals(90, s.selectedScore)

        val narrowed = s.bind(FearIndexType.MARKET, 46, 30..40)
        assertEquals(40, narrowed.selectedScore)
        assertTrue(narrowed.isAtCurrent)
    }
}
