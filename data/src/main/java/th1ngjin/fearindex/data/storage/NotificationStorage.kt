package th1ngjin.fearindex.data.storage

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import th1ngjin.fearindex.domain.entity.NotificationSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 설정 로컬 캐시 (SharedPreferences).
 *
 * 서버 동기화 실패 시에도 사용자가 설정한 값을 유지하기 위한 로컬 저장소.
 * deviceId는 StuckCounterStorage와 공유 (같은 UUID).
 */
@Singleton
class NotificationStorage @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(settings: NotificationSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.notificationEnabled)
            .putBoolean(KEY_GLOBAL_ENABLED, settings.globalNotificationEnabled)
            .putInt(KEY_MARKET_LOWER, settings.marketLowerThreshold)
            .putInt(KEY_MARKET_UPPER, settings.marketUpperThreshold)
            .putBoolean(KEY_KOSPI_ENABLED, settings.kospiNotificationEnabled)
            .putInt(KEY_KOSPI_LOWER, settings.kospiLowerThreshold)
            .putInt(KEY_KOSPI_UPPER, settings.kospiUpperThreshold)
            .putBoolean(KEY_CRYPTO_ENABLED, settings.cryptoNotificationEnabled)
            .putInt(KEY_CRYPTO_LOWER, settings.cryptoLowerThreshold)
            .putInt(KEY_CRYPTO_UPPER, settings.cryptoUpperThreshold)
            .putBoolean(KEY_WEEKLY_ENABLED, settings.weeklyReportNotificationEnabled)
            .apply()
    }

    fun load(): NotificationSettings {
        val notificationEnabled = prefs.getBoolean(KEY_ENABLED, false)
        val hasLegacyNotificationState =
            prefs.contains(KEY_ENABLED) ||
                prefs.contains(KEY_MARKET_LOWER) ||
                prefs.contains(KEY_CRYPTO_LOWER)

        return NotificationSettings(
            notificationEnabled = notificationEnabled,
            globalNotificationEnabled = prefs.getBoolean(KEY_GLOBAL_ENABLED, true),
            marketLowerThreshold = prefs.getInt(KEY_MARKET_LOWER, 30),
            marketUpperThreshold = prefs.getInt(KEY_MARKET_UPPER, 70),
            kospiNotificationEnabled = if (prefs.contains(KEY_KOSPI_ENABLED)) {
                prefs.getBoolean(KEY_KOSPI_ENABLED, false)
            } else if (hasLegacyNotificationState) {
                notificationEnabled
            } else {
                true
            },
            kospiLowerThreshold = prefs.getInt(KEY_KOSPI_LOWER, 30),
            kospiUpperThreshold = prefs.getInt(KEY_KOSPI_UPPER, 70),
            cryptoNotificationEnabled = prefs.getBoolean(KEY_CRYPTO_ENABLED, true),
            cryptoLowerThreshold = prefs.getInt(KEY_CRYPTO_LOWER, 25),
            cryptoUpperThreshold = prefs.getInt(KEY_CRYPTO_UPPER, 75),
            weeklyReportNotificationEnabled = prefs.getBoolean(KEY_WEEKLY_ENABLED, true),
        )
    }

    /** master toggle 값이 명시적으로 저장된 적 있는지 (기본값과 구분). */
    fun hasStoredPreference(): Boolean = prefs.contains(KEY_ENABLED)

    /** 시스템 알림 권한 프롬프트를 띄운 적 있는지 — Android엔 iOS notDetermined가 없어 플래그로 추적. */
    fun hasRequestedPermission(): Boolean = prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)

    fun markPermissionRequested() {
        prefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "notification_settings_prefs"
        private const val KEY_ENABLED = "notificationEnabled"
        private const val KEY_PERMISSION_REQUESTED = "notificationPermissionRequested"
        private const val KEY_GLOBAL_ENABLED = "globalNotificationEnabled"
        private const val KEY_MARKET_LOWER = "marketLowerThreshold"
        private const val KEY_MARKET_UPPER = "marketUpperThreshold"
        private const val KEY_KOSPI_ENABLED = "kospiNotificationEnabled"
        private const val KEY_KOSPI_LOWER = "kospiLowerThreshold"
        private const val KEY_KOSPI_UPPER = "kospiUpperThreshold"
        private const val KEY_CRYPTO_ENABLED = "cryptoNotificationEnabled"
        private const val KEY_CRYPTO_LOWER = "cryptoLowerThreshold"
        private const val KEY_CRYPTO_UPPER = "cryptoUpperThreshold"
        private const val KEY_WEEKLY_ENABLED = "weeklyReportNotificationEnabled"
    }
}
