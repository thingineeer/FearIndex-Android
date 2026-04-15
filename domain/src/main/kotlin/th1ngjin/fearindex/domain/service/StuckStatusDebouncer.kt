package th1ngjin.fearindex.domain.service

import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckStatus

/**
 * 물림 상태 서버 반영 디바운서 (도메인 인터페이스).
 *
 * Presentation은 이 추상화에만 의존하고, 구현체는 Data 레이어에 위치한다.
 */
interface StuckStatusDebouncer {

    sealed interface Outcome {
        data class Success(val indexType: FearIndexType) : Outcome
        data class Failure(val indexType: FearIndexType, val error: Throwable) : Outcome
    }

    /** 새 상태를 예약한다. 기존 타이머가 있으면 취소하고 재시작. */
    fun schedule(indexType: FearIndexType, status: StuckStatus)

    /** 모든 pending 상태를 즉시 서버로 보낸다. */
    suspend fun flushPending()

    /** 앱 시작 시 실패했던 pending 상태를 재시도. */
    suspend fun retryPendingIfNeeded()

    fun setOutcomeListener(listener: ((Outcome) -> Unit)?)
}
