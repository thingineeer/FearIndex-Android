package th1ngjin.fearindex.domain.repository

import kotlinx.coroutines.flow.Flow
import th1ngjin.fearindex.domain.entity.NotificationRecord
import java.time.Instant

/**
 * 알림 내역 저장소 인터페이스 — 구현은 Data(로컬 JSONL 파일 + SharedPreferences).
 * 서버 통신 없음(서버비 0). 보관 정책(prune)은 UseCase 가 적용하고 결과를 [replaceAll] 로 되돌려준다.
 */
interface NotificationHistoryRepository {
    /** 저장된 전체 내역 (id dedup 보장, 정렬은 구현에 따름) */
    suspend fun fetchAll(): List<NotificationRecord>

    /** 1건 기록. 같은 id 가 이미 있으면 무시(dedup), fallback id 는 실제 id 로 승격될 수 있다. */
    suspend fun append(record: NotificationRecord)

    /** 전체 교체 (UseCase 의 prune 결과 영속) */
    suspend fun replaceAll(records: List<NotificationRecord>)

    /** 내역 화면을 마지막으로 확인한 시각 */
    suspend fun lastSeenAt(): Instant?

    /** 내역 화면 확인 시각 기록 (미확인 배지 해제) */
    suspend fun setLastSeenAt(instant: Instant)

    /** append / replaceAll / setLastSeenAt 이후 발행 — 홈 배지·내역 화면 갱신 트리거 */
    val updates: Flow<Unit>
}
