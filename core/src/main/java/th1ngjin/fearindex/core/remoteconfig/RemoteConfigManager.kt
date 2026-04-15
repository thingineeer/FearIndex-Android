package th1ngjin.fearindex.core.remoteconfig

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
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
 * - [Keys.ADS_ENABLED]: 배너 광고 on/off (베타 테스트에서는 false로 배포)
 * - [Keys.VOTE_ENABLED]: 투표 기능 on/off (릴리즈 안전장치)
 */
@Singleton
class RemoteConfigManager @Inject constructor() {

    private val config = Firebase.remoteConfig

    init {
        val settings = remoteConfigSettings {
            // Debug/staging에서는 즉시 반영, release는 1시간 캐시.
            minimumFetchIntervalInSeconds = 3600
        }
        config.setConfigSettingsAsync(settings)
        config.setDefaultsAsync(
            mapOf(
                Keys.ADS_ENABLED to true,
                Keys.VOTE_ENABLED to true,
            ),
        )
    }

    suspend fun fetchAndActivate(): Boolean = runCatching {
        config.fetchAndActivate().await()
    }.onFailure { Timber.tag(TAG).w(it, "Remote Config fetch 실패") }.getOrDefault(false)

    val adsEnabled: Boolean get() = config.getBoolean(Keys.ADS_ENABLED)
    val voteEnabled: Boolean get() = config.getBoolean(Keys.VOTE_ENABLED)

    object Keys {
        const val ADS_ENABLED = "ads_enabled"
        const val VOTE_ENABLED = "vote_enabled"
    }

    private companion object {
        const val TAG = "RemoteConfig"
    }
}
