package th1ngjin.fearindex.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexPeak
import java.time.Instant

/**
 * iOS `ComputeFearIndexPeaksTests.swift` 와 1:1 대응하는 테스트.
 */
class ComputeFearIndexPeaksTest {

    private val sut = ComputeFearIndexPeaks()

    /** 오름차순 일 단위 타임스탬프 + 주어진 score 배열로 FearIndex 목록 생성. */
    private fun makeHistory(scores: List<Double>): List<FearIndex> {
        val base = Instant.ofEpochSecond(1_700_000_000)
        return scores.mapIndexed { idx, score ->
            FearIndex(
                score = score,
                rating = FearIndex.Rating.from(score),
                timestamp = base.plusSeconds(idx.toLong() * 86_400),
                previousClose = score,
                previous1Week = score,
                previous1Month = score,
                previous1Year = null,
            )
        }
    }

    // MARK: - Basic

    @Test
    fun `빈 배열 - nil 반환`() {
        assertNull(sut(emptyList()))
    }

    @Test
    fun `단일 포인트 - high == low == that point`() {
        val history = makeHistory(listOf(42.0))
        val peaks = sut(history)
        assertNotNull(peaks)
        val (high, low) = peaks!!

        assertEquals(42.0, high.score, 0.0)
        assertEquals(42.0, low.score, 0.0)
        assertEquals(0, high.index)
        assertEquals(0, low.index)
        assertEquals(history[0].timestamp, high.date)
        assertEquals(history[0].timestamp, low.date)
        assertEquals(FearIndexPeak.Kind.HIGH, high.kind)
        assertEquals(FearIndexPeak.Kind.LOW, low.kind)
    }

    @Test
    fun `명확한 peak - 50,80,30 - high=80 low=30`() {
        val history = makeHistory(listOf(50.0, 80.0, 30.0))
        val (high, low) = sut(history)!!

        assertEquals(80.0, high.score, 0.0)
        assertEquals(1, high.index)
        assertEquals(30.0, low.score, 0.0)
        assertEquals(2, low.index)
    }

    // MARK: - Tie-breaking (최근 우선)

    @Test
    fun `동점 최대 2개 - 50,80,60,80,40 - high=최근 80 low=40`() {
        val history = makeHistory(listOf(50.0, 80.0, 60.0, 80.0, 40.0))
        val (high, low) = sut(history)!!

        // 두 번째 80 (index=3) 이 선택되어야 함
        assertEquals(80.0, high.score, 0.0)
        assertEquals(3, high.index)
        assertEquals(40.0, low.score, 0.0)
        assertEquals(4, low.index)
    }

    @Test
    fun `동점 최소 2개 - 60,40,70,40,55 - low=최근 40`() {
        val history = makeHistory(listOf(60.0, 40.0, 70.0, 40.0, 55.0))
        val (high, low) = sut(history)!!

        assertEquals(70.0, high.score, 0.0)
        assertEquals(2, high.index)
        assertEquals(40.0, low.score, 0.0)
        assertEquals(3, low.index) // 두 번째 40
    }

    @Test
    fun `동점 + 단일 값 3개 - 50,50,50 - high low 모두 가장 최근 index`() {
        val history = makeHistory(listOf(50.0, 50.0, 50.0))
        val (high, low) = sut(history)!!

        assertEquals(2, high.index)
        assertEquals(2, low.index)
    }

    // MARK: - Monotonic

    @Test
    fun `오름차순 - 10,20,30 - high=30(마지막) low=10(처음)`() {
        val history = makeHistory(listOf(10.0, 20.0, 30.0))
        val (high, low) = sut(history)!!

        assertEquals(30.0, high.score, 0.0)
        assertEquals(2, high.index)
        assertEquals(10.0, low.score, 0.0)
        assertEquals(0, low.index)
    }

    @Test
    fun `내림차순 - 30,20,10 - high=30(처음) low=10(마지막)`() {
        val history = makeHistory(listOf(30.0, 20.0, 10.0))
        val (high, low) = sut(history)!!

        assertEquals(30.0, high.score, 0.0)
        assertEquals(0, high.index)
        assertEquals(10.0, low.score, 0.0)
        assertEquals(2, low.index)
    }

    // MARK: - Date Mapping

    @Test
    fun `peak date 는 history 의 원본 timestamp 를 보존`() {
        val history = makeHistory(listOf(40.0, 90.0, 20.0, 70.0))
        val (high, low) = sut(history)!!

        assertEquals(history[1].timestamp, high.date)
        assertEquals(history[2].timestamp, low.date)
    }

    // MARK: - Boundaries

    @Test
    fun `0과 100 경계값 - 0,100,50 - high=100 low=0`() {
        val history = makeHistory(listOf(0.0, 100.0, 50.0))
        val (high, low) = sut(history)!!

        assertEquals(100.0, high.score, 0.0)
        assertEquals(0.0, low.score, 0.0)
    }

    // MARK: - Performance

    @Test
    fun `큰 배열 1000 포인트 처리`() {
        // 0..99 범위의 의사 랜덤(결정적) 패턴
        val scores = (0 until 1000).map { idx -> ((idx * 37 + 13) % 101).toDouble() }
        val history = makeHistory(scores)

        val peaks = sut(history)
        assertNotNull(peaks)
        val (high, low) = peaks!!
        assertTrue(high.score >= low.score)
    }
}
