package th1ngjin.fearindex.data.service

import th1ngjin.fearindex.data.storage.StuckCounterStorage
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.repository.StuckCounterRepository
import th1ngjin.fearindex.domain.service.StuckStatusDebouncer as StuckStatusDebouncerProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 물림 상태 서버 반영 디바운서.
 *
 * - 사용자가 토글을 연타해도 마지막 상태만 서버에 반영
 * - 5초 idle 후 서버 호출 (새 탭이 들어오면 타이머 재시작)
 * - 앱 백그라운드/종료 시점에는 즉시 flush 가능
 * - 네트워크 실패 시 SharedPreferences 재시도 큐에 저장, 다음 앱 실행 시 복구
 * - flush 결과는 outcomeListener 콜백으로 ViewModel에 전파
 */
@Singleton
class StuckStatusDebouncerImpl @Inject constructor(
    private val repository: StuckCounterRepository,
    private val storage: StuckCounterStorage,
) : StuckStatusDebouncerProtocol {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val tasks = mutableMapOf<FearIndexType, Job>()
    private val pending = mutableMapOf<FearIndexType, StuckStatus>()
    private var outcomeListener: ((StuckStatusDebouncerProtocol.Outcome) -> Unit)? = null

    var debounceMillis: Long = 5_000L

    override fun setOutcomeListener(listener: ((StuckStatusDebouncerProtocol.Outcome) -> Unit)?) {
        outcomeListener = listener
    }

    /**
     * 새 상태를 예약한다. 기존 타이머가 있으면 취소하고 재시작.
     */
    override fun schedule(indexType: FearIndexType, status: StuckStatus) {
        scope.launch {
            mutex.withLock {
                pending[indexType] = status
                tasks[indexType]?.cancel()
                tasks[indexType] = scope.launch {
                    delay(debounceMillis)
                    flushOne(indexType)
                }
            }
        }
    }

    /** 모든 pending 상태를 즉시 서버로 보낸다. */
    override suspend fun flushPending() {
        val keys: List<FearIndexType> = mutex.withLock { pending.keys.toList() }
        keys.forEach { flushOne(it) }
    }

    /** 앱 시작 시 실패했던 pending 상태를 재시도. */
    override suspend fun retryPendingIfNeeded() {
        FearIndexType.entries.forEach { indexType ->
            val status = storage.loadPendingRetry(indexType) ?: return@forEach
            try {
                repository.submitStuckStatus(indexType, status)
                storage.clearPendingRetry(indexType)
                Timber.i("[StuckStatusDebouncer] 재시도 성공: $indexType=$status")
                outcomeListener?.invoke(StuckStatusDebouncerProtocol.Outcome.Success(indexType))
            } catch (e: Exception) {
                Timber.e(e, "[StuckStatusDebouncer] 재시도 실패")
                outcomeListener?.invoke(StuckStatusDebouncerProtocol.Outcome.Failure(indexType, e))
                // 유지 — 다음 앱 시작 시 재시도
            }
        }
    }

    private suspend fun flushOne(indexType: FearIndexType) {
        val status = mutex.withLock {
            val s = pending.remove(indexType) ?: return
            tasks.remove(indexType)?.cancel()
            s
        }
        try {
            repository.submitStuckStatus(indexType, status)
            storage.clearPendingRetry(indexType)
            outcomeListener?.invoke(StuckStatusDebouncerProtocol.Outcome.Success(indexType))
        } catch (e: Exception) {
            Timber.e(e, "[StuckStatusDebouncer] flush 실패 — 재시도 큐 저장")
            storage.savePendingRetry(indexType, status)
            outcomeListener?.invoke(StuckStatusDebouncerProtocol.Outcome.Failure(indexType, e))
        }
    }
}
