package th1ngjin.fearindex.domain.repository

import th1ngjin.fearindex.domain.entity.MarketIndex

interface MarketIndexRepository {
    suspend fun getMarketIndices(): List<MarketIndex>
}
