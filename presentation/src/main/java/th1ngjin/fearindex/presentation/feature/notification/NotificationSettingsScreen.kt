package th1ngjin.fearindex.presentation.feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 알림 설정 화면 — iOS NotificationSettingsView 경량 포팅.
 *
 * 현재 범위 (v0):
 * - 마스터 토글 (알림 on/off)
 * - 시장 공포지수: 하한/상한 임계값 슬라이더
 * - 암호화폐 공포지수: 하한/상한 임계값 슬라이더
 *
 * TODO: FCM 토큰 등록, 서버 동기화, 권한 요청은 후속 작업.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
) {
    var notificationEnabled by remember { mutableStateOf(false) }
    var marketLower by remember { mutableFloatStateOf(25f) }
    var marketUpper by remember { mutableFloatStateOf(75f) }
    var cryptoLower by remember { mutableFloatStateOf(25f) }
    var cryptoUpper by remember { mutableFloatStateOf(75f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("알림 설정") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MasterToggleCard(
                enabled = notificationEnabled,
                onToggle = { notificationEnabled = it },
            )

            if (notificationEnabled) {
                ThresholdCard(
                    title = "시장 공포지수 알림",
                    lower = marketLower,
                    upper = marketUpper,
                    onLowerChange = { marketLower = it },
                    onUpperChange = { marketUpper = it },
                )

                ThresholdCard(
                    title = "암호화폐 공포지수 알림",
                    lower = cryptoLower,
                    upper = cryptoUpper,
                    onLowerChange = { cryptoLower = it },
                    onUpperChange = { cryptoUpper = it },
                )

                InfoCard()
            }
        }
    }
}

@Composable
private fun MasterToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = "푸시 알림",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "공포지수가 설정한 임계값에 도달하면 알림을 받습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun ThresholdCard(
    title: String,
    lower: Float,
    upper: Float,
    onLowerChange: (Float) -> Unit,
    onUpperChange: (Float) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Lower threshold
            ThresholdRow(
                label = "하한 (공포)",
                value = lower,
                color = MaterialTheme.colorScheme.primary,
                onChange = { v ->
                    onLowerChange(v.coerceAtMost(upper - 1))
                },
            )
            Text(
                text = "공포지수가 ${lower.toInt()} 이하일 때 알림을 받습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Upper threshold
            ThresholdRow(
                label = "상한 (탐욕)",
                value = upper,
                color = MaterialTheme.colorScheme.primary,
                onChange = { v ->
                    onUpperChange(v.coerceAtLeast(lower + 1))
                },
            )
            Text(
                text = "공포지수가 ${upper.toInt()} 이상일 때 알림을 받습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThresholdRow(
    label: String,
    value: Float,
    color: androidx.compose.ui.graphics.Color,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..100f,
            steps = 99,
        )
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Text(
            text = "알림은 15분 간격으로 체크되며, 하루에 같은 구간 진입 시 최대 1회만 발송됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}
