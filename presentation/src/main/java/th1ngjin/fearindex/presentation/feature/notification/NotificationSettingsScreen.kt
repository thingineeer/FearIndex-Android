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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th1ngjin.fearindex.presentation.R

/**
 * 알림 설정 화면 — iOS NotificationSettingsView 대칭.
 *
 * Material 3 settings 패턴:
 * - Master toggle: ListItem (headlineContent + supportingContent + trailingContent)
 * - Threshold 섹션: PrimaryTabRow + Card(surfaceContainer) + 연속 Slider (steps=0)
 * - Info footer: 카드 대신 인라인 Icon + Text (시각적 노이즈 감소)
 *
 * ViewModel 기반 서버 동기화 (debounce 0.5초).
 * Android 13+ POST_NOTIFICATIONS 권한 요청.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
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

    // 서버 동기화 에러 Snackbar (다국어 — LaunchedEffect는 non-Composable context이므로 Resources.getString 사용)
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
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.notification_tab_market)) },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.notification_tab_crypto)) },
                        )
                    }

                    when (selectedTab) {
                        0 -> ThresholdCard(
                            title = stringResource(R.string.notification_market_title),
                            lower = settings.marketLowerThreshold.toFloat(),
                            upper = settings.marketUpperThreshold.toFloat(),
                            onLowerChange = { viewModel.updateMarketLower(it.toInt()) },
                            onUpperChange = { viewModel.updateMarketUpper(it.toInt()) },
                            onValueChangeFinished = { viewModel.onMarketSliderFinished() },
                        )
                        1 -> ThresholdCard(
                            title = stringResource(R.string.notification_crypto_title),
                            lower = settings.cryptoLowerThreshold.toFloat(),
                            upper = settings.cryptoUpperThreshold.toFloat(),
                            onLowerChange = { viewModel.updateCryptoLower(it.toInt()) },
                            onUpperChange = { viewModel.updateCryptoUpper(it.toInt()) },
                            onValueChangeFinished = { viewModel.onCryptoSliderFinished() },
                        )
                    }

                    InfoFooter()
                }
            }
        }
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
 * Threshold 카드 — 연속 슬라이더 (steps=0).
 * 99 스텝 점선 노이즈 제거, 값은 상단 텍스트로 표시.
 */
@Composable
private fun ThresholdCard(
    title: String,
    lower: Float,
    upper: Float,
    onLowerChange: (Float) -> Unit,
    onUpperChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            ThresholdRow(
                label = stringResource(R.string.notification_lower_label),
                value = lower,
                onChange = { v -> onLowerChange(v.coerceAtMost(upper - 1)) },
                onValueChangeFinished = onValueChangeFinished,
                supportingText = stringResource(
                    R.string.notification_lower_description,
                    lower.toInt(),
                ),
            )

            ThresholdRow(
                label = stringResource(R.string.notification_upper_label),
                value = upper,
                onChange = { v -> onUpperChange(v.coerceAtLeast(lower + 1)) },
                onValueChangeFinished = onValueChangeFinished,
                supportingText = stringResource(
                    R.string.notification_upper_description,
                    upper.toInt(),
                ),
            )
        }
    }
}

/**
 * 슬라이더 + 값 표시 + supporting text.
 * steps = 0 (연속 슬라이더) — 99개 틱 마크 노이즈 제거.
 * 값은 상단 라벨 옆 타이틀 텍스트로 명확히 표시.
 */
@Composable
private fun ThresholdRow(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    supportingText: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..100f,
            steps = 0,
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Info footer — 카드 제거, Icon + Text 인라인.
 * 시각적 밀도 완화 + Material 3 footer 패턴.
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
    Spacer(modifier = Modifier.height(8.dp))
}
