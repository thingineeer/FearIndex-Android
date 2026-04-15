package th1ngjin.fearindex.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CNNFearGreedResponse(
    @SerialName("fear_and_greed") val fearAndGreed: FearAndGreedDTO,
    @SerialName("fear_and_greed_historical") val fearAndGreedHistorical: FearAndGreedHistoricalDTO,
)

@Serializable
data class FearAndGreedDTO(
    val score: Double,
    val rating: String,
    val timestamp: String,
    @SerialName("previous_close") val previousClose: Double,
    @SerialName("previous_1_week") val previous1Week: Double,
    @SerialName("previous_1_month") val previous1Month: Double,
    @SerialName("previous_1_year") val previous1Year: Double,
)

@Serializable
data class FearAndGreedHistoricalDTO(
    val timestamp: Double? = null,
    val score: Double? = null,
    val rating: String? = null,
    val data: List<HistoricalDataPointDTO> = emptyList(),
)

@Serializable
data class HistoricalDataPointDTO(
    val x: Double,
    val y: Double,
    val rating: String,
)
