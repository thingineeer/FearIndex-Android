package th1ngjin.fearindex.core.remoteconfig

import th1ngjin.fearindex.core.ads.AppOpenAdConfig

data class AdsRemoteConfig(
    val adsEnabled: Boolean = false,
    val interstitialAdsEnabled: Boolean = false,
    val interstitialSessionCap: Int = 0,
    val interstitialCooldownMillis: Long = 180_000L,
    val kospiInterstitialEnabled: Boolean = false,
    // 앱오픈 광고 (iOS ServerConfig app_open_* 대응). 기본 OFF — Firebase RC 게시로 활성화.
    val appOpenAdsEnabled: Boolean = false,
    val appOpenSessionCap: Int = 2,
    val appOpenCooldownMillis: Long = 600_000L,
    val appOpenMinBackgroundMillis: Long = 30_000L,
) {
    /** 앱오픈 광고 정책 설정으로 변환. adsEnabled 마스터 스위치도 함께 반영. */
    fun appOpenAdConfig(): AppOpenAdConfig = AppOpenAdConfig(
        enabled = adsEnabled && appOpenAdsEnabled,
        sessionCap = appOpenSessionCap,
        cooldownMillis = appOpenCooldownMillis,
        minBackgroundMillis = appOpenMinBackgroundMillis,
    )
}
