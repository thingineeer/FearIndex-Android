package th1ngjin.fearindex.domain.entity

import java.time.Instant
import kotlin.math.roundToInt

/**
 * FCM 수신 payload → [NotificationRecord] 순수 매핑 (iOS `NotificationRecord+Payload` 1:1).
 *
 * - `data.type` 을 [NotificationKind] 로 정규화 (미지 type 도 [NotificationKind.OTHER] 로 기록)
 * - dedup id: `RemoteMessage.messageId` → `google.message_id` → `gcm.message_id` → 파생 키
 * - 제목·본문이 모두 비어 있으면(silent push) null — 보여줄 내용이 없다
 */
object NotificationRecordMapper {

    fun fromPush(
        data: Map<String, String>,
        messageId: String?,
        title: String?,
        body: String?,
        receivedAt: Instant,
    ): NotificationRecord? {
        val trimmedTitle = title.orEmpty().trim()
        val trimmedBody = body.orEmpty().trim()
        if (trimmedTitle.isEmpty() && trimmedBody.isEmpty()) return null
        val kind = kindFromType(data["type"])
        return NotificationRecord(
            id = resolveId(data, messageId, kind, receivedAt),
            kind = kind,
            title = trimmedTitle,
            body = trimmedBody,
            score = parseScore(data["score"]),
            receivedAt = receivedAt,
        )
    }

    /** 서버 `data.type` → 채널. 접두사 기반이라 `kospi_*`/`crypto_*`/`weekly_*` 신규 타입도 흡수한다. */
    fun kindFromType(type: String?): NotificationKind = when {
        type == null -> NotificationKind.OTHER
        type == "fear_index_alert" || type.startsWith("global_") -> NotificationKind.MARKET
        type.startsWith("kospi_") -> NotificationKind.KOSPI
        type.startsWith("crypto_") -> NotificationKind.CRYPTO
        type.startsWith("weekly_") -> NotificationKind.WEEKLY
        else -> NotificationKind.OTHER
    }

    private fun resolveId(
        data: Map<String, String>,
        messageId: String?,
        kind: NotificationKind,
        receivedAt: Instant,
    ): String =
        messageId.nonBlank()
            ?: data["google.message_id"].nonBlank()
            ?: data["gcm.message_id"].nonBlank()
            ?: NotificationRecord.fallbackId(kind, receivedAt)

    /** FCM data 는 문자열 — "25"/"25.4" 모두 허용, 파싱 불가/NaN 은 null */
    private fun parseScore(raw: String?): Int? =
        raw?.trim()?.toDoubleOrNull()?.takeIf { it.isFinite() }?.roundToInt()

    private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
