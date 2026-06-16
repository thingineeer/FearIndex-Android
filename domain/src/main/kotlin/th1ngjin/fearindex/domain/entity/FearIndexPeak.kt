package th1ngjin.fearindex.domain.entity

import java.time.Instant

/**
 * 공포지수 차트의 고점(high)/저점(low) 마커를 나타내는 도메인 엔티티.
 *
 * CNN Fear & Greed API 는 peak 정보를 제공하지 않으므로
 * [ComputeFearIndexPeaks] UseCase 가 history 배열을 순회하여 생성한다.
 *
 * iOS `FearIndexPeak.swift` 와 1:1 대응.
 */
data class FearIndexPeak(
    /** high / low 구분. */
    val kind: Kind,
    /** 해당 시점의 공포지수 점수 (0.0..100.0). */
    val score: Double,
    /** 해당 시점의 타임스탬프. */
    val date: Instant,
    /**
     * 원본 history 배열 내 인덱스.
     * UI 가 차트 X 축 위치를 매핑할 때 사용한다.
     */
    val index: Int,
) {
    /** 피크 종류. */
    enum class Kind { HIGH, LOW }
}
