package th1ngjin.fearindex.core.remoteconfig

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import th1ngjin.fearindex.core.debug.ScreenshotMode
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Remote Config 래퍼.
 *
 * iOS `RemoteConfigManager`와 동일한 키를 사용하며, Android 전용/iOS 전용 값은
 * Firebase Console의 조건부 값(condition)을 사용해 UserProperty `platform`으로 분기.
 *
 * 현재 노출 키:
 * - [Keys.ADS_ENABLED]: 광고 전체 on/off (베타 테스트에서는 false로 배포)
 * - [Keys.INTERSTITIAL_ADS_ENABLED]: 인터스티셜 광고 on/off
 * - [Keys.INTERSTITIAL_SESSION_CAP]: 세션당 인터스티셜 최대 노출 수
 * - [Keys.INTERSTITIAL_COOLDOWN_SEC]: 인터스티셜 노출 간 쿨다운(초)
 * - [Keys.KOSPI_INTERSTITIAL_ENABLED]: KOSPI 홈 진입 인터스티셜 on/off
 * - [Keys.VOTE_ENABLED]: 투표 기능 on/off (릴리즈 안전장치)
 */
@Singleton
class RemoteConfigManager @Inject constructor() {

    private val screenshotMode = ScreenshotMode.isEnabled()
    private val config by lazy { Firebase.remoteConfig }
    private val _adsConfig = MutableStateFlow(defaultAdsConfig())
    val adsConfig: StateFlow<AdsRemoteConfig> = _adsConfig.asStateFlow()

    init {
        if (!screenshotMode) {
            val settings = remoteConfigSettings {
                // Debug/staging에서는 즉시 반영, release는 1시간 캐시.
                minimumFetchIntervalInSeconds = 3600
            }
            config.setConfigSettingsAsync(settings)
            config.setDefaultsAsync(
                mapOf(
                    Keys.ADS_ENABLED to false,
                    Keys.INTERSTITIAL_ADS_ENABLED to false,
                    Keys.INTERSTITIAL_SESSION_CAP to 0,
                    Keys.INTERSTITIAL_COOLDOWN_SEC to 180,
                    Keys.KOSPI_INTERSTITIAL_ENABLED to false,
                    Keys.VOTE_ENABLED to true,
                ),
            ).addOnCompleteListener {
                _adsConfig.value = currentAdsConfig()
            }
        }
    }

    suspend fun fetchAndActivate(): Boolean {
        if (screenshotMode) return false

        return runCatching {
            config.fetchAndActivate().await()
        }.onFailure {
            Timber.tag(TAG).w(it, "Remote Config fetch 실패")
        }.getOrDefault(false).also {
            _adsConfig.value = currentAdsConfig()
        }
    }

    val adsEnabled: Boolean get() = adsConfig.value.adsEnabled
    val interstitialAdsEnabled: Boolean get() = adsConfig.value.interstitialAdsEnabled
    val interstitialSessionCap: Int
        get() = adsConfig.value.interstitialSessionCap
    val interstitialCooldownMillis: Long
        get() = adsConfig.value.interstitialCooldownMillis
    val kospiInterstitialEnabled: Boolean get() = adsConfig.value.kospiInterstitialEnabled
    val voteEnabled: Boolean get() = if (screenshotMode) true else config.getBoolean(Keys.VOTE_ENABLED)

    private fun currentAdsConfig(): AdsRemoteConfig =
        if (screenshotMode) {
            defaultAdsConfig()
        } else {
            AdsRemoteConfig(
                adsEnabled = config.getBoolean(Keys.ADS_ENABLED),
                interstitialAdsEnabled = config.getBoolean(Keys.INTERSTITIAL_ADS_ENABLED),
                interstitialSessionCap = config.getLong(Keys.INTERSTITIAL_SESSION_CAP).toInt().coerceAtLeast(0),
                interstitialCooldownMillis = config.getLong(Keys.INTERSTITIAL_COOLDOWN_SEC).coerceAtLeast(0L) * 1_000L,
                kospiInterstitialEnabled = config.getBoolean(Keys.KOSPI_INTERSTITIAL_ENABLED),
            )
        }

    private fun defaultAdsConfig(): AdsRemoteConfig =
        AdsRemoteConfig(
            adsEnabled = false,
            interstitialAdsEnabled = false,
            interstitialSessionCap = 0,
            interstitialCooldownMillis = 180_000L,
            kospiInterstitialEnabled = false,
        )

    object Keys {
        const val ADS_ENABLED = "ads_enabled"
        const val INTERSTITIAL_ADS_ENABLED = "interstitial_ads_enabled"
        const val INTERSTITIAL_SESSION_CAP = "interstitial_session_cap"
        const val INTERSTITIAL_COOLDOWN_SEC = "interstitial_cooldown_sec"
        const val KOSPI_INTERSTITIAL_ENABLED = "kospi_interstitial_enabled"
        const val VOTE_ENABLED = "vote_enabled"
    }

    private companion object {
        const val TAG = "RemoteConfig"
    }
}
