package th1ngjin.fearindex.core.util

/**
 * 차트 데이터 → 픽셀 좌표 변환 (순수 함수).
 *
 * 라인 데이터 포인트와 고점/저점 peak 마커가 **동일한 수식**을 공유하도록 하여,
 * 같은 인덱스/점수에서 1px 오차 없이 정확히 같은 픽셀 좌표에 그려지는 것을 보장한다.
 * (별도 inline 계산으로 중복되면 한쪽만 바뀌어 마커가 라인에서 어긋날 수 있음)
 */
object ChartCoordinates {

    /**
     * 데이터 인덱스를 X 픽셀로 변환.
     *
     * @param index 데이터 인덱스 (0 .. count-1)
     * @param count 전체 데이터 개수
     * @param width 차트 플롯 영역 너비 (px)
     */
    fun xForIndex(index: Int, count: Int, width: Float): Float {
        val denominator = (count - 1).coerceAtLeast(1)
        return width * index / denominator
    }

    /**
     * 공포지수 점수(0..100)를 Y 픽셀로 변환 (상하 반전: score 0 = 바닥, 100 = 천장).
     *
     * @param score 공포지수 점수
     * @param height 차트 플롯 영역 높이 (px)
     */
    fun yForScore(score: Double, height: Float): Float {
        return height * (1f - score.toFloat() / 100f)
    }
}
