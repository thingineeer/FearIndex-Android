package th1ngjin.fearindex.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
fun FearIndexNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val homeViewModel: HomeViewModel = hiltViewModel()

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

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelResId)) },
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
                HomeScreen(viewModel = homeViewModel)
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
                )
            }
            composable("notification_settings") {
                NotificationSettingsScreen(
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable("privacy_policy") {
                th1ngjin.fearindex.presentation.feature.privacy.PrivacyPolicyScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
