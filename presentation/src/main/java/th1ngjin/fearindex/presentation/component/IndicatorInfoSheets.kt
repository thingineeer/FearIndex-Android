package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.presentation.R

/**
 * RSI/공매도 지표 설명 시트 — iOS `RSIInfoSheet`/`ShortPressureInfoSheet` 대응.
 */

private val ScaleBlue = Color(0xFF1E88E5)
private val ScaleRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RSIInfoSheet(
    assetLabel: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.indicator_rsi_title, assetLabel),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = stringResource(R.string.indicator_rsi_info_what_title),
                body = stringResource(R.string.indicator_rsi_info_what_body),
            )
            InfoRow(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = stringResource(R.string.indicator_rsi_info_analogy_title),
                body = stringResource(R.string.indicator_rsi_info_analogy_body),
            )

            SectionHeader(stringResource(R.string.indicator_rsi_info_scale_section))
            RSIScaleBar()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.indicator_rsi_info_scale_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader(stringResource(R.string.indicator_rsi_info_read_section))
            InfoRow(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = stringResource(R.string.indicator_rsi_zone_overbought),
                body = stringResource(R.string.indicator_rsi_info_overbought_body),
            )
            InfoRow(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                title = stringResource(R.string.indicator_rsi_zone_oversold),
                body = stringResource(R.string.indicator_rsi_info_oversold_body),
            )

            SectionHeader(stringResource(R.string.indicator_rsi_info_support_section))
            InfoRow(
                icon = Icons.Default.Speed,
                title = stringResource(R.string.indicator_rsi_info_support_title),
                body = stringResource(R.string.indicator_rsi_info_support_body),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortPressureInfoSheet(
    assetLabel: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.indicator_short_title, assetLabel),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = stringResource(R.string.indicator_short_info_what_title),
                body = stringResource(R.string.indicator_short_info_what_body),
            )

            SectionHeader(stringResource(R.string.indicator_short_info_read_section))
            InfoRow(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = stringResource(R.string.indicator_short_signal_heavy),
                body = stringResource(R.string.indicator_short_support_heavy),
            )
            InfoRow(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                title = stringResource(R.string.indicator_short_signal_covering),
                body = stringResource(R.string.indicator_short_info_covering_body),
            )

            SectionHeader(stringResource(R.string.indicator_short_info_support_section))
            InfoRow(
                icon = Icons.Default.Speed,
                title = stringResource(R.string.indicator_short_info_support_title),
                body = stringResource(R.string.indicator_short_info_support_body),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(text: String) {
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 0~100 RSI 눈금 막대 — 과매도(0–30 파랑) / 중립(30–70 회색) / 과매수(70–100 빨강). */
@Composable
private fun RSIScaleBar() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(
                    modifier = Modifier
                        .weight(0.30f)
                        .height(10.dp)
                        .background(ScaleBlue, CircleShape),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Spacer(
                    modifier = Modifier
                        .weight(0.40f)
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Spacer(
                    modifier = Modifier
                        .weight(0.30f)
                        .height(10.dp)
                        .background(ScaleRed, CircleShape),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.weight(0.30f))
            Text("30", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.weight(0.40f))
            Text("70", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.weight(0.30f))
            Text("100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
