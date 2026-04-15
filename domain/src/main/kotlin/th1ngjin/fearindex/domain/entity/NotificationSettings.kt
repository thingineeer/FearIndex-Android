package th1ngjin.fearindex.domain.entity

/**
 * 알림 설정 — iOS Firestore `users/{deviceId}` 필드와 1:1 매핑.
 *
 * 시장/암호화폐 각각 하한/상한 임계값을 가진다.
 * iOS 기본값: 시장 20/80, 암호화폐 25/75.
 */
data class NotificationSettings(
    val notificationEnabled: Boolean = false,
    val marketLowerThreshold: Int = 20,
    val marketUpperThreshold: Int = 80,
    val cryptoLowerThreshold: Int = 25,
    val cryptoUpperThreshold: Int = 75,
) {
    companion object {
        val DEFAULT = NotificationSettings()
    }
}
