package th1ngjin.fearindex.domain.entity

/**
 * 지표 계산용 시계열 + 출처 메타데이터 — iOS `AssetShortRatioSeries` 대칭.
 * 데이터 부족/미지원이면 빈 시계열 (메타데이터는 있을 수 있음).
 */

data class AssetCloseSeries(
    /** 시간순(오래된→최신) 일봉 종가. */
    val closes: List<Double>,
    val sourceMetadata: IndicatorSourceMetadata? = null,
) {
    companion object {
        val EMPTY = AssetCloseSeries(closes = emptyList())
    }
}

data class AssetShortRatioSeries(
    /** 시간순(오래된→최신) 공매도 비중(%). */
    val ratios: List<Double>,
    val sourceMetadata: IndicatorSourceMetadata? = null,
) {
    companion object {
        val EMPTY = AssetShortRatioSeries(ratios = emptyList())
    }
}
