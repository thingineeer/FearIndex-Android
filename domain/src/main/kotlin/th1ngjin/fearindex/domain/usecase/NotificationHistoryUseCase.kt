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
     * 보관 정책 적용된 최신순 내역.
     *
     * **표시(무료 30일 숨김)와 영속(하드캡 초과분 삭제)을 분리한다.** 저장소에서 실제로 지우는 건
     * 시계와 무관한 하드캡 초과분뿐이라, ① 프리미엄 구매 즉시 30일 이전 내역이 복원되고
     * ② 기기 시계가 튀어도 레코드가 영구 소실되지 않는다. (iOS 와 동일 패턴)
     */
    suspend fun fetch(isPremium: Boolean): List<NotificationRecord> {
        val all = repository.fetchAll()
        val persistable = NotificationHistoryPolicy.persistablePrune(all)
        if (persistable.size != all.size) repository.replaceAll(persistable)
        return NotificationHistoryPolicy.prune(persistable, isPremium, now())
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
