package th1ngjin.fearindex.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YahooSparkResponse(
    val spark: SparkWrapper? = null,
)

@Serializable
data class SparkWrapper(
    val result: List<SparkResult> = emptyList(),
    val error: SparkError? = null,
)

@Serializable
data class SparkResult(
    val symbol: String,
    val response: List<SparkResponseItem> = emptyList(),
)

@Serializable
data class SparkResponseItem(
    val meta: SparkMeta? = null,
)

@Serializable
data class SparkMeta(
    val regularMarketPrice: Double? = null,
    val previousClose: Double? = null,
    val symbol: String? = null,
    val currency: String? = null,
)

@Serializable
data class SparkError(
    val code: String? = null,
    val description: String? = null,
)
