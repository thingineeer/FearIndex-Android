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

    /** 사용자가 master toggle 값을 명시적으로 저장한 적 있는지 (초기 권한 부트스트랩 가드). */
    suspend fun hasStoredNotificationPreference(): Boolean

    /** 앱이 시스템 알림 권한 프롬프트를 띄운 적 있는지 — "실제로 표시된 최초 결정" 판별용. */
    suspend fun hasRequestedNotificationPermission(): Boolean

    suspend fun markNotificationPermissionRequested()
}
