package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.data.datasource.MarketIndexDataSource
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.repository.MarketIndexRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketIndexRepositoryImpl @Inject constructor(
    private val dataSource: MarketIndexDataSource,
) : MarketIndexRepository {

    private val symbolNameMap = mapOf(
        "^KS11" to "코스피",
        "^KQ11" to "코스닥",
        "^IXIC" to "나스닥",
        "^GSPC" to "S&P 500",
        "^DJI" to "다우존스",
    )

    override suspend fun getMarketIndices(): List<MarketIndex> {
        val response = dataSource.fetch()
        val results = response.spark?.result ?: return emptyList()

        return results.mapNotNull { sparkResult ->
            val meta = sparkResult.response.firstOrNull()?.meta ?: return@mapNotNull null
            val price = meta.regularMarketPrice ?: return@mapNotNull null
            val previousClose = meta.previousClose ?: return@mapNotNull null

            val changePercent = if (previousClose != 0.0) {
                ((price - previousClose) / previousClose) * 100.0
            } else {
                0.0
            }

            MarketIndex(
                symbol = sparkResult.symbol,
                name = symbolNameMap[sparkResult.symbol] ?: sparkResult.symbol,
                price = price,
                changePercent = changePercent,
                isPositive = changePercent >= 0,
            )
        }
    }
}
