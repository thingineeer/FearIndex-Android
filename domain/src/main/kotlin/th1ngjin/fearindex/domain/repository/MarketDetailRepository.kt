package th1ngjin.fearindex.domain.repository

import th1ngjin.fearindex.domain.entity.CryptoPrice
import th1ngjin.fearindex.domain.entity.ExchangeRateQuote
import th1ngjin.fearindex.domain.entity.MarketIndex

/**
 * 시장 상세 화면 데이터. iOS 의 MarketIndex/CryptoPrice/ExchangeRate 파이프라인 통합.
 * - 지수: Yahoo(글로벌) + Naver(한국)
 * - 암호화폐: CoinGecko
 * - 환율: currency-api (USD/KRW)
 */
interface MarketDetailRepository {
    /** 지수 탭 (글로벌 7 + 한국 2). 부분 실패 허용(실패 심볼 스킵). */
    suspend fun getIndices(forceRefresh: Boolean = false): List<MarketIndex>

    /** 암호화폐 탭 (BTC/ETH/XRP/SOL/BNB). */
    suspend fun getCryptoPrices(forceRefresh: Boolean = false): List<CryptoPrice>

    /** 환율 탭 — USD/KRW. */
    suspend fun getUsdKrwRate(forceRefresh: Boolean = false): ExchangeRateQuote?
}
