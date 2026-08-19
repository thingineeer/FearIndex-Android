package th1ngjin.fearindex.domain.util

import th1ngjin.fearindex.domain.entity.NotificationRecord
import java.time.Duration
import java.time.Instant

/**
 * 알림 내역 보관 정책 (순수 로직, iOS `NotificationHistoryPolicy` 1:1 + Android [upsert]).
 *
 * - 무료 30일 / 프리미엄(광고 제거 IAP) 무제한 — 클라 저장이라 원가 0, 상품 차별화 목적
 * - 하드캡 5,000건: 파일 비대 방어 (프리미엄 포함, ≈1.5MB 상한)
 */
object NotificationHistoryPolicy {
    const val FREE_RETENTION_DAYS = 30
    const val HARD_CAP = 5000

    /** fallback id 레코드가 같은 알림의 실제 message id 레코드로 승격될 수 있는 수신 시각 차 상한 */
    private val FALLBACK_UPGRADE_WINDOW: Duration = Duration.ofSeconds(120)

    /** 보관 일수 — 프리미엄은 null(무제한) */
    fun retentionDays(isPremium: Boolean): Int? = if (isPremium) null else FREE_RETENTION_DAYS

    /**
     * **표시용** 보관 정책: 최신순 정렬 → 기간 초과 숨김(무료) → 하드캡.
     *
     * 기간 필터는 **화면에서 가리기만 할 뿐 저장소에서 지우지 않는다**. 두 가지 이유:
     * 1. 잠금 카피("30일 이전 내역은 프리미엄에서")가 성립하려면 프리미엄 구매 즉시 과거 내역이
     *    다시 보여야 한다 — 물리 삭제였다면 결제해도 복원 불가라 과약속이 된다.
     * 2. `now` 는 기기 시계다. 시계가 미래로 튀면 정상 레코드가 기간 초과로 오판되는데,
     *    삭제였다면 복구 불가. 숨김이면 시계 복귀 시 자동 복원된다.
     */
    fun prune(records: List<NotificationRecord>, isPremium: Boolean, now: Instant): List<NotificationRecord> {
        val sorted = records.sortedByDescending { it.receivedAt }
        val days = retentionDays(isPremium) ?: return sorted.take(HARD_CAP)
        val cutoff = now.minus(Duration.ofDays(days.toLong()))
        return sorted.filter { !it.receivedAt.isBefore(cutoff) }.take(HARD_CAP)
    }

    /**
     * **영속용** 보관 정책: 최신순 정렬 → 하드캡만 적용.
     *
     * 저장소에서 실제로 지우는 건 **시계와 무관한 하드캡 초과분뿐**이다(파일 비대 방어).
     * 기간 필터는 [prune] 의 표시 단계에서만 적용한다 — 위 주석의 두 이유 참조.
     */
    fun persistablePrune(records: List<NotificationRecord>): List<NotificationRecord> =
        records.sortedByDescending { it.receivedAt }.take(HARD_CAP)

    /** 병합 + id dedup(base 우선) + 최신순 정렬 */
    fun merge(base: List<NotificationRecord>, incoming: List<NotificationRecord>): List<NotificationRecord> {
        val byId = LinkedHashMap<String, NotificationRecord>()
        incoming.forEach { byId[it.id] = it }
        base.forEach { byId[it.id] = it }
        return byId.values.sortedByDescending { it.receivedAt }
    }

    /**
     * 1건 반영 (Android 전용 규칙):
     * - 같은 id 가 이미 있으면 입력 리스트 그대로 (dedup, 변경 없음)
     * - incoming 이 실제 message id 이고, 같은 제목·본문의 fallback id 레코드가 120초 이내에 있으면 그것을 교체(승격)
     * - 그 외 추가. 결과는 최신순.
     */
    fun upsert(records: List<NotificationRecord>, incoming: NotificationRecord): List<NotificationRecord> {
        if (records.any { it.id == incoming.id }) return records
        val twinIndex = if (incoming.hasFallbackId) -1 else records.indexOfFirst { it.isFallbackTwinOf(incoming) }
        val updated = if (twinIndex >= 0) {
            records.toMutableList().also { it[twinIndex] = incoming }
        } else {
            records + incoming
        }
        return updated.sortedByDescending { it.receivedAt }
    }

    /** 미확인 알림 존재 여부 — 홈 🔔 빨간 점 배지용 */
    fun hasUnread(records: List<NotificationRecord>, lastSeenAt: Instant?): Boolean =
        unreadCount(records, lastSeenAt) > 0

    /** 마지막 확인 이후 수신 건수 (`lastSeenAt` null 이면 전체) */
    fun unreadCount(records: List<NotificationRecord>, lastSeenAt: Instant?): Int =
        if (lastSeenAt == null) records.size else records.count { it.receivedAt.isAfter(lastSeenAt) }

    private fun NotificationRecord.isFallbackTwinOf(incoming: NotificationRecord): Boolean =
        hasFallbackId &&
            title == incoming.title &&
            body == incoming.body &&
            Duration.between(receivedAt, incoming.receivedAt).abs() <= FALLBACK_UPGRADE_WINDOW
}
