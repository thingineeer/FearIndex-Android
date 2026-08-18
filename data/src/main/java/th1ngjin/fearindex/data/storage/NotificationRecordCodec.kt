package th1ngjin.fearindex.data.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import th1ngjin.fearindex.domain.entity.NotificationKind
import th1ngjin.fearindex.domain.entity.NotificationRecord
import java.time.Instant

/** JSONL 한 줄의 스키마 — iOS `NotificationRecord` Codable 과 동일 필드명 (kind=rawValue, receivedAt=epoch 초). */
@Serializable
data class NotificationRecordJson(
    val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val score: Int? = null,
    val receivedAt: Long,
)

/** [NotificationRecord] ↔ JSONL 라인. 손상 라인은 null 로 돌려 한 줄 오류가 전체 내역을 막지 않게 한다. */
object NotificationRecordCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun encode(record: NotificationRecord): String = json.encodeToString(record.toJson())

    fun decode(line: String): NotificationRecord? =
        runCatching { json.decodeFromString<NotificationRecordJson>(line) }
            .getOrNull()
            ?.toRecord()

    private fun NotificationRecord.toJson() = NotificationRecordJson(
        id = id,
        kind = kind.storageValue,
        title = title,
        body = body,
        score = score,
        receivedAt = receivedAt.epochSecond,
    )

    private fun NotificationRecordJson.toRecord() = NotificationRecord(
        id = id,
        kind = NotificationKind.fromStorage(kind),
        title = title,
        body = body,
        score = score,
        receivedAt = Instant.ofEpochSecond(receivedAt),
    )
}
