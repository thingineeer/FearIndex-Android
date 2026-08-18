package th1ngjin.fearindex

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
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
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.ads.AdRequestAvailability
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.core.remoteconfig.RemoteConfigManager
import th1ngjin.fearindex.core.update.UpdateStatus
import th1ngjin.fearindex.domain.entity.NotificationPermissionSyncPolicy
import th1ngjin.fearindex.domain.repository.NotificationRepository
import th1ngjin.fearindex.domain.service.DeviceIdProvider
import th1ngjin.fearindex.presentation.feature.splash.SplashView
import th1ngjin.fearindex.presentation.feature.update.ForceUpdateView
import th1ngjin.fearindex.presentation.navigation.FearIndexNavHost
import th1ngjin.fearindex.variant.VariantHooks
import th1ngjin.fearindex.presentation.theme.FearIndexTheme
import th1ngjin.fearindex.update.InAppUpdateManager
import timber.log.Timber
import javax.inject.Inject

private const val SPLASH_MIN_DURATION_MS = 1_500L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var remoteConfig: RemoteConfigManager
    @Inject lateinit var purchaseManager: PurchaseManager
    @Inject lateinit var notificationRepository: Lazy<NotificationRepository>
    @Inject lateinit var deviceIdProvider: Lazy<DeviceIdProvider>

    private val inAppUpdateManager by lazy {
        InAppUpdateManager(AppUpdateManagerFactory.create(this))
    }

    // 알림 권한 프롬프트가 해소돼야 온보딩 투어를 띄운다(시스템 다이얼로그가 투어를 가리는 문제 방지).
    private val notificationPromptResolved = mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            applyInitialNotificationAuthorization(granted, isFirstDecision = true)
            notificationPromptResolved.value = true
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
        maybeRunInitialNotificationAuthorization()
        setContent {
            FearIndexTheme {
                var showSplash by remember { mutableStateOf(true) }
                var forceUpdate by remember { mutableStateOf(false) }
                var tourActive by remember { mutableStateOf(false) }
                // QA: 첫 실행 여부 무관 투어 강제 (디버그 빌드 전용, 스크린샷/검증)
                //   adb shell am start -n th1ngjin.fearindex.debug/th1ngjin.fearindex.MainActivity \
                //     --ez qa_onboarding true --ei qa_onboarding_step 2
                val qaForceTour = BuildConfig.DEBUG &&
                    (intent?.getBooleanExtra("qa_onboarding", false) == true)
                val qaStartStep = intent?.getIntExtra("qa_onboarding_step", 1) ?: 1
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
                // 스플래시/강제 업데이트 표시 중엔 앱오픈 광고가 그 위에 겹치지 않도록 차단.
                // (앱오픈은 콜드스타트에 원래 안 뜨지만, 그 사이 백그라운드 왕복 등 엣지 케이스 방어.)
                LaunchedEffect(showSplash, forceUpdate, tourActive) {
                    FearIndexApp.appOpenAdManager.isForegroundBlocked =
                        showSplash || forceUpdate || tourActive
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    FearIndexNavHost(
                        readyForTour = !showSplash && !forceUpdate &&
                            notificationPromptResolved.value,
                        qaForceTour = qaForceTour,
                        qaStartStep = qaStartStep,
                        onTourActiveChange = { tourActive = it },
                        settingsDebugSection = VariantHooks.settingsDebugSection(purchaseManager),
                    )
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
        // 외부(다른 기기/환불 등) 구매 상태 변화를 반영하기 위해 entitlement 재평가.
        purchaseManager.refreshEntitlements()
    }

    /**
     * 앱 시작 시 알림 권한 초기화 — iOS `setupPushNotifications` 대응 (v1.8.8 리텐션 게이트).
     *
     * 시스템 프롬프트가 실제로 표시되는 최초 결정에서 허용하면 master toggle ON + 서버 동기화.
     * 이미 결정된 기기(저장값 존재)는 정책이 NoChange를 반환해 저장값 불가침.
     */
    private fun maybeRunInitialNotificationAuthorization() {
        if (ScreenshotMode.isEnabled()) {
            notificationPromptResolved.value = true
            return
        }
        lifecycleScope.launch {
            val granted = NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()
            val repository = notificationRepository.get()
            val shouldPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !granted &&
                !repository.hasRequestedNotificationPermission()
            if (shouldPrompt) {
                // 콜백 유실(회전/프로세스 킬) 시 재프롬프트를 막기 위해 launch 시점에 마킹.
                repository.markNotificationPermissionRequested()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                applyInitialNotificationAuthorization(granted, isFirstDecision = false)
                notificationPromptResolved.value = true
            }
        }
    }

    private fun applyInitialNotificationAuthorization(granted: Boolean, isFirstDecision: Boolean) {
        lifecycleScope.launch {
            val repository = notificationRepository.get()
            val action = NotificationPermissionSyncPolicy.initialAuthorizationAction(
                systemAuthorized = granted,
                hasStoredPreference = repository.hasStoredNotificationPreference(),
                isFirstAuthorizationDecision = isFirstDecision,
            )
            when (action) {
                is NotificationPermissionSyncPolicy.InitialAuthorizationAction.NoChange -> Unit
                is NotificationPermissionSyncPolicy.InitialAuthorizationAction.InitializeLocalOnly -> {
                    repository.saveSettingsLocal(
                        repository.loadSettingsLocal().copy(notificationEnabled = action.enabled),
                    )
                }
                is NotificationPermissionSyncPolicy.InitialAuthorizationAction.InitializeAndSyncServer -> {
                    val settings = repository.loadSettingsLocal()
                        .copy(notificationEnabled = action.enabled)
                    try {
                        repository.updateSettings(deviceIdProvider.get().loadDeviceId(), settings)
                        Timber.d("Initial notification authorization synced: enabled=${action.enabled}")
                    } catch (e: Exception) {
                        Timber.e(e, "Initial notification authorization sync Error")
                    }
                }
            }
        }
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
