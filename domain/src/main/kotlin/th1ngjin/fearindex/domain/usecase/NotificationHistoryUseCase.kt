package th1ngjin.fearindex.domain.usecase

import kotlinx.coroutines.flow.Flow
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.domain.repository.NotificationHistoryRepository
import th1ngjin.fearindex.domain.util.NotificationHistoryPolicy
import java.time.Instant

/** 알림 내역 조회/기록/확인 UseCase (iOS `NotificationHistoryUseCase` 1:1) — Presentation 은 이것만 의존. */
class NotificationHistoryUseCase(
    private val repository: NotificationHistoryRepository,
    private val now: () -> Instant = Instant::now,
) {
    /** 저장소 변경 스트림 (홈 🔔 배지·내역 화면 갱신) */
    val updates: Flow<Unit>
        get() = repository.updates

    /**
     * 보관 정책(무료 30일 / 프리미엄 무제한, 하드캡) 적용된 최신순 내역.
     * prune 은 제거만 하므로 건수가 줄었을 때만 저장소를 다시 쓴다.
     */
    suspend fun fetch(isPremium: Boolean): List<NotificationRecord> {
        val all = repository.fetchAll()
        val pruned = NotificationHistoryPolicy.prune(all, isPremium, now())
        if (pruned.size != all.size) repository.replaceAll(pruned)
        return pruned
    }

    /** 수신 알림 기록 (저장소가 id dedup / fallback 승격) */
    suspend fun record(record: NotificationRecord) = repository.append(record)

    /** 내역 화면 확인 처리 (홈 🔔 배지 해제) */
    suspend fun markSeen(now: Instant = this.now()) = repository.setLastSeenAt(now)

    /** 미확인 알림 존재 여부 (보관 기간 안의 기록만 대상) */
    suspend fun hasUnread(isPremium: Boolean): Boolean = unreadCount(isPremium) > 0

    /** 보관 기간 안의 미확인 건수 */
    suspend fun unreadCount(isPremium: Boolean): Int {
        val visible = NotificationHistoryPolicy.prune(repository.fetchAll(), isPremium, now())
        return NotificationHistoryPolicy.unreadCount(visible, repository.lastSeenAt())
    }
}
