package th1ngjin.fearindex.domain.repository

import th1ngjin.fearindex.domain.entity.NotificationSettings

/**
 * 알림 설정 Repository — FCM 토큰 등록 + 서버 동기화.
 *
 * 모든 mutation은 Firebase Cloud Functions 경유 (Firestore 직접 쓰기 금지 원칙).
 */
interface NotificationRepository {
    suspend fun registerFCMToken(deviceId: String, fcmToken: String)
    suspend fun updateSettings(deviceId: String, settings: NotificationSettings)
    suspend fun unregisterDevice(deviceId: String)
    suspend fun getSettings(deviceId: String): NotificationSettings
    suspend fun saveSettingsLocal(settings: NotificationSettings)
    suspend fun loadSettingsLocal(): NotificationSettings
}
