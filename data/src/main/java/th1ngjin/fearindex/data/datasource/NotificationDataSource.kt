package th1ngjin.fearindex.data.datasource

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
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
    private val functions: FirebaseFunctions,
) {

    suspend fun registerFCMToken(deviceId: String, fcmToken: String) {
        val payload = mapOf(
            "deviceId" to deviceId,
            "fcmToken" to fcmToken,
            "settings" to mapOf(
                "language" to languageCode(),
            ),
        )
        functions
            .getHttpsCallable("registerFCMToken")
            .call(payload)
            .await()
        Timber.d("FCM token registered: ${fcmToken.take(10)}...")
    }

    suspend fun updateSettings(
        deviceId: String,
        notificationEnabled: Boolean,
        lowerThreshold: Int,
        upperThreshold: Int,
        cryptoLowerThreshold: Int,
        cryptoUpperThreshold: Int,
    ) {
        // 클라이언트 레벨 클램핑 (iOS와 동일: lower 0~50, upper 50~100).
        val clampedLower = lowerThreshold.coerceIn(0, 50)
        val clampedUpper = upperThreshold.coerceIn(50, 100)
        val clampedCryptoLower = cryptoLowerThreshold.coerceIn(0, 50)
        val clampedCryptoUpper = cryptoUpperThreshold.coerceIn(50, 100)

        val payload = mapOf(
            "deviceId" to deviceId,
            "settings" to mapOf(
                "notificationEnabled" to notificationEnabled,
                "lowerThreshold" to clampedLower,
                "upperThreshold" to clampedUpper,
                "cryptoLowerThreshold" to clampedCryptoLower,
                "cryptoUpperThreshold" to clampedCryptoUpper,
                "cryptoNotificationEnabled" to notificationEnabled,
                "language" to languageCode(),
            ),
        )
        functions
            .getHttpsCallable("updateNotificationSettings")
            .call(payload)
            .await()
        Timber.d("Notification settings updated")
    }

    suspend fun unregisterDevice(deviceId: String) {
        val payload = mapOf("deviceId" to deviceId)
        functions
            .getHttpsCallable("unregisterDevice")
            .call(payload)
            .await()
        Timber.d("Device unregistered: $deviceId")
    }

    /**
     * ISO 639-1 2자리 언어 코드 (iOS와 동일).
     * iOS: `Locale.current.language.languageCode?.identifier ?? "en"`
     */
    private fun languageCode(): String =
        Locale.getDefault().language.ifEmpty { "en" }
}
