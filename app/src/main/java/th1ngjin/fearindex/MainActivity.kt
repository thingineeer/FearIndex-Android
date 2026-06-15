package th1ngjin.fearindex

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import th1ngjin.fearindex.core.ads.AdRequestAvailability
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.core.remoteconfig.RemoteConfigManager
import th1ngjin.fearindex.core.update.UpdateStatus
import th1ngjin.fearindex.presentation.feature.splash.SplashView
import th1ngjin.fearindex.presentation.feature.update.ForceUpdateView
import th1ngjin.fearindex.presentation.navigation.FearIndexNavHost
import th1ngjin.fearindex.presentation.theme.FearIndexTheme
import th1ngjin.fearindex.update.InAppUpdateManager
import timber.log.Timber
import javax.inject.Inject

private const val SPLASH_MIN_DURATION_MS = 1_500L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var remoteConfig: RemoteConfigManager

    private val inAppUpdateManager by lazy {
        InAppUpdateManager(AppUpdateManagerFactory.create(this))
    }

    private val updateLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            // IMMEDIATE 플로우 결과. 사용자가 취소해도 게이트는 유지되며,
            // onResume 에서 진행 중 업데이트를 재개한다.
            Timber.i("In-App Update result: ${it.resultCode}")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // OS splash는 Android 12+ 강제 노출이지만 테마에서 아이콘을 투명 처리하여 흰 화면만 잠깐 깜빡.
        // 실제 splash UI 는 아래 Compose SplashView (iOS 와 동일한 레이아웃).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestAdsConsentInfo()
        setContent {
            FearIndexTheme {
                var showSplash by remember { mutableStateOf(true) }
                var forceUpdate by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    // 강제 업데이트 판정을 위해 Remote Config 를 먼저 fetch.
                    runCatching { remoteConfig.fetchAndActivate() }
                    forceUpdate = remoteConfig.checkForUpdate(currentVersionName()) ==
                        UpdateStatus.FORCE_UPDATE_REQUIRED
                    if (forceUpdate) {
                        startForceUpdateFlow()
                    }
                    delay(SPLASH_MIN_DURATION_MS)
                    showSplash = false
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    FearIndexNavHost()
                    if (forceUpdate) {
                        ForceUpdateView(onUpdate = ::startForceUpdateFlow)
                    }
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        SplashView()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // IMMEDIATE 업데이트가 진행 중이었다면 재개 (사용자가 잠깐 이탈한 경우).
        inAppUpdateManager.resumeIfInProgress(this, updateLauncher)
    }

    /** versionName 에서 debug suffix(`-debug`) 제거 후 순수 SemVer 만 반환. */
    private fun currentVersionName(): String = BuildConfig.VERSION_NAME.substringBefore("-")

    /**
     * Play In-App Update IMMEDIATE 를 띄우고, 불가하면 Play Store 링크로 폴백.
     */
    private fun startForceUpdateFlow() {
        inAppUpdateManager.startImmediateUpdate(
            activity = this,
            launcher = updateLauncher,
            onUnavailable = ::openPlayStore,
        )
    }

    private fun openPlayStore() {
        val packageName = applicationContext.packageName.substringBefore(".debug")
        val market = Uri.parse("market://details?id=$packageName")
        val web = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        val intent = Intent(Intent.ACTION_VIEW, market).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }.onFailure {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, web)) }
        }
    }

    private fun requestAdsConsentInfo() {
        if (ScreenshotMode.isEnabled()) return

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                AdRequestAvailability.update(consentInformation.canRequestAds())
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError ->
                    formError?.let { Timber.w("UMP consent form failed: ${it.message}") }
                    AdRequestAvailability.update(consentInformation.canRequestAds())
                }
            },
            { requestError ->
                Timber.w("UMP consent info update failed: ${requestError.message}")
                AdRequestAvailability.update(consentInformation.canRequestAds())
            },
        )
    }
}
