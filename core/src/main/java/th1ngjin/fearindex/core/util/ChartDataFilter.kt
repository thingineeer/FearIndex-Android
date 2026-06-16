package th1ngjin.fearindex.core.util

import th1ngjin.fearindex.domain.entity.FearIndex
import java.time.Instant
import kotlin.math.abs

/**
 * 차트 데이터 필터링/샘플링 유틸리티.
 *
 * iOS `ChartDataFilter.swift`와 1:1 대응.
 * - 기간별 필터링 (TimeInterval 기반)
 * - 데이터 없을 시 폴백 (최근 N개)
 * - 샘플링 (maxSamplePoints 기준 균등 분배)
 * - 이진 검색 (findClosestBinary)
 */
object ChartDataFilter {

    /**
     * 기간별 데이터 필터링.
     *
     * @param data 전체 FearIndex 데이터
     * @param days 기간 일수 (예: 90, 180, 365)
     * @param maxSamplePoints 최대 샘플 포인트 수 (null이면 샘플링 안 함)
     * @return 필터링+샘플링된 데이터 (timestamp 오름차순)
     */
    fun filter(
        data: List<FearIndex>,
        days: Int,
        maxSamplePoints: Int? = null,
    ): List<FearIndex> {
        val now = Instant.now()
        val periodSeconds = days.toLong() * 24 * 60 * 60
        val startDate = now.minusSeconds(periodSeconds)

        val filtered = data.filter { item ->
            !item.timestamp.isBefore(startDate) && !item.timestamp.isAfter(now)
        }

        // 데이터가 없으면 가장 최근 N개 표시 (폴백)
        if (filtered.isEmpty()) {
            return fallbackRecentData(data, days, maxSamplePoints)
        }

        val sorted = filtered.sortedBy { it.timestamp }

        return samplePeakPreserving(sorted, maxSamplePoints)
    }

    /**
     * 가장 가까운 데이터 포인트 찾기 (O(n)).
     */
    fun findClosest(date: Instant, data: List<FearIndex>): FearIndex? {
        return data.minByOrNull { abs(it.timestamp.epochSecond - date.epochSecond) }
    }

    /**
     * 이진 검색으로 가장 가까운 데이터 포인트 찾기 (O(log n)).
     * 데이터가 timestamp 기준 오름차순 정렬되어 있어야 한다.
     */
    fun findClosestBinary(date: Instant, data: List<FearIndex>): FearIndex? {
        if (data.isEmpty()) return null
        if (data.size == 1) return data[0]

        val targetTime = date.epochSecond
        var low = 0
        var high = data.size - 1

        while (low < high) {
            val mid = (low + high) / 2
            if (data[mid].timestamp.epochSecond < targetTime) {
                low = mid + 1
            } else {
                high = mid
            }
        }

        if (low == 0) return data[0]
        if (low >= data.size) return data[data.size - 1]

        val before = data[low - 1]
        val after = data[low]
        val diffBefore = abs(before.timestamp.epochSecond - date.epochSecond)
        val diffAfter = abs(after.timestamp.epochSecond - date.epochSecond)

        return if (diffBefore <= diffAfter) before else after
    }

    /**
     * 기간 내 데이터 개수 계산.
     */
    fun count(data: List<FearIndex>, days: Int): Int {
        val now = Instant.now()
        val periodSeconds = days.toLong() * 24 * 60 * 60
        val startDate = now.minusSeconds(periodSeconds)

        return data.count { item ->
            !item.timestamp.isBefore(startDate) && !item.timestamp.isAfter(now)
        }
    }

    /**
     * 터치 X 좌표로부터 가장 가까운 데이터 인덱스를 계산.
     * ChartScreen의 inline nearestIndex 로직을 공통화.
     *
     * @param touchX 터치 X 좌표 (px)
     * @param width 차트 전체 너비 (px)
     * @param size 데이터 포인트 개수
     * @return 가장 가까운 데이터 인덱스
     */
    fun nearestIndex(touchX: Float, width: Float, size: Int): Int {
        if (size <= 1) return 0
        val ratio = (touchX / width).coerceIn(0f, 1f)
        val base = (ratio * (size - 1)).toInt().coerceIn(0, size - 1)
        val baseX = width * base / (size - 1)
        val nextIndex = (base + 1).coerceAtMost(size - 1)
        val nextX = width * nextIndex / (size - 1)
        return if (abs(touchX - nextX) < abs(touchX - baseX)) nextIndex else base
    }

    // -- Private --

    /** 폴백: 가장 최근 데이터 N개 반환. */
    private fun fallbackRecentData(
        data: List<FearIndex>,
        days: Int,
        maxSamplePoints: Int?,
    ): List<FearIndex> {
        val sorted = data.sortedBy { it.timestamp }
        val count = maxSamplePoints ?: days
        return sorted.takeLast(count)
    }

    /**
     * 데이터 샘플링 — **peak-preserving**. 원본 min/max + 좌우 이웃 포인트를
     * 항상 포함시켜 라인과 고점/저점 마커가 시각적으로 정확히(1px 오차 없이) 일치하도록 보장한다.
     *
     * 고정 step 샘플링은 원본 peak index 가 시퀀스에 없으면 마커가 라인 위 허공에 뜨는
     * 버그를 유발 → extrema 를 anchor 로 고정. (iOS #19 회귀 fix 와 1:1 대응)
     *
     * @return min/max anchor 를 포함하고 timestamp 오름차순 정렬된, 정확히 maxPoints 개의 샘플.
     */
    internal fun samplePeakPreserving(data: List<FearIndex>, maxPoints: Int?): List<FearIndex> {
        if (maxPoints == null) return data
        if (data.size <= maxPoints) return data

        val (minIdx, maxIdx) = extremaIndices(data)
        val selected = collectAnchors(data, minIdx, maxIdx)
        expandNeighbors(selected, data, minIdx, maxIdx)
        fillRemainingSamples(selected, data, maxPoints)
        return selected.sorted().map { data[it] }
    }

    /** 시작/끝/min/max 4 anchor 를 수집. */
    private fun collectAnchors(data: List<FearIndex>, minIdx: Int, maxIdx: Int): MutableSet<Int> {
        return mutableSetOf(0, data.size - 1, minIdx, maxIdx)
    }

    /** 각 extremum 좌우 1 포인트씩 추가 (peak 주변 세그먼트 밀도 유지). */
    private fun expandNeighbors(
        selected: MutableSet<Int>,
        data: List<FearIndex>,
        minIdx: Int,
        maxIdx: Int,
    ) {
        for (neighbor in intArrayOf(minIdx - 1, minIdx + 1, maxIdx - 1, maxIdx + 1)) {
            if (neighbor in data.indices) selected.add(neighbor)
        }
    }

    /**
     * 잔여 슬롯을 균등 step 으로 채움. anchor 와 충돌하면 자연스럽게 dedup.
     * 그래도 부족하면 앞에서부터 채워 정확히 maxPoints 개를 보장.
     */
    private fun fillRemainingSamples(
        selected: MutableSet<Int>,
        data: List<FearIndex>,
        maxPoints: Int,
    ) {
        if (selected.size >= maxPoints) return
        val step = (data.size - 1).toDouble() / (maxPoints - 1).toDouble()
        var i = 0
        while (i < maxPoints && selected.size < maxPoints) {
            selected.add((i * step).let { Math.round(it).toInt() })
            i++
        }
        var index = 0
        while (selected.size < maxPoints && index < data.size) {
            selected.add(index)
            index++
        }
    }

    /** 원본에서 (최저 score index, 최고 score index). 동점 시 최근(뒤) 우선. */
    private fun extremaIndices(data: List<FearIndex>): Pair<Int, Int> {
        var minIdx = 0
        var maxIdx = 0
        for ((i, item) in data.withIndex()) {
            if (item.score >= data[maxIdx].score) maxIdx = i
            if (item.score <= data[minIdx].score) minIdx = i
        }
        return minIdx to maxIdx
    }
}
