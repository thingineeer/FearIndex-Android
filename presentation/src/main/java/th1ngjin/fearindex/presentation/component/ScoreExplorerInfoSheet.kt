package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.domain.entity.DateRange
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.presentation.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * "점수별 과거 수익률" ⓘ 시트 — iOS `ScoreExplorerInfoSheet` 1:1 (v1.9.4).
 * 계산 방법 / 표본 정의 / 데이터 출처(+기간·갱신일) / 한계 / 면책. `RSIInfoSheet` 패턴.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreExplorerInfoSheet(
    indexType: FearIndexType,
    sourceRange: DateRange?,
    updatedAt: Instant?,
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
                text = stringResource(R.string.score_explorer_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExplorerInfoRow(
                icon = Icons.Default.Functions,
                title = stringResource(R.string.score_explorer_info_method_title),
                body = stringResource(R.string.score_explorer_info_method_body),
            )
            ExplorerInfoRow(
                icon = Icons.Default.DateRange,
                title = stringResource(R.string.score_explorer_info_sample_title),
                body = stringResource(R.string.score_explorer_info_sample_body),
            )
            ExplorerInfoRow(
                icon = Icons.Default.Storage,
                title = stringResource(R.string.score_explorer_info_source_title),
                body = stringResource(R.string.score_explorer_info_source_body),
            )
            DataRangeRow(sourceRange = sourceRange, updatedAt = updatedAt)
            ExplorerInfoRow(
                icon = Icons.Default.WarningAmber,
                title = stringResource(R.string.score_explorer_info_limits_title),
                body = stringResource(R.string.score_explorer_info_limits_body),
            )
            DisclaimerSection(indexType)
        }
    }
}

/** 데이터 기간(있을 때만) + 갱신일. 기간이 없으면(번들 fallback) 갱신일만 표시. */
@Composable
private fun DataRangeRow(sourceRange: DateRange?, updatedAt: Instant?) {
    val updated = updatedAt?.let { stringResource(R.string.market_detail_updated_at, formatDay(it)) }
    val rangeText = sourceRange?.let {
        stringResource(R.string.score_explorer_footer_data_range, formatDay(it.start), formatDay(it.end))
    }
    val title = rangeText ?: updated ?: return
    ExplorerInfoRow(
        icon = Icons.Default.CalendarMonth,
        title = title,
        body = if (rangeText != null) updated else null,
    )
}

/** 자산별 기존 면책 문구 재사용 (S&P 500 / KOSPI / BTC). */
@Composable
private fun DisclaimerSection(indexType: FearIndexType) {
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.score_explorer_info_disclaimer_title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(disclaimerRes(indexType)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun disclaimerRes(indexType: FearIndexType): Int = when (indexType) {
    FearIndexType.MARKET -> R.string.insight_detail_disclaimer_market
    FearIndexType.KOSPI -> R.string.insight_detail_disclaimer_kospi
    FearIndexType.CRYPTO -> R.string.insight_detail_disclaimer_crypto
}

@Composable
private fun ExplorerInfoRow(icon: ImageVector, title: String, body: String?) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (body != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private fun formatDay(instant: Instant): String = dayFormatter.format(instant.atZone(ZoneId.systemDefault()))
