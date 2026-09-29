package th1ngjin.fearindex.variant

import android.content.Context
import androidx.compose.runtime.Composable
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import th1ngjin.fearindex.core.crash.CrashReporter
import th1ngjin.fearindex.core.purchases.DebugPremiumOverrideStore
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.debug.DebugPurchaseTestCard

/**
 * 빌드 변형별 훅 — debug: 결제 테스트 오버라이드(재시작 유지) 적용 + 설정 화면 "DEBUG: 결제 테스트" 카드 주입.
 * release 소스셋의 동명 object 는 전부 no-op.
 */
object VariantHooks {
    @Volatile
    private var overrideStore: DebugPremiumOverrideStore? = null

    /** 앱 시작 시 저장된 개발자 오버라이드를 다시 적용한다 (iOS `applyDebugPremiumOverrideIfNeeded`). */
    fun onApplicationCreate(context: Context, purchaseManager: PurchaseManager) {
        val store = DebugPremiumOverrideStore(context.applicationContext, purchaseManager)
        overrideStore = store
        store.applyPersisted()
    }

    /** debug: 로그캣(DebugTree)만 사용 — Crashlytics 트리 없음. */
    fun plantLogging(crashReporter: CrashReporter) = Unit

    fun settingsDebugSection(purchaseManager: PurchaseManager): (@Composable () -> Unit)? {
        val store = overrideStore ?: return null
        return { DebugPurchaseTestCard(store = store, purchaseManager = purchaseManager) }
    }

    /**
     * UMP 동의 요청 파라미터 — `adb shell setprop` 으로 EEA 동의 폼을 실측한다.
     *   debug.fearindex.ump_geo    eea | other          (비우면 실제 지역)
     *   debug.fearindex.ump_device <해시 ID>            (logcat "addTestDeviceHashedId(...)" 값)
     *   debug.fearindex.ump_reset  1                    (실행마다 동의 상태 초기화 — 끝나면 0)
     */
    fun consentRequestParameters(context: Context, consentInformation: ConsentInformation): ConsentRequestParameters {
        if (systemProperty("debug.fearindex.ump_reset") == "1") consentInformation.reset()
        val geography = when (systemProperty("debug.fearindex.ump_geo")) {
            "eea" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
            "other" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_OTHER
            else -> return ConsentRequestParameters.Builder().build()
        }
        val debugSettings = ConsentDebugSettings.Builder(context)
            .setDebugGeography(geography)
            .apply { systemProperty("debug.fearindex.ump_device").takeIf { it.isNotBlank() }?.let(::addTestDeviceHashedId) }
            .build()
        return ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings).build()
    }

    private fun systemProperty(key: String): String = try {
        val systemProperties = Class.forName("android.os.SystemProperties")
        systemProperties.getMethod("get", String::class.java, String::class.java).invoke(null, key, "") as? String ?: ""
    } catch (e: Throwable) {
        ""
    }
}
