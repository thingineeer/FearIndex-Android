package th1ngjin.fearindex.domain.entity

import java.time.Instant

data class ReturnDataTable(
    val version: Int,
    val updatedAt: Instant,
    val dataPoints: List<ReturnDataPoint>,
    val historicalEvents: List<ReturnEventEntry>,
    /**
     * 집계에 사용된 원천 점수 데이터의 기간 (v1.9.4, ⓘ "데이터 기간" 표시용).
     * 서버 `sourceFngRange`(market/crypto) / `sourceScoreRange`(kospi) 에서 매핑. 없으면 null (숨김).
     */
    val sourceRange: DateRange? = null,
)
