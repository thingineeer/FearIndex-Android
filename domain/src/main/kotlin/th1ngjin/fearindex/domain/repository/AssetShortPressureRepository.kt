package th1ngjin.fearindex.domain.repository

import th1ngjin.fearindex.domain.entity.AssetShortRatioSeries
import th1ngjin.fearindex.domain.entity.FearIndexType

/**
 * 자산 일별 공매도 비중(%) 시계열 — iOS `AssetShortPressureRepositoryProtocol` 대칭.
 * MARKET=FINRA(SPY), CRYPTO=Binance(순숏 계정 비율), KOSPI=서버(/api/kospi/short).
 */
interface AssetShortPressureRepository {
    /** 시간순(오래된→최신) 공매도 비중(%) + 출처 메타데이터. 미지원/부족이면 빈 시계열. */
    suspend fun dailyShortRatios(type: FearIndexType): AssetShortRatioSeries
}
