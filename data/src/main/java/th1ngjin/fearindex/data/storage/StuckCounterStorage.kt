package th1ngjin.fearindex.data.storage

import android.content.Context
import android.content.SharedPreferences
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.StuckStatus
import th1ngjin.fearindex.domain.service.DeviceIdProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 물림 카운터 관련 영속 저장소.
 *
 * - deviceId: UUID (서버 식별자)
 * - 사용자가 마지막으로 선택한 status (market/crypto 별)
 * - 네트워크 실패 시 재시도 큐 (market/crypto 별)
 */
@Singleton
class StuckCounterStorage @Inject constructor(
    @ApplicationContext context: Context,
) : DeviceIdProvider {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadDeviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    fun loadStatus(indexType: FearIndexType): StuckStatus {
        val raw = prefs.getString(statusKey(indexType), null) ?: return StuckStatus.NONE
        return runCatching { StuckStatus.valueOf(raw) }.getOrDefault(StuckStatus.NONE)
    }

    fun saveStatus(indexType: FearIndexType, status: StuckStatus) {
        prefs.edit().putString(statusKey(indexType), status.name).apply()
    }

    fun loadPendingRetry(indexType: FearIndexType): StuckStatus? {
        val raw = prefs.getString(retryKey(indexType), null) ?: return null
        return runCatching { StuckStatus.valueOf(raw) }.getOrNull()
    }

    fun savePendingRetry(indexType: FearIndexType, status: StuckStatus) {
        prefs.edit().putString(retryKey(indexType), status.name).apply()
    }

    fun clearPendingRetry(indexType: FearIndexType) {
        prefs.edit().remove(retryKey(indexType)).apply()
    }

    private fun statusKey(indexType: FearIndexType) =
        "stuckStatus_${indexType.name.lowercase()}"

    private fun retryKey(indexType: FearIndexType) =
        "stuckPendingRetry_${indexType.name.lowercase()}"

    companion object {
        private const val PREFS_NAME = "stuck_counter_prefs"
        private const val KEY_DEVICE_ID = "deviceId"
    }
}
