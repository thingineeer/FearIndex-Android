package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.data.datasource.MarketIndexDataSource
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.domain.entity.MarketIndex
import th1ngjin.fearindex.domain.repository.MarketIndexRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketIndexRepositoryImpl @Inject constructor(
    private val dataSource: MarketIndexDataSource,
) : MarketIndexRepository {

    private val symbolNameMap = mapOf(
        "^KS11" to "KOSPI",
        "^KQ11" to "KOSDAQ",
        "^IXIC" to "Nasdaq",
        "^GSPC" to "S&P 500",
        "^DJI" to "Dow Jones",
    )

    override suspend fun getMarketIndices(): List<MarketIndex> {
        if (ScreenshotMode.isEnabled()) return ScreenshotFixtures.marketIndices()

        val response = dataSource.fetch()
        val results = response.spark?.result ?: return emptyList()

        return results.mapNotNull { sparkResult ->
            val meta = sparkResult.response.firstOrNull()?.meta ?: return@mapNotNull null
            val price = meta.regularMarketPrice ?: return@mapNotNull null
            val previousClose = meta.previousClose ?: return@mapNotNull null

            val change = price - previousClose
            val changePercent = if (previousClose != 0.0) {
                (change / previousClose) * 100.0
            } else {
                0.0
            }

            MarketIndex(
                symbol = sparkResult.symbol,
                name = symbolNameMap[sparkResult.symbol] ?: sparkResult.symbol,
                price = price,
                change = change,
                changePercent = changePercent,
            )
        }
    }
}
