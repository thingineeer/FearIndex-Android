package th1ngjin.fearindex.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.domain.entity.FearIndex
import java.time.Instant

/**
 * peak-preserving 다운샘플링 검증.
 *
 * 핵심 불변식: 샘플링 결과는 **원본의 최저(min)/최고(max) score 포인트를 반드시 포함**해야 한다.
 * 그래야 차트 위 고점/저점 마커가 라인 위 실제 데이터 포인트와 1px 오차 없이 정확히 일치한다.
 *
 * iOS `ChartDataFilter.sample` (#19 회귀 fix) 와 1:1 대응.
 */
class SamplePeakPreservingTest {

    /** 오름차순 일 단위 타임스탬프 + score 배열로 FearIndex 목록 생성. */
    private fun makeHistory(scores: List<Double>): List<FearIndex> {
        val base = Instant.ofEpochSecond(1_700_000_000)
        return scores.mapIndexed { idx, score ->
            FearIndex(
                score = score,
                rating = FearIndex.Rating.from(score),
                timestamp = base.plusSeconds(idx.toLong() * 86_400),
            )
        }
    }

    /** 원본 기준 (min index, max index). 동점 시 최근(뒤) 우선 — ComputeFearIndexPeaks 와 동일. */
    private fun extrema(data: List<FearIndex>): Pair<Int, Int> {
        var minIdx = 0
        var maxIdx = 0
        for ((i, item) in data.withIndex()) {
            if (item.score >= data[maxIdx].score) maxIdx = i
            if (item.score <= data[minIdx].score) minIdx = i
        }
        return minIdx to maxIdx
    }

    // MARK: - No-op cases

    @Test
    fun `maxPoints null 이면 원본 그대로`() {
        val data = makeHistory(listOf(10.0, 90.0, 20.0))
        assertEquals(data, ChartDataFilter.samplePeakPreserving(data, null))
    }

    @Test
    fun `데이터가 maxPoints 이하면 원본 그대로`() {
        val data = makeHistory(listOf(10.0, 90.0, 20.0, 50.0))
        assertEquals(data, ChartDataFilter.samplePeakPreserving(data, 10))
    }

    // MARK: - 핵심 불변식: min/max 포함

    @Test
    fun `샘플링 결과는 원본의 min과 max score 를 정확히 포함`() {
        // 균등 step 샘플링이면 누락될 위치(홀수 인덱스)에 극값을 배치.
        val scores = (0 until 200).map { 50.0 }.toMutableList()
        scores[37] = 97.3   // 최고점 (홀수 위치)
        scores[133] = 4.1   // 최저점 (홀수 위치)
        val data = makeHistory(scores)

        val sampled = ChartDataFilter.samplePeakPreserving(data, 50)
        val sampledScores = sampled.map { it.score }

        assertTrue("최고점 97.3 이 샘플에 포함되어야 함", sampledScores.contains(97.3))
        assertTrue("최저점 4.1 이 샘플에 포함되어야 함", sampledScores.contains(4.1))
    }

    @Test
    fun `샘플링 결과의 max timestamp 가 원본 max 포인트와 동일 (1px 일치 보장)`() {
        val scores = (0 until 300).map { (it % 40).toDouble() }.toMutableList()
        scores[251] = 100.0 // 유일 최고점, 홀수/비정렬 위치
        val data = makeHistory(scores)
        val (minIdx, maxIdx) = extrema(data)

        val sampled = ChartDataFilter.samplePeakPreserving(data, 60)

        // 원본 max 포인트의 timestamp 가 샘플에 그대로 있어야 한다.
        assertTrue(sampled.any { it.timestamp == data[maxIdx].timestamp })
        assertTrue(sampled.any { it.timestamp == data[minIdx].timestamp })
    }

    @Test
    fun `동점 max 여러개면 최근(뒤) 포인트를 보존`() {
        val scores = (0 until 100).map { 30.0 }.toMutableList()
        scores[10] = 80.0
        scores[90] = 80.0 // 동점 max, 최근 = index 90
        val data = makeHistory(scores)

        val sampled = ChartDataFilter.samplePeakPreserving(data, 20)

        // ComputeFearIndexPeaks 가 고를 max(index 90)의 timestamp 가 샘플에 있어야
        // 마커와 라인이 일치한다.
        assertTrue(sampled.any { it.timestamp == data[90].timestamp })
    }

    // MARK: - 시작/끝 anchor

    @Test
    fun `샘플링은 항상 첫 포인트와 마지막 포인트를 포함`() {
        val data = makeHistory((0 until 500).map { (it * 7 % 101).toDouble() })
        val sampled = ChartDataFilter.samplePeakPreserving(data, 100)

        assertEquals(data.first().timestamp, sampled.first().timestamp)
        assertEquals(data.last().timestamp, sampled.last().timestamp)
    }

    // MARK: - 결과 크기/정렬

    @Test
    fun `샘플 개수는 maxPoints 와 정확히 일치`() {
        val data = makeHistory((0 until 1000).map { ((it * 37 + 13) % 101).toDouble() })
        val sampled = ChartDataFilter.samplePeakPreserving(data, 200)
        assertEquals(200, sampled.size)
    }

    @Test
    fun `샘플 결과는 timestamp 오름차순 정렬`() {
        val data = makeHistory((0 until 600).map { ((it * 53 + 7) % 101).toDouble() })
        val sampled = ChartDataFilter.samplePeakPreserving(data, 120)

        for (i in 1 until sampled.size) {
            assertTrue(
                "index $i 정렬 위반",
                !sampled[i].timestamp.isBefore(sampled[i - 1].timestamp),
            )
        }
    }

    @Test
    fun `샘플 결과에 중복 timestamp 없음`() {
        val data = makeHistory((0 until 800).map { ((it * 41 + 3) % 101).toDouble() })
        val sampled = ChartDataFilter.samplePeakPreserving(data, 150)

        val distinct = sampled.map { it.timestamp }.distinct()
        assertEquals(sampled.size, distinct.size)
    }
}
