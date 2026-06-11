package th1ngjin.fearindex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import th1ngjin.fearindex.core.ads.AdRequestAvailability
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.presentation.feature.splash.SplashView
import th1ngjin.fearindex.presentation.navigation.FearIndexNavHost
import th1ngjin.fearindex.presentation.theme.FearIndexTheme
import timber.log.Timber

private const val SPLASH_MIN_DURATION_MS = 1_500L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
                LaunchedEffect(Unit) {
                    delay(SPLASH_MIN_DURATION_MS)
                    showSplash = false
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    FearIndexNavHost()
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
