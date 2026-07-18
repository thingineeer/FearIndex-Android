package th1ngjin.fearindex.presentation.feature.notification

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th1ngjin.fearindex.domain.entity.NotificationSettings
import th1ngjin.fearindex.presentation.R

/**
 * 알림 설정 허브 화면 — iOS NotificationSettingsView 대칭.
 *
 * 구조 (iOS parity):
 * - 마스터 토글 (푸시 알림 on/off)
 * - 자산별 [아이콘 + 이름 + Switch + 화살표] 행 → 탭하면 임계값 상세 화면으로 이동
 *   (글로벌 / 코스피 / 암호화폐). 주간 리포트는 토글만(상세 없음).
 * - Info footer
 *
 * 임계값 슬라이더는 자산별 상세 화면(NotificationDetailScreen)에서 편집한다.
 * 탭바를 쓰지 않아 "탭 3개 + 토글 3개 중복" 혼란을 제거했다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
    onCategoryClick: (NotificationCategory) -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // POST_NOTIFICATIONS 권한 요청 (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.toggleNotification(true)
        }
    }

    // 서버 동기화 에러 Snackbar (LaunchedEffect는 non-Composable context이므로 Resources.getString 사용)
    LaunchedEffect(syncError) {
        syncError?.let { err ->
            val message = context.getString(R.string.notification_sync_error, err)
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notification_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MasterToggleItem(
                enabled = settings.notificationEnabled,
                onToggle = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.toggleNotification(enabled)
                    }
                },
            )

            AnimatedVisibility(
                visible = settings.notificationEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 자산별 알림: 행 = [아이콘 + 이름 + 토글 + 화살표]. 탭하면 임계값 상세 화면.
                    CategoryLinkItem(
                        icon = Icons.Default.Public,
                        title = stringResource(R.string.notification_global_title),
                        checked = settings.globalNotificationEnabled,
                        onCheckedChange = viewModel::toggleGlobal,
                        onClick = { onCategoryClick(NotificationCategory.MARKET) },
                    )
                    CategoryLinkItem(
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        title = stringResource(R.string.notification_kospi_title),
                        checked = settings.kospiNotificationEnabled,
                        onCheckedChange = viewModel::toggleKospi,
                        onClick = { onCategoryClick(NotificationCategory.KOSPI) },
                    )
                    CategoryLinkItem(
                        icon = Icons.Default.CurrencyBitcoin,
                        title = stringResource(R.string.notification_crypto_title),
                        checked = settings.cryptoNotificationEnabled,
                        onCheckedChange = viewModel::toggleCrypto,
                        onClick = { onCategoryClick(NotificationCategory.CRYPTO) },
                    )
                    // 주간 리포트: 상세 화면 없이 토글만.
                    CategoryToggleItem(
                        icon = Icons.Default.DateRange,
                        title = stringResource(R.string.notification_weekly_title),
                        checked = settings.weeklyReportNotificationEnabled,
                        onCheckedChange = viewModel::toggleWeekly,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    InfoFooter()
                }
            }
        }
    }
}

/**
 * 자산별 알림 행 — 아이콘 + 이름 + on/off Switch + 상세 화면 진입 화살표.
 * 행 본문(아이콘/이름/화살표 영역)을 탭하면 임계값 상세 화면으로 이동한다.
 * Switch는 독립적으로 자산 알림 on/off.
 */
@Composable
private fun CategoryLinkItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            },
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        )
    }
}

/**
 * 토글 전용 행 — 아이콘 + 이름 + Switch (상세 화면 없음, 주간 리포트용).
 */
@Composable
private fun CategoryToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        )
    }
}

/**
 * Master toggle — Material 3 ListItem 패턴.
 * surfaceContainer로 라이트 모드에서도 경계 명확.
 */
@Composable
private fun MasterToggleItem(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.notification_push_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.notification_push_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        )
    }
}

/**
 * Info footer — 카드 제거, Icon + Text 인라인.
 */
@Composable
private fun InfoFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.notification_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
