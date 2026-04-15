package th1ngjin.fearindex.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CryptoFearIndexResponse(
    val name: String,
    val data: List<CryptoFearIndexDTO>,
)

@Serializable
data class CryptoFearIndexDTO(
    val value: String,
    @SerialName("value_classification") val valueClassification: String,
    val timestamp: String,
    @SerialName("time_until_update") val timeUntilUpdate: String? = null,
)
