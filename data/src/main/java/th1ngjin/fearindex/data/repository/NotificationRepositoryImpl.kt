package th1ngjin.fearindex.data.repository

import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.data.datasource.NotificationDataSource
import th1ngjin.fearindex.data.storage.NotificationStorage
import th1ngjin.fearindex.domain.entity.NotificationSettings
import th1ngjin.fearindex.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationRepository 구현체.
 *
 * - 서버 호출(Cloud Functions)과 로컬 캐시(SharedPreferences)를 조합.
 * - updateSettings: 로컬 먼저 저장 → 서버 동기화 (optimistic).
 * - getSettings: 로컬 캐시 반환 (서버 조회 없음 — 서버는 write-only).
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dataSource: NotificationDataSource,
    private val storage: NotificationStorage,
) : NotificationRepository {

    override suspend fun registerFCMToken(deviceId: String, fcmToken: String) {
        if (ScreenshotMode.isEnabled()) return
        dataSource.registerFCMToken(deviceId, fcmToken, storage.load())
    }

    override suspend fun updateSettings(deviceId: String, settings: NotificationSettings) {
        storage.save(settings)
        if (ScreenshotMode.isEnabled()) return
        dataSource.updateSettings(
            deviceId = deviceId,
            settings = settings,
        )
    }

    override suspend fun unregisterDevice(deviceId: String) {
        if (ScreenshotMode.isEnabled()) return
        dataSource.unregisterDevice(deviceId)
    }

    override suspend fun getSettings(deviceId: String): NotificationSettings {
        return storage.load()
    }

    override suspend fun saveSettingsLocal(settings: NotificationSettings) {
        storage.save(settings)
    }

    override suspend fun loadSettingsLocal(): NotificationSettings {
        return storage.load()
    }
}
