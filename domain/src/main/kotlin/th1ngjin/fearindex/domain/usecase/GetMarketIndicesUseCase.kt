package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.repository.MarketIndexRepository

class GetMarketIndicesUseCase(private val repository: MarketIndexRepository) {
    suspend operator fun invoke(): List<MarketIndex> = repository.getMarketIndices()
}
