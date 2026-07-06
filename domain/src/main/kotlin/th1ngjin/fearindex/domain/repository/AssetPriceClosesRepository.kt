package th1ngjin.fearindex.domain.repository

import th1ngjin.fearindex.domain.entity.AssetCloseSeries
import th1ngjin.fearindex.domain.entity.FearIndexType

/**
 * 자산 일봉 종가 시계열(RSI 보조 근거용) — iOS `AssetPriceClosesRepositoryProtocol` 대칭.
 * MARKET=S&P500(Yahoo ^GSPC), CRYPTO=BTC(CoinGecko), KOSPI=서버 스냅샷 kospiClose.
 */
interface AssetPriceClosesRepository {
    /** 시간순(오래된→최신) 일봉 종가 + 출처 메타데이터. 미지원/부족이면 빈 시계열. */
    suspend fun dailyCloses(type: FearIndexType): AssetCloseSeries
}
