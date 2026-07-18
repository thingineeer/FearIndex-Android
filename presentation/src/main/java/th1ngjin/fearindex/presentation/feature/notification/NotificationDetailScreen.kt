package th1ngjin.fearindex.presentation.feature.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th1ngjin.fearindex.presentation.R

/** 알림 임계값 상세 화면이 다루는 자산 종류. */
enum class NotificationCategory { MARKET, KOSPI, CRYPTO }

/** 하한(공포)·상한(탐욕) 강조 색 — 게이지/차트와 동일 톤. */
private val LowerAccent = Color(0xFFE53935)
private val UpperAccent = Color(0xFF00897B)

/**
 * 자산별 알림 임계값 상세 화면 — iOS Market/Kospi/CryptoNotificationSettingsView 대칭.
 *
 * 하한(매수 기회) + 상한(과열 경고) 두 섹션의 슬라이더. 값/설명 문구는 자산별로 분기.
 * ViewModel 로직(clamp + debounce 서버 동기화)은 허브와 공유한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    category: NotificationCategory,
    onBackClick: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val titleRes: Int
    val lower: Int
    val upper: Int
    val lowerDescRes: Int
    val upperDescRes: Int
    val onLowerChange: (Int) -> Unit
    val onUpperChange: (Int) -> Unit
    val onFinished: () -> Unit
    when (category) {
        NotificationCategory.MARKET -> {
            titleRes = R.string.notification_global_title
            lower = settings.marketLowerThreshold
            upper = settings.marketUpperThreshold
            lowerDescRes = R.string.notification_lower_description
            upperDescRes = R.string.notification_upper_description
            onLowerChange = viewModel::updateMarketLower
            onUpperChange = viewModel::updateMarketUpper
            onFinished = viewModel::onMarketSliderFinished
        }
        NotificationCategory.KOSPI -> {
            titleRes = R.string.notification_kospi_title
            lower = settings.kospiLowerThreshold
            upper = settings.kospiUpperThreshold
            lowerDescRes = R.string.notification_kospi_lower_description
            upperDescRes = R.string.notification_kospi_upper_description
            onLowerChange = viewModel::updateKospiLower
            onUpperChange = viewModel::updateKospiUpper
            onFinished = viewModel::onKospiSliderFinished
        }
        NotificationCategory.CRYPTO -> {
            titleRes = R.string.notification_crypto_title
            lower = settings.cryptoLowerThreshold
            upper = settings.cryptoUpperThreshold
            lowerDescRes = R.string.notification_crypto_lower_description
            upperDescRes = R.string.notification_crypto_upper_description
            onLowerChange = viewModel::updateCryptoLower
            onUpperChange = viewModel::updateCryptoUpper
            onFinished = viewModel::onCryptoSliderFinished
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
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
            ThresholdSection(
                headerIcon = Icons.Default.ArrowDownward,
                headerText = stringResource(R.string.notification_lower_header),
                accent = LowerAccent,
                label = stringResource(R.string.notification_lower_label),
                value = lower,
                onChange = { onLowerChange(it) },
                onFinished = onFinished,
                supportingText = stringResource(lowerDescRes, lower),
            )
            ThresholdSection(
                headerIcon = Icons.Default.ArrowUpward,
                headerText = stringResource(R.string.notification_upper_header),
                accent = UpperAccent,
                label = stringResource(R.string.notification_upper_label),
                value = upper,
                onChange = { onUpperChange(it) },
                onFinished = onFinished,
                supportingText = stringResource(upperDescRes, upper),
            )
        }
    }
}

/**
 * 임계값 섹션 카드 — 헤더(색상 아이콘 + 제목) + 값 + 연속 슬라이더 + 설명.
 * steps = 0 (연속 슬라이더) — 99개 틱 마크 노이즈 제거.
 */
@Composable
private fun ThresholdSection(
    headerIcon: ImageVector,
    headerText: String,
    accent: Color,
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    onFinished: () -> Unit,
    supportingText: String,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = headerIcon,
                    contentDescription = null,
                    tint = accent,
                )
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

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
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }

            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                onValueChangeFinished = onFinished,
                valueRange = 0f..100f,
                steps = 0,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                ),
                modifier = Modifier.semantics {
                    contentDescription = "$label $value"
                },
            )

            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
