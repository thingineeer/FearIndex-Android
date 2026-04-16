package th1ngjin.fearindex.data.dto

import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.ReturnDataPoint
import th1ngjin.fearindex.domain.entity.ReturnDataTable
import th1ngjin.fearindex.domain.entity.ReturnEventEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Firestore `returnData/{market|crypto}` 문서 DTO.
 *
 * iOS `ReturnDataDTO`와 1:1 대응. Firestore SDK는 스키마 검증 없이 Map을 돌려주므로
 * DTO에서 안전하게 파싱하고 Entity로 변환해야 함.
 *
 * 스키마 (iOS v1.7.9):
 * ```
 * returnData/{indexType}
 * ├── version: number
 * ├── updatedAt: number (Unix seconds)
 * ├── dataPoints: [101]
 * │   └── { score, returns{1M,3M,6M,1Y}, worstCase, bestCase, sampleCount }
 * └── historicalEvents: [N]
 *     └── { id, date("YYYY-MM-DD"), score, descriptionKey, returnAfter? }
 * ```
 */
data class ReturnDataDTO(
    val version: Int,
    val updatedAt: Double,
    val dataPoints: List<ReturnDataPointDTO>,
    val historicalEvents: List<ReturnEventEntryDTO>,
) {
    fun toDomain(): ReturnDataTable = ReturnDataTable(
        version = version,
        updatedAt = Instant.ofEpochSecond(updatedAt.toLong()),
        dataPoints = dataPoints.map { it.toDomain() },
        historicalEvents = historicalEvents.map { it.toDomain() },
    )

    companion object {
        /**
         * Firestore `DocumentSnapshot.data`(Map<String, Any?>)에서 DTO 복원.
         * 필드 누락 시 null 반환 (fallback 유도).
         */
        fun fromMap(raw: Map<String, Any?>): ReturnDataDTO? {
            val version = (raw["version"] as? Number)?.toInt() ?: return null
            val updatedAt = (raw["updatedAt"] as? Number)?.toDouble() ?: return null
            val dataPointsRaw = raw["dataPoints"] as? List<*> ?: return null
            val eventsRaw = raw["historicalEvents"] as? List<*> ?: return null

            val dataPoints = dataPointsRaw.mapNotNull { entry ->
                @Suppress("UNCHECKED_CAST")
                (entry as? Map<String, Any?>)?.let { ReturnDataPointDTO.fromMap(it) }
            }
            if (dataPoints.size != 101) return null // iOS 스펙: 반드시 101개

            val events = eventsRaw.mapNotNull { entry ->
                @Suppress("UNCHECKED_CAST")
                (entry as? Map<String, Any?>)?.let { ReturnEventEntryDTO.fromMap(it) }
            }
            return ReturnDataDTO(version, updatedAt, dataPoints, events)
        }
    }
}

data class ReturnDataPointDTO(
    val score: Int,
    val returns: HistoricalReturnsDTO,
    val worstCase: HistoricalReturnsDTO,
    val bestCase: HistoricalReturnsDTO,
    val sampleCount: Int,
) {
    fun toDomain(): ReturnDataPoint = ReturnDataPoint(
        score = score,
        returns = returns.toDomain(),
        worstCase = worstCase.toDomain(),
        bestCase = bestCase.toDomain(),
        sampleCount = sampleCount,
    )

    companion object {
        fun fromMap(raw: Map<String, Any?>): ReturnDataPointDTO? {
            val score = (raw["score"] as? Number)?.toInt() ?: return null
            @Suppress("UNCHECKED_CAST")
            val returns = (raw["returns"] as? Map<String, Any?>)
                ?.let { HistoricalReturnsDTO.fromMap(it) } ?: return null
            @Suppress("UNCHECKED_CAST")
            val worst = (raw["worstCase"] as? Map<String, Any?>)
                ?.let { HistoricalReturnsDTO.fromMap(it) } ?: return null
            @Suppress("UNCHECKED_CAST")
            val best = (raw["bestCase"] as? Map<String, Any?>)
                ?.let { HistoricalReturnsDTO.fromMap(it) } ?: return null
            val samples = (raw["sampleCount"] as? Number)?.toInt() ?: 0
            return ReturnDataPointDTO(score, returns, worst, best, samples)
        }
    }
}

data class HistoricalReturnsDTO(
    val oneMonth: Double,
    val threeMonth: Double,
    val sixMonth: Double,
    val oneYear: Double,
) {
    fun toDomain(): HistoricalReturns = HistoricalReturns(
        oneMonth = oneMonth,
        threeMonth = threeMonth,
        sixMonth = sixMonth,
        oneYear = oneYear,
    )

    companion object {
        fun fromMap(raw: Map<String, Any?>): HistoricalReturnsDTO? {
            val oneMonth = (raw["oneMonth"] as? Number)?.toDouble() ?: return null
            val threeMonth = (raw["threeMonth"] as? Number)?.toDouble() ?: return null
            val sixMonth = (raw["sixMonth"] as? Number)?.toDouble() ?: return null
            val oneYear = (raw["oneYear"] as? Number)?.toDouble() ?: return null
            return HistoricalReturnsDTO(oneMonth, threeMonth, sixMonth, oneYear)
        }
    }
}

data class ReturnEventEntryDTO(
    val id: String,
    val date: String, // "YYYY-MM-DD"
    val score: Int,
    val descriptionKey: String,
    val returnAfter: HistoricalReturnsDTO?,
) {
    fun toDomain(): ReturnEventEntry {
        val instant = runCatching {
            LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant()
        }.getOrElse { Instant.EPOCH }
        return ReturnEventEntry(
            id = id,
            date = instant,
            score = score,
            descriptionKey = descriptionKey,
            returnAfter = returnAfter?.toDomain(),
        )
    }

    companion object {
        fun fromMap(raw: Map<String, Any?>): ReturnEventEntryDTO? {
            val id = raw["id"] as? String ?: return null
            val date = raw["date"] as? String ?: return null
            val score = (raw["score"] as? Number)?.toInt() ?: return null
            val descKey = raw["descriptionKey"] as? String ?: return null
            @Suppress("UNCHECKED_CAST")
            val after = (raw["returnAfter"] as? Map<String, Any?>)
                ?.let { HistoricalReturnsDTO.fromMap(it) }
            return ReturnEventEntryDTO(id, date, score, descKey, after)
        }
    }
}
