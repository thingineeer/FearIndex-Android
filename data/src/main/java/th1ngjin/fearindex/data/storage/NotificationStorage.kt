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
            .putInt(KEY_MARKET_LOWER, settings.marketLowerThreshold)
            .putInt(KEY_MARKET_UPPER, settings.marketUpperThreshold)
            .putInt(KEY_CRYPTO_LOWER, settings.cryptoLowerThreshold)
            .putInt(KEY_CRYPTO_UPPER, settings.cryptoUpperThreshold)
            .apply()
    }

    fun load(): NotificationSettings {
        return NotificationSettings(
            notificationEnabled = prefs.getBoolean(KEY_ENABLED, false),
            marketLowerThreshold = prefs.getInt(KEY_MARKET_LOWER, 20),
            marketUpperThreshold = prefs.getInt(KEY_MARKET_UPPER, 80),
            cryptoLowerThreshold = prefs.getInt(KEY_CRYPTO_LOWER, 25),
            cryptoUpperThreshold = prefs.getInt(KEY_CRYPTO_UPPER, 75),
        )
    }

    companion object {
        private const val PREFS_NAME = "notification_settings_prefs"
        private const val KEY_ENABLED = "notificationEnabled"
        private const val KEY_MARKET_LOWER = "marketLowerThreshold"
        private const val KEY_MARKET_UPPER = "marketUpperThreshold"
        private const val KEY_CRYPTO_LOWER = "cryptoLowerThreshold"
        private const val KEY_CRYPTO_UPPER = "cryptoUpperThreshold"
    }
}
