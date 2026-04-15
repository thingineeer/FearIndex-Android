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
 */
@Singleton
class NotificationDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
) {

    suspend fun registerFCMToken(deviceId: String, fcmToken: String) {
        val payload = mapOf(
            "deviceId" to deviceId,
            "fcmToken" to fcmToken,
            "platform" to "android",
            "language" to Locale.getDefault().toLanguageTag(),
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
        val payload = mapOf(
            "deviceId" to deviceId,
            "notificationEnabled" to notificationEnabled,
            "lowerThreshold" to lowerThreshold,
            "upperThreshold" to upperThreshold,
            "cryptoLowerThreshold" to cryptoLowerThreshold,
            "cryptoUpperThreshold" to cryptoUpperThreshold,
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
}
