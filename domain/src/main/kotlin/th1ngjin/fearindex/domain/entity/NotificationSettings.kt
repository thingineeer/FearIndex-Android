package th1ngjin.fearindex.domain.entity

/**
 * 알림 설정 — iOS Firestore `users/{deviceId}` 필드와 1:1 매핑.
 *
 * 시장/KOSPI/암호화폐 각각 채널 on/off와 하한/상한 임계값을 가진다.
 * iOS v1.8.0 기본값: 시장 30/70, KOSPI 30/70, 암호화폐 25/75.
 */
data class NotificationSettings(
    val notificationEnabled: Boolean = false,
    val globalNotificationEnabled: Boolean = true,
    val marketLowerThreshold: Int = 30,
    val marketUpperThreshold: Int = 70,
    val kospiNotificationEnabled: Boolean = true,
    val kospiLowerThreshold: Int = 30,
    val kospiUpperThreshold: Int = 70,
    val cryptoNotificationEnabled: Boolean = true,
    val cryptoLowerThreshold: Int = 25,
    val cryptoUpperThreshold: Int = 75,
    val weeklyReportNotificationEnabled: Boolean = true,
) {
    companion object {
        const val SCHEMA_VERSION = 180
        val DEFAULT = NotificationSettings()
    }
}
