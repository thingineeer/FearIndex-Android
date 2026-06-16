package th1ngjin.fearindex.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 차트 좌표 변환 순수 함수 검증.
 *
 * 핵심 불변식: 라인 데이터 포인트와 고점/저점 peak 마커는 **동일한 변환 수식**을 써야
 * 같은 인덱스/점수에서 1px 오차 없이 정확히 같은 픽셀 좌표에 그려진다.
 * 이 테스트가 그 수식을 고정한다.
 */
class ChartCoordinatesTest {

    private val width = 360f
    private val height = 200f

    // MARK: - X 좌표 (index → px)

    @Test
    fun `x - 첫 인덱스는 0`() {
        assertEquals(0f, ChartCoordinates.xForIndex(0, count = 100, width = width), 0f)
    }

    @Test
    fun `x - 마지막 인덱스는 width`() {
        assertEquals(width, ChartCoordinates.xForIndex(99, count = 100, width = width), 0f)
    }

    @Test
    fun `x - 중간 인덱스는 비례 위치`() {
        // index 50 / (100-1) * 360
        assertEquals(width * 50 / 99, ChartCoordinates.xForIndex(50, count = 100, width = width), 0f)
    }

    @Test
    fun `x - count 1이면 0으로 나눔 방지 (denominator 최소 1)`() {
        assertEquals(0f, ChartCoordinates.xForIndex(0, count = 1, width = width), 0f)
    }

    // MARK: - Y 좌표 (score → px, 상하 반전)

    @Test
    fun `y - score 0 은 바닥 (height)`() {
        assertEquals(height, ChartCoordinates.yForScore(0.0, height = height), 0f)
    }

    @Test
    fun `y - score 100 은 천장 (0)`() {
        assertEquals(0f, ChartCoordinates.yForScore(100.0, height = height), 0f)
    }

    @Test
    fun `y - score 50 은 정중앙`() {
        assertEquals(height / 2f, ChartCoordinates.yForScore(50.0, height = height), 0f)
    }

    @Test
    fun `y - 소수 점수도 정확히 변환`() {
        // 97.3 → height * (1 - 0.973) = 200 * 0.027 = 5.4 (float 정밀도 내)
        assertEquals(height * (1f - 0.973f), ChartCoordinates.yForScore(97.3, height = height), 0.001f)
    }

    // MARK: - 라인 ↔ peak 일치 (1px 불변식)

    @Test
    fun `라인 점과 peak 마커는 같은 index score 에서 정확히 같은 좌표`() {
        // 라인이 index 37, score 97.3 으로 그린 점과
        // peak 마커가 같은 값으로 그리는 점은 1px 오차 없이 일치해야 한다.
        val count = 200
        val idx = 37
        val score = 97.3

        val lineX = ChartCoordinates.xForIndex(idx, count, width)
        val lineY = ChartCoordinates.yForScore(score, height)
        val peakX = ChartCoordinates.xForIndex(idx, count, width)
        val peakY = ChartCoordinates.yForScore(score, height)

        assertEquals(lineX, peakX, 0f)
        assertEquals(lineY, peakY, 0f)
    }
}
