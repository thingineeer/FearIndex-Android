package th1ngjin.fearindex.presentation.feature.history

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th1ngjin.fearindex.domain.entity.NotificationKind
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.presentation.BuildConfig
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.common.PremiumLockRow
import th1ngjin.fearindex.presentation.component.AdBanner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 알림 내역 화면 (v1.9.4, iOS `NotificationHistoryView` parity).
 * 날짜별 sticky 헤더 + 무료 배너 interleave(3,10,17…) + 하단 프리미엄 잠금 row + 빈 상태 + ⓘ 정책 시트.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    onBack: () -> Unit,
    viewModel: NotificationHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showInfoSheet by remember { mutableStateOf(false) }
    val activity = LocalContext.current.findActivity()

    // 첫 로드 완료 후 1회: 확인 처리 + `notification_history_viewed` (iOS openNotificationHistory).
    LaunchedEffect(uiState.isLoaded) {
        if (uiState.isLoaded) viewModel.onShown()
    }

    Scaffold(
        topBar = {
            HistoryTopBar(onBack = onBack, onInfoClick = { showInfoSheet = true })
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("notification-history-view"),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                !uiState.isLoaded -> LoadingState()
                uiState.records.isEmpty() -> EmptyState()
                else -> HistoryList(
                    uiState = uiState,
                    onPurchase = { activity?.let(viewModel::purchase) },
                    onRestore = viewModel::restore,
                )
            }
        }
    }

    if (showInfoSheet) {
        NotificationHistoryInfoSheet(onDismiss = { showInfoSheet = false })
    }
    uiState.dialog?.let { dialog ->
        NotificationHistoryResultDialog(dialog = dialog, onDismiss = viewModel::dismissDialog)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopBar(onBack: () -> Unit, onInfoClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.notification_history_title),
                fontWeight = FontWeight.Bold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.notification_history_info_title),
                )
            }
        },
    )
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("notification-history-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(44.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.notification_history_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.notification_history_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryList(
    uiState: NotificationHistoryUiState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    val sections = remember(uiState.records, uiState.isPremium) {
        val rows = NotificationHistoryLayout.rows(uiState.records, includeAds = !uiState.isPremium)
        NotificationHistoryLayout.sections(rows)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notification-history-list"),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        sections.forEach { section ->
            stickyHeader(key = "day-${section.day}") { DayHeader(section.day) }
            items(
                count = section.rows.size,
                key = { index -> section.rows[index].id },
            ) { index ->
                HistoryRow(row = section.rows[index])
            }
        }
        if (!uiState.isPremium) {
            item(key = "premium-lock") {
                HistoryLockRow(uiState = uiState, onPurchase = onPurchase, onRestore = onRestore)
            }
        }
    }
}

@Composable
private fun HistoryRow(row: NotificationHistoryLayout.Row) {
    when (row) {
        is NotificationHistoryLayout.Row.Record -> NotificationRecordCard(
            record = row.record,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        is NotificationHistoryLayout.Row.Banner ->
            // TODO: 알림 내역 전용 광고 유닛 생성 전 홈 배너 fallback (iOS AdConfig.historyBannerAdUnitID 대응 유닛 미발급)
            AdBanner(
                adUnitId = BuildConfig.ADMOB_BANNER_HOME,
                modifier = Modifier.padding(vertical = 8.dp),
                screenName = "알림내역",
            )
    }
}

@Composable
private fun HistoryLockRow(
    uiState: NotificationHistoryUiState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("notification-history-lock-row"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        PremiumLockRow(
            title = stringResource(R.string.notification_history_lock_title),
            body = stringResource(R.string.notification_history_lock_body),
            priceText = uiState.priceText,
            modifier = Modifier.padding(14.dp),
            isBusy = uiState.isBusy,
            onPurchase = onPurchase,
            onRestore = onRestore,
        )
    }
}

// ---------------------------------------------------------------------------
// Day header / record row
// ---------------------------------------------------------------------------

@Composable
private fun DayHeader(day: LocalDate) {
    Text(
        text = dayTitle(day),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun dayTitle(day: LocalDate): String =
    when (val label = NotificationHistoryLayout.dayLabel(day, LocalDate.now())) {
        NotificationHistoryLayout.DayLabel.Today ->
            stringResource(R.string.notification_history_today)
        NotificationHistoryLayout.DayLabel.Yesterday ->
            stringResource(R.string.notification_history_yesterday)
        is NotificationHistoryLayout.DayLabel.Date ->
            remember(label.day) {
                label.day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            }
    }

@Composable
private fun NotificationRecordCard(record: NotificationRecord, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            KindIcon(kind = record.kind)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                RecordMetaLine(record = record)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = record.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun KindIcon(kind: NotificationKind) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = kind.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RecordMetaLine(record: NotificationRecord) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(record.kind.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        record.score?.let { score ->
            Spacer(modifier = Modifier.width(6.dp))
            ScoreChip(score = score)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = relativeOrShortTime(record.receivedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun ScoreChip(score: Int) {
    Text(
        text = "$score",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** 1시간 안쪽은 상대 시간("5분 전"), 그 외는 localized 짧은 시각 (iOS timeText 규칙). */
@Composable
private fun relativeOrShortTime(receivedAt: Instant): String {
    val nowMillis = System.currentTimeMillis()
    val millis = receivedAt.toEpochMilli()
    return if (nowMillis - millis < DateUtils.HOUR_IN_MILLIS) {
        DateUtils.getRelativeTimeSpanString(millis, nowMillis, DateUtils.MINUTE_IN_MILLIS).toString()
    } else {
        remember(millis) {
            receivedAt.atZone(ZoneId.systemDefault()).toLocalTime()
                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        }
    }
}

private fun NotificationKind.icon(): ImageVector = when (this) {
    NotificationKind.MARKET -> Icons.Outlined.Public
    NotificationKind.KOSPI -> Icons.AutoMirrored.Outlined.TrendingUp
    NotificationKind.CRYPTO -> Icons.Outlined.CurrencyBitcoin
    NotificationKind.WEEKLY -> Icons.Outlined.Description
    NotificationKind.OTHER -> Icons.Outlined.Notifications
}

private fun NotificationKind.labelRes(): Int = when (this) {
    NotificationKind.MARKET -> R.string.notification_history_kind_global
    NotificationKind.KOSPI -> R.string.notification_history_kind_kospi
    NotificationKind.CRYPTO -> R.string.notification_history_kind_crypto
    NotificationKind.WEEKLY -> R.string.notification_history_kind_weekly
    NotificationKind.OTHER -> R.string.notification_history_kind_other
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 구매/복원 결과 다이얼로그 — 설정 화면(SettingsResultDialog) 문구 재사용. */
@Composable
private fun NotificationHistoryResultDialog(
    dialog: NotificationHistoryDialog,
    onDismiss: () -> Unit,
) {
    val message = when (dialog) {
        NotificationHistoryDialog.PurchaseFailed ->
            stringResource(R.string.settings_remove_ads_purchase_failed)
        NotificationHistoryDialog.RestoreSuccess ->
            stringResource(R.string.settings_restore_success)
        NotificationHistoryDialog.RestoreFailure ->
            stringResource(R.string.settings_restore_failure)
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
    )
}
