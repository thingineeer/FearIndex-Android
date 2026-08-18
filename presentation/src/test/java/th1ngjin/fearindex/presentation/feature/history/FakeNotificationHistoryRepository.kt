package th1ngjin.fearindex.presentation.feature.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.domain.repository.NotificationHistoryRepository
import java.time.Instant

/** 인메모리 알림 내역 저장소 (ViewModel 테스트용). */
class FakeNotificationHistoryRepository(
    initial: List<NotificationRecord> = emptyList(),
) : NotificationHistoryRepository {
    private val records = initial.toMutableList()
    var lastSeen: Instant? = null
        private set
    var setLastSeenCount = 0
        private set
    private val _updates = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    override val updates: Flow<Unit> = _updates

    override suspend fun fetchAll(): List<NotificationRecord> = records.toList()

    override suspend fun append(record: NotificationRecord) {
        if (records.none { it.id == record.id }) records.add(0, record)
        _updates.tryEmit(Unit)
    }

    override suspend fun replaceAll(records: List<NotificationRecord>) {
        this.records.clear()
        this.records.addAll(records)
    }

    override suspend fun lastSeenAt(): Instant? = lastSeen

    override suspend fun setLastSeenAt(instant: Instant) {
        lastSeen = instant
        setLastSeenCount++
        _updates.tryEmit(Unit)
    }
}
