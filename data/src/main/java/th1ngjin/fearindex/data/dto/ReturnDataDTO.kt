package th1ngjin.fearindex.data.dto

import th1ngjin.fearindex.domain.entity.DateRange
import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.HistoricalSampleCounts
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
 * 스키마 (iOS v1.7.9 + v1.9.4 확장):
 * ```
 * returnData/{indexType}
 * ├── version: number
 * ├── updatedAt: number (Unix seconds)
 * ├── dataPoints: [101]
 * │   └── { score, returns{1M,3M,6M,1Y}, worstCase, bestCase, sampleCount, horizonCounts?{1M,3M,6M,1Y} }
 * ├── historicalEvents: [N]
 * │   └── { id, date("YYYY-MM-DD"), score, descriptionKey, returnAfter? }
 * ├── sourceFngRange?: { from: "yyyy-MM-dd", to: "yyyy-MM-dd" }   (market/crypto)
 * └── sourceScoreRange?: { from, to }                              (kospi)
 * ```
 */
data class ReturnDataDTO(
    val version: Int,
    val updatedAt: Double,
    val dataPoints: List<ReturnDataPointDTO>,
    val historicalEvents: List<ReturnEventEntryDTO>,
    /** 집계 원천 점수 기간 — market/crypto (CNN F&G / alternative.me). 옵셔널 (레거시 문서 호환). */
    val sourceFngRange: SourceDateRangeDTO? = null,
    /** 집계 원천 점수 기간 — kospi (자체 V2 지수). 옵셔널. */
    val sourceScoreRange: SourceDateRangeDTO? = null,
) {
    fun toDomain(): ReturnDataTable = ReturnDataTable(
        version = version,
        updatedAt = Instant.ofEpochSecond(updatedAt.toLong()),
        dataPoints = dataPoints.map { it.toDomain() },
        historicalEvents = historicalEvents.map { it.toDomain() },
        sourceRange = (sourceFngRange ?: sourceScoreRange)?.toDomain(),
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
            return ReturnDataDTO(
                version, updatedAt, dataPoints, events,
                sourceFngRange = SourceDateRangeDTO.fromAny(raw["sourceFngRange"]),
                sourceScoreRange = SourceDateRangeDTO.fromAny(raw["sourceScoreRange"]),
            )
        }
    }
}

/**
 * `{ from: "yyyy-MM-dd", to: "yyyy-MM-dd" }` (Firestore `source*Range` 맵). iOS `SourceDateRangeDTO` 1:1.
 */
data class SourceDateRangeDTO(
    val from: String,
    val to: String,
) {
    /** 날짜 파싱 실패 또는 from > to 면 null (DateRange 의 start<=end 전제조건 보호). UTC 자정. */
    fun toDomain(): DateRange? {
        val start = ReturnDataDateParser.parse(from) ?: return null
        val end = ReturnDataDateParser.parse(to) ?: return null
        if (start.isAfter(end)) return null
        return DateRange(start = start, end = end)
    }

    companion object {
        fun fromAny(raw: Any?): SourceDateRangeDTO? {
            val map = raw as? Map<*, *> ?: return null
            val from = map["from"] as? String ?: return null
            val to = map["to"] as? String ?: return null
            return SourceDateRangeDTO(from, to)
        }
    }
}

/** `yyyy-MM-dd` → UTC 자정 Instant. 이벤트 날짜 / source range 공용. 형식 오류 시 null. */
internal object ReturnDataDateParser {
    fun parse(text: String): Instant? = runCatching {
        LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant()
    }.getOrNull()
}

data class ReturnDataPointDTO(
    val score: Int,
    val returns: HistoricalReturnsDTO,
    val worstCase: HistoricalReturnsDTO,
    val bestCase: HistoricalReturnsDTO,
    val sampleCount: Int,
    /** horizon 별 표본 수 (v1.9.4 서버 `horizonCounts`). 없으면 sampleCount 를 모든 horizon 에 적용. */
    val horizonCounts: HistoricalSampleCountsDTO? = null,
) {
    fun toDomain(): ReturnDataPoint = ReturnDataPoint(
        score = score,
        returns = returns.toDomain(),
        worstCase = worstCase.toDomain(),
        bestCase = bestCase.toDomain(),
        sampleCount = sampleCount,
        horizonSampleCounts = horizonCounts?.toDomain() ?: HistoricalSampleCounts.same(sampleCount),
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
            @Suppress("UNCHECKED_CAST")
            val counts = (raw["horizonCounts"] as? Map<String, Any?>)
                ?.let { HistoricalSampleCountsDTO.fromMap(it) }
            return ReturnDataPointDTO(score, returns, worst, best, samples, counts)
        }
    }
}

/** iOS `HistoricalSampleCountsDTO` 1:1 — 필드 하나라도 없으면 null (fallback 유도). */
data class HistoricalSampleCountsDTO(
    val oneMonth: Int,
    val threeMonth: Int,
    val sixMonth: Int,
    val oneYear: Int,
) {
    fun toDomain(): HistoricalSampleCounts = HistoricalSampleCounts(
        oneMonth = oneMonth,
        threeMonth = threeMonth,
        sixMonth = sixMonth,
        oneYear = oneYear,
    )

    companion object {
        fun fromMap(raw: Map<String, Any?>): HistoricalSampleCountsDTO? {
            val oneMonth = (raw["oneMonth"] as? Number)?.toInt() ?: return null
            val threeMonth = (raw["threeMonth"] as? Number)?.toInt() ?: return null
            val sixMonth = (raw["sixMonth"] as? Number)?.toInt() ?: return null
            val oneYear = (raw["oneYear"] as? Number)?.toInt() ?: return null
            return HistoricalSampleCountsDTO(oneMonth, threeMonth, sixMonth, oneYear)
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
        val instant = ReturnDataDateParser.parse(date) ?: Instant.EPOCH
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
