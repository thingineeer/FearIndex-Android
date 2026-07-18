package th1ngjin.fearindex.presentation.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.presentation.BuildConfig
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.component.AdBanner
import th1ngjin.fearindex.presentation.feature.onboarding.LocalOnboardingTour
import th1ngjin.fearindex.presentation.feature.onboarding.OnboardingAnchor
import th1ngjin.fearindex.presentation.feature.onboarding.tourAnchor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onNotificationSettingsClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onWidgetGuideClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val versionName = rememberAppVersion(context)
    val shareMessage = stringResource(R.string.settings_share_app_message)
    val shareChooser = stringResource(R.string.settings_share_app_chooser)
    val tour = LocalOnboardingTour.current
    val widgetRowBringIntoView = remember { BringIntoViewRequester() }

    // 온보딩 7단계: 위젯 사용법 행이 보이도록 스크롤
    LaunchedEffect(tour?.activeStepNumber) {
        if (tour?.activeStepNumber == 7) {
            runCatching { widgetRowBringIntoView.bringIntoView() }
        }
    }

    Scaffold(
        bottomBar = {
            AdBanner(
                adUnitId = BuildConfig.ADMOB_BANNER_SETTINGS,
                screenName = "설정",
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.tab_settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.settings_menu_notification),
                modifier = Modifier.tourAnchor(tour, OnboardingAnchor.NOTIFICATION),
                onClick = onNotificationSettingsClick,
            )
            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.Star,
                title = stringResource(R.string.settings_menu_rate),
                onClick = { openPlayStoreForReview(context) },
            )
            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.Share,
                title = stringResource(R.string.settings_menu_share),
                onClick = { shareApp(context, shareMessage, shareChooser) },
            )
            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_menu_about),
                subtitle = stringResource(R.string.settings_about_version, versionName),
            )
            HorizontalDivider()

            // 앱 사용법 — 온보딩 투어 재생 (기존/신규 공통)
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = stringResource(R.string.settings_menu_app_usage),
                onClick = { tour?.restart() },
            )
            HorizontalDivider()

            // 위젯 사용법 — 온보딩 7단계 앵커 + 가이드 진입
            SettingsItem(
                icon = Icons.Default.Widgets,
                title = stringResource(R.string.settings_menu_widget_guide),
                modifier = Modifier
                    .tourAnchor(tour, OnboardingAnchor.WIDGET)
                    .bringIntoViewRequester(widgetRowBringIntoView),
                onClick = onWidgetGuideClick,
            )
            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.Description,
                title = stringResource(R.string.settings_menu_kospi_methodology),
                subtitle = stringResource(R.string.settings_kospi_methodology_summary),
            )
            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(R.string.settings_menu_privacy),
                onClick = onPrivacyPolicyClick,
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
    )
}

@Composable
private fun rememberAppVersion(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
        ?.removeSuffix("-debug")
        ?: "1.0"
} catch (e: PackageManager.NameNotFoundException) {
    "1.0"
}

/**
 * Play Store 앱 상세 페이지 → 리뷰 작성 시트를 여는 표준 Android 방식.
 * market:// URI를 Play Store 앱이 받으면 바로 열고, 없으면 웹 URL로 fallback.
 */
private fun openPlayStoreForReview(context: Context) {
    val packageName = context.packageName.removeSuffix(".debug")
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(marketIntent)
    } catch (e: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(webIntent)
    }
}

private fun shareApp(context: Context, message: String, chooserTitle: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
