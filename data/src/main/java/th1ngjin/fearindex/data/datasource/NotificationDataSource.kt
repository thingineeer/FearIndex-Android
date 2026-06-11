package th1ngjin.fearindex.data.datasource

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import th1ngjin.fearindex.domain.entity.NotificationSettings
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Firebase Cloud Functions 호출 — 알림 설정 관련.
 *
 * 리전: asia-northeast3 (서울). Hilt에서 주입받는 FirebaseFunctions 인스턴스가 이미 해당 리전.
 *
 * **Payload 규약** (iOS `FCMService.swift`와 완전 동일):
 * - `{ deviceId, settings: { ... } }` 중첩 구조.
 * - 서버 `updateNotificationSettings`는 `data.settings.lowerThreshold` 경로로 읽음.
 * - flat 구조로 보내면 서버가 undefined로 읽어 `INTERNAL` 에러 반환.
 */
@Singleton
class NotificationDataSource @Inject constructor(
    private val functions: Provider<FirebaseFunctions>,
    private val metadataProvider: NotificationClientMetadataProvider,
) {

    suspend fun registerFCMToken(
        deviceId: String,
        fcmToken: String,
        settings: NotificationSettings,
    ) {
        val payload = mapOf(
            "deviceId" to deviceId,
            "fcmToken" to fcmToken,
            "settings" to settings.toPayload(metadataProvider.current()),
        )
        functions.get()
            .getHttpsCallable("registerFCMToken")
            .call(payload)
            .await()
        Timber.d("FCM token registered: ${fcmToken.take(10)}...")
    }

    suspend fun updateSettings(
        deviceId: String,
        settings: NotificationSettings,
    ) {
        val payload = mapOf(
            "deviceId" to deviceId,
            "settings" to settings.toPayload(metadataProvider.current()),
        )
        functions.get()
            .getHttpsCallable("updateNotificationSettings")
            .call(payload)
            .await()
        Timber.d("Notification settings updated")
    }

    suspend fun unregisterDevice(deviceId: String) {
        val payload = mapOf("deviceId" to deviceId)
        functions.get()
            .getHttpsCallable("unregisterDevice")
            .call(payload)
            .await()
        Timber.d("Device unregistered: $deviceId")
    }

    private fun NotificationSettings.toPayload(metadata: NotificationClientMetadata): Map<String, Any> =
        mapOf(
            "notificationEnabled" to notificationEnabled,
            "globalNotificationEnabled" to globalNotificationEnabled,
            "lowerThreshold" to marketLowerThreshold.clampLower(),
            "upperThreshold" to marketUpperThreshold.clampUpper(),
            "kospiNotificationEnabled" to kospiNotificationEnabled,
            "kospiLowerThreshold" to kospiLowerThreshold.clampLower(),
            "kospiUpperThreshold" to kospiUpperThreshold.clampUpper(),
            "cryptoNotificationEnabled" to cryptoNotificationEnabled,
            "cryptoLowerThreshold" to cryptoLowerThreshold.clampLower(),
            "cryptoUpperThreshold" to cryptoUpperThreshold.clampUpper(),
            "weeklyReportNotificationEnabled" to weeklyReportNotificationEnabled,
        ) + metadata.toPayload()

    private fun NotificationClientMetadata.toPayload(): Map<String, Any> =
        mapOf(
            "language" to language,
            "platform" to platform,
            "appVersion" to appVersion,
            "buildNumber" to buildNumber,
            "notificationSchemaVersion" to notificationSchemaVersion,
        )

    private fun Int.clampLower(): Int = coerceIn(0, 50)
    private fun Int.clampUpper(): Int = coerceIn(51, 100)
}
