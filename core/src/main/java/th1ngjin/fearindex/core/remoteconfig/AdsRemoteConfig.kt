package th1ngjin.fearindex.core.remoteconfig

data class AdsRemoteConfig(
    val adsEnabled: Boolean = false,
    val interstitialAdsEnabled: Boolean = false,
    val interstitialSessionCap: Int = 0,
    val interstitialCooldownMillis: Long = 180_000L,
    val kospiInterstitialEnabled: Boolean = false,
)
