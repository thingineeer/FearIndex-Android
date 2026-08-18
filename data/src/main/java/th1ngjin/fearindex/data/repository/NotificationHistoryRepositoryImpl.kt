package th1ngjin.fearindex.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import th1ngjin.fearindex.data.storage.JsonlFileStore
import th1ngjin.fearindex.data.storage.NotificationRecordCodec
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.domain.repository.NotificationHistoryRepository
import th1ngjin.fearindex.domain.util.NotificationHistoryPolicy
import timber.log.Timber
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** 내역 화면 마지막 확인 시각 저장소 — SharedPreferences 를 JVM 테스트에서 떼어내기 위한 얇은 인터페이스. */
interface NotificationHistoryLastSeenStore {
    fun get(): Instant?
    fun set(instant: Instant)
}

class SharedPreferencesLastSeenStore(
    private val prefs: SharedPreferences,
) : NotificationHistoryLastSeenStore {

    override fun get(): Instant? =
        if (prefs.contains(KEY_LAST_SEEN)) Instant.ofEpochSecond(prefs.getLong(KEY_LAST_SEEN, 0L)) else null

    override fun set(instant: Instant) {
        prefs.edit().putLong(KEY_LAST_SEEN, instant.epochSecond).apply()
    }

    companion object {
        const val PREFS_NAME = "notification_history_prefs"
        const val KEY_LAST_SEEN = "last_seen_at_epoch_seconds"
    }
}

/**
 * 알림 내역 저장소 — 앱 내부 저장소 JSONL 파일 1개 + SharedPreferences(lastSeenAt). 서버 통신 없음(서버비 0).
 * iOS `NotificationHistoryRepository` 대응 (Android 는 NSE 가 없어 spool 파일 없음).
 *
 * append 는 read → [NotificationHistoryPolicy.upsert] → 변경 시 전체 rewrite. 파일이 작아(하드캡 5,000)
 * 단순·일관성을 우선했고, FCM 서비스/포그라운드 동기화의 동시 append 는 [writeMutex] 로 직렬화한다.
 */
@Singleton
class NotificationHistoryRepositoryImpl internal constructor(
    private val store: JsonlFileStore,
    private val lastSeenStore: NotificationHistoryLastSeenStore,
) : NotificationHistoryRepository {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        store = JsonlFileStore(File(context.filesDir, FILE_PATH)),
        lastSeenStore = SharedPreferencesLastSeenStore(
            context.getSharedPreferences(SharedPreferencesLastSeenStore.PREFS_NAME, Context.MODE_PRIVATE),
        ),
    )

    private val writeMutex = Mutex()

    private val _updates = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val updates: Flow<Unit> = _updates.asSharedFlow()

    /** 파일의 raw 순서와 무관하게 id dedup + 최신순으로 돌려준다 (손상 라인은 skip). */
    override suspend fun fetchAll(): List<NotificationRecord> =
        NotificationHistoryPolicy.merge(readRecords(), emptyList())

    override suspend fun append(record: NotificationRecord) {
        val changed = writeMutex.withLock {
            val existing = readRecords()
            val updated = NotificationHistoryPolicy.upsert(existing, record)
            if (updated == existing) false else persist(updated)
        }
        if (changed) _updates.tryEmit(Unit)
    }

    override suspend fun replaceAll(records: List<NotificationRecord>) {
        writeMutex.withLock { persist(records) }
        _updates.tryEmit(Unit)
    }

    override suspend fun lastSeenAt(): Instant? = lastSeenStore.get()

    override suspend fun setLastSeenAt(instant: Instant) {
        lastSeenStore.set(instant)
        _updates.tryEmit(Unit)
    }

    private suspend fun readRecords(): List<NotificationRecord> =
        runCatching { store.readAllLines().mapNotNull(NotificationRecordCodec::decode) }
            .onFailure { Timber.w(it, "[NotificationHistory] read 실패") }
            .getOrDefault(emptyList())

    /** 전체 재기록. 성공 여부를 돌려준다. */
    private suspend fun persist(records: List<NotificationRecord>): Boolean =
        runCatching { store.rewrite(records.map(NotificationRecordCodec::encode)) }
            .onFailure { Timber.w(it, "[NotificationHistory] rewrite 실패") }
            .isSuccess

    companion object {
        const val FILE_PATH = "notification_history/notification-history.jsonl"
    }
}
