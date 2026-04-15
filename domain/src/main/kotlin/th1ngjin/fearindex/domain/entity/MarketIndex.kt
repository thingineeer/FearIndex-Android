package th1ngjin.fearindex.domain.entity

data class MarketIndex(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
    val isPositive: Boolean,
)
