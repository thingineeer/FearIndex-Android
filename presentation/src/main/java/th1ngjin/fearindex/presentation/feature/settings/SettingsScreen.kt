package th1ngjin.fearindex.presentation.feature.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th1ngjin.fearindex.presentation.BuildConfig
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.component.AdBanner
import th1ngjin.fearindex.presentation.feature.onboarding.LocalOnboardingTour
import th1ngjin.fearindex.presentation.feature.onboarding.OnboardingAnchor
import th1ngjin.fearindex.presentation.feature.onboarding.tourAnchor

/** 광고 제거 구매 완료 체크 아이콘 색상 (iOS SF Symbol `.green`). */
private val PremiumCheckColor = Color(0xFF34C759)

/** 지원 문의 이메일 (iOS `SettingsView.supportEmail`). */
private const val SUPPORT_EMAIL = "dlaudwls1203@gmail.com"

/** 상품 가격 미로드 시 표시할 fallback (iOS `?? "US$4.99"`). */
private const val PRICE_FALLBACK = "US$4.99"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onNotificationSettingsClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onWidgetGuideClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
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

    val isAdFree by viewModel.isAdFree.collectAsStateWithLifecycle()
    val priceText by viewModel.priceText.collectAsStateWithLifecycle()
    val isPurchasing by viewModel.isPurchasing.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()

    dialog?.let {
        SettingsResultDialog(dialog = it, onDismiss = viewModel::dismissDialog)
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

            PremiumSectionCard(
                isAdFree = isAdFree,
                isPurchasing = isPurchasing,
                isRestoring = isRestoring,
                priceText = priceText ?: PRICE_FALLBACK,
                onPurchase = { context.findActivity()?.let(viewModel::purchaseRemoveAds) },
                onRestore = viewModel::restorePurchases,
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

/**
 * 광고 제거 IAP 카드 — iOS [PremiumSectionCard] 대응 (미구매: 구매 + 복원 행 / 구매됨: 체크 행).
 */
@Composable
private fun PremiumSectionCard(
    isAdFree: Boolean,
    isPurchasing: Boolean,
    isRestoring: Boolean,
    priceText: String,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_premium_header),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        if (isAdFree) {
            PremiumPurchasedRow()
        } else {
            PremiumPurchaseRow(
                priceText = priceText,
                isPurchasing = isPurchasing,
                enabled = !isPurchasing && !isRestoring,
                onPurchase = onPurchase,
            )
            HorizontalDivider()
            PremiumRestoreRow(
                isRestoring = isRestoring,
                enabled = !isPurchasing && !isRestoring,
                onRestore = onRestore,
            )
        }
    }
}

@Composable
private fun PremiumPurchaseRow(
    priceText: String,
    isPurchasing: Boolean,
    enabled: Boolean,
    onPurchase: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onPurchase)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_remove_ads_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_remove_ads_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        if (isPurchasing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(
                text = priceText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PremiumRestoreRow(
    isRestoring: Boolean,
    enabled: Boolean,
    onRestore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onRestore)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.settings_restore_purchases),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isRestoring) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun PremiumPurchasedRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = PremiumCheckColor,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.settings_remove_ads_purchased),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 구매/복원 결과 다이얼로그 — iOS `showAlert` 대응.
 * 구매 실패는 안내 문구 + 지원 이메일(작은 글씨) 2줄 구성.
 */
@Composable
private fun SettingsResultDialog(
    dialog: SettingsDialog,
    onDismiss: () -> Unit,
) {
    val message = when (dialog) {
        SettingsDialog.PurchaseFailed -> stringResource(R.string.settings_remove_ads_purchase_failed)
        SettingsDialog.RestoreSuccess -> stringResource(R.string.settings_restore_success)
        SettingsDialog.RestoreFailure -> stringResource(R.string.settings_restore_failure)
    }
    val supportContact = stringResource(R.string.settings_support_contact, SUPPORT_EMAIL)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        text = {
            Column {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                if (dialog == SettingsDialog.PurchaseFailed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = supportContact,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
