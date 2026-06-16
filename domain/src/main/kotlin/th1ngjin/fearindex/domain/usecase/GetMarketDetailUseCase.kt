package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.CryptoPrice
import th1ngjin.fearindex.domain.entity.ExchangeRateQuote
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.repository.MarketDetailRepository
import javax.inject.Inject

/** 시장 상세 — 지수/암호화폐/환율 조회 UseCase 묶음. */
class GetMarketIndicesDetailUseCase @Inject constructor(
    private val repository: MarketDetailRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): List<MarketIndex> =
        repository.getIndices(forceRefresh)
}

class GetCryptoPricesUseCase @Inject constructor(
    private val repository: MarketDetailRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): List<CryptoPrice> =
        repository.getCryptoPrices(forceRefresh)
}

class GetUsdKrwRateUseCase @Inject constructor(
    private val repository: MarketDetailRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): ExchangeRateQuote? =
        repository.getUsdKrwRate(forceRefresh)
}
