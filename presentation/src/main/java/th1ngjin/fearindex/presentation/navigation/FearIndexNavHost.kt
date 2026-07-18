package th1ngjin.fearindex.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.EntryPointAccessors
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.core.analytics.AnalyticsManager
import th1ngjin.fearindex.core.analytics.AnalyticsScreen
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.di.AnalyticsEntryPoint
import androidx.compose.ui.platform.LocalContext
import th1ngjin.fearindex.presentation.feature.chart.ChartScreen
import th1ngjin.fearindex.presentation.feature.home.HomeScreen
import th1ngjin.fearindex.presentation.feature.home.HomeViewModel
import th1ngjin.fearindex.presentation.feature.marketdetail.MarketDetailScreen
import th1ngjin.fearindex.presentation.feature.onboarding.LocalOnboardingTour
import th1ngjin.fearindex.presentation.feature.onboarding.OnboardingDestination
import th1ngjin.fearindex.presentation.feature.onboarding.OnboardingTourOverlay
import th1ngjin.fearindex.presentation.feature.onboarding.OnboardingTourViewModel
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.presentation.feature.notification.NotificationCategory
import th1ngjin.fearindex.presentation.feature.notification.NotificationDetailScreen
import th1ngjin.fearindex.presentation.feature.notification.NotificationSettingsScreen
import th1ngjin.fearindex.presentation.feature.settings.SettingsScreen
import th1ngjin.fearindex.presentation.feature.vote.VoteScreen

enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int,
) {
    Home("home", Icons.Default.Home, R.string.tab_home),
    Chart("chart", Icons.AutoMirrored.Filled.ShowChart, R.string.tab_chart),
    Vote("vote", Icons.Default.BarChart, R.string.tab_vote),
    Settings("settings", Icons.Default.Settings, R.string.tab_settings),
}

@Composable
fun FearIndexNavHost(
    readyForTour: Boolean = true,
    qaForceTour: Boolean = false,
    qaStartStep: Int = 1,
    onTourActiveChange: (Boolean) -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val homeViewModel: HomeViewModel = hiltViewModel()
    val tourViewModel: OnboardingTourViewModel = hiltViewModel()
    val tourState = tourViewModel.uiState

    fun navigateTab(route: String) {
        if (currentDestination?.route != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    fun switchHomeSegment(type: FearIndexType) {
        if (homeViewModel.uiState.value.selectedHomeType != type) {
            homeViewModel.selectHomeIndexType(type)
        }
    }

    // 스플래시가 걷힌 뒤 신규 설치 첫 실행 자동 노출 (또는 QA 강제).
    LaunchedEffect(readyForTour, qaForceTour) {
        if (readyForTour) tourViewModel.startIfEligible(force = qaForceTour, startStep = qaStartStep)
    }

    // 앱오픈 광고 억제 신호를 MainActivity(app 모듈)로 전달 — 투어 중 광고 노출 방지.
    LaunchedEffect(tourState.isActive) { onTourActiveChange(tourState.isActive) }

    // 단계 진입 시 탭/세그먼트 전환 (iOS handleTourStep 미러).
    LaunchedEffect(tourState.isActive, tourState.index) {
        if (!tourState.isActive) return@LaunchedEffect
        when (tourViewModel.currentStep?.destination) {
            OnboardingDestination.HOME_MARKET,
            OnboardingDestination.HOME_INSIGHT -> {
                navigateTab(BottomNavItem.Home.route); switchHomeSegment(FearIndexType.MARKET)
            }
            OnboardingDestination.HOME_KOSPI -> {
                navigateTab(BottomNavItem.Home.route); switchHomeSegment(FearIndexType.KOSPI)
            }
            OnboardingDestination.HOME_CRYPTO -> {
                navigateTab(BottomNavItem.Home.route); switchHomeSegment(FearIndexType.CRYPTO)
            }
            OnboardingDestination.VOTE -> navigateTab(BottomNavItem.Vote.route)
            OnboardingDestination.SETTINGS,
            OnboardingDestination.SETTINGS_WIDGET -> navigateTab(BottomNavItem.Settings.route)
            null -> Unit
        }
    }

    // 종료(완료/건너뛰기) 시 홈 글로벌(market) 최상단 복귀 (iOS finishTour 미러).
    var tourWasActive by remember { mutableStateOf(false) }
    LaunchedEffect(tourState.isActive) {
        if (tourState.isActive) {
            tourWasActive = true
        } else if (tourWasActive) {
            tourWasActive = false
            navigateTab(BottomNavItem.Home.route)
            switchHomeSegment(FearIndexType.MARKET)
        }
    }

    val context = LocalContext.current
    val analytics = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, AnalyticsEntryPoint::class.java)
            .analyticsManager()
    }

    // 화면 진입 시 logScreen 호출 — iOS와 동일한 화면 이름 사용
    LaunchedEffect(currentDestination?.route) {
        when (currentDestination?.route) {
            "home" -> analytics.logScreen(AnalyticsScreen.홈)
            "chart" -> analytics.logScreen(AnalyticsScreen.차트)
            "vote" -> analytics.logScreen(AnalyticsScreen.투표)
            "settings" -> analytics.logScreen(AnalyticsScreen.설정)
            "notification_settings" -> {
                analytics.logScreen(AnalyticsScreen.알림설정)
                analytics.log(AnalyticsEvent.알림설정화면진입)
                analytics.log(AnalyticsEvent.알림설정진입경로(경로 = "설정탭"))
            }
        }
    }

    CompositionLocalProvider(LocalOnboardingTour provides tourViewModel) {
      Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // 본문 background(라일락)와 NavigationBar default surface(흰색) 충돌 방지 →
            // container를 background로 통일하고 indicator만 살짝 떠보이게 처리.
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
            ) {
                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelResId)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true,
                        onClick = {
                            // 탭선택 이벤트 — iOS와 동일 이름 ("탭선택", 파라미터 "탭이름")
                            analytics.log(
                                AnalyticsEvent.탭선택(
                                    탭이름 = when (item) {
                                        BottomNavItem.Home -> "홈"
                                        BottomNavItem.Chart -> "차트"
                                        BottomNavItem.Vote -> "투표"
                                        BottomNavItem.Settings -> "설정"
                                    },
                                ),
                            )
                            navController.navigate(item.route) {
                                popUpTo(
                                    navController.graph.findStartDestination().id,
                                ) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onTickerClick = { navController.navigate("market_detail") },
                )
            }
            composable(BottomNavItem.Chart.route) {
                ChartScreen(viewModel = homeViewModel)
            }
            composable(BottomNavItem.Vote.route) {
                VoteScreen(viewModel = homeViewModel)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    onNotificationSettingsClick = {
                        navController.navigate("notification_settings")
                    },
                    onPrivacyPolicyClick = {
                        navController.navigate("privacy_policy")
                    },
                    onWidgetGuideClick = {
                        navController.navigate("widget_usage_guide")
                    },
                )
            }
            composable("notification_settings") {
                NotificationSettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onCategoryClick = { category ->
                        navController.navigate("notification_detail/${category.name}")
                    },
                )
            }
            composable("notification_detail/{category}") { backStackEntry ->
                val category = runCatching {
                    NotificationCategory.valueOf(
                        backStackEntry.arguments?.getString("category").orEmpty(),
                    )
                }.getOrDefault(NotificationCategory.MARKET)
                NotificationDetailScreen(
                    category = category,
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable("privacy_policy") {
                th1ngjin.fearindex.presentation.feature.privacy.PrivacyPolicyScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable("widget_usage_guide") {
                th1ngjin.fearindex.presentation.feature.settings.WidgetUsageGuideScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable("market_detail") {
                MarketDetailScreen(onBack = { navController.popBackStack() })
            }
        }
        }

        // 온보딩 코치마크 오버레이 — Scaffold(바텀바 포함) 위 전체 화면
        val step = tourViewModel.currentStep
        if (tourState.isActive && step != null) {
            OnboardingTourOverlay(
                stepNumber = tourState.index + 1,
                totalSteps = tourViewModel.totalSteps,
                step = step,
                anchor = step.anchorId?.let { tourState.anchors[it] },
                onAdvance = { tourViewModel.advance() },
                onSkip = { tourViewModel.skip() },
            )
        }
      }
    }
}
