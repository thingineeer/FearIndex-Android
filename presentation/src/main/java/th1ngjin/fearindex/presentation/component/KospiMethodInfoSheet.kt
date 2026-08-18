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
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.domain.entity.ExchangeRateQuote
import th1ngjin.fearindex.domain.entity.KospiCluster
import th1ngjin.fearindex.domain.entity.KospiFearIndex
import th1ngjin.fearindex.domain.entity.KospiSignalScore
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.common.KospiSignalText
import th1ngjin.fearindex.presentation.theme.fearScoreColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 코스피 산출 방식 시트 — iOS `KospiMethodInfoSheet` 대응.
 * 산출 방식/데이터 품질/현재 계산 정보/환율/신호 분해/클러스터 점수/결측 처리 섹션.
 */

/** iOS clusterSection displayOrder 대칭. */
private val ClusterDisplayOrder = listOf(
    KospiCluster.PRICE,
    KospiCluster.BREADTH,
    KospiCluster.SENTIMENT,
    KospiCluster.CREDIT,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KospiMethodInfoSheet(
    snapshot: KospiFearIndex?,
    exchangeRate: ExchangeRateQuote?,
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
                text = stringResource(R.string.kospi_method_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            SectionHeader(stringResource(R.string.kospi_method_summary_header))
            MethodLabel(Icons.Default.BarChart, stringResource(R.string.kospi_method_sources))
            MethodLabel(Icons.Default.EventAvailable, stringResource(R.string.kospi_method_close_only))
            MethodLabel(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.kospi_method_percentile))
            MethodLabel(Icons.Default.Verified, stringResource(R.string.kospi_method_score_collections))

            SectionHeader(stringResource(R.string.kospi_method_quality_header))
            MethodLabel(Icons.AutoMirrored.Filled.ArrowRightAlt, stringResource(R.string.kospi_method_carry_forward))
            MethodLabel(Icons.Default.Balance, stringResource(R.string.kospi_method_weight_redistribution))
            MethodLabel(Icons.Default.Storage, stringResource(R.string.kospi_method_raw_collections))

            SectionHeader(stringResource(R.string.kospi_method_current_header))
            if (snapshot != null) {
                LabeledRow(stringResource(R.string.kospi_method_data_date), snapshot.dataDate)
                LabeledRow(stringResource(R.string.kospi_method_generated_at), kstTimestamp(snapshot.generatedAt))
                LabeledRow(
                    stringResource(R.string.kospi_method_confidence),
                    stringResource(KospiSignalText.confidenceResId(snapshot.confidence)),
                )
            } else {
                MethodLabel(Icons.Default.Schedule, stringResource(R.string.kospi_method_no_snapshot))
            }

            if (exchangeRate != null) {
                SectionHeader(stringResource(R.string.kospi_method_exchange_header))
                LabeledRow(
                    stringResource(R.string.kospi_method_exchange_rate),
                    String.format(Locale.US, "%,.1f", exchangeRate.rate),
                )
                LabeledRow(
                    stringResource(R.string.kospi_method_exchange_updated),
                    exchangeRateFeedDate(exchangeRate.lastUpdated),
                )
            }

            if (snapshot != null) {
                SectionHeader(stringResource(R.string.kospi_signals_title))
                if (snapshot.signals.isEmpty()) {
                    MethodLabel(Icons.Default.Schedule, stringResource(R.string.kospi_signals_empty))
                } else {
                    snapshot.signals.forEach { signal ->
                        SheetSignalRow(signal)
                    }
                }

                SectionHeader(stringResource(R.string.kospi_method_cluster_header))
                ClusterDisplayOrder.forEach { cluster ->
                    LabeledRow(
                        stringResource(KospiSignalText.clusterNameResId(cluster)),
                        snapshot.clusterScores[cluster]
                            ?.let { String.format(Locale.US, "%.1f", it) }
                            ?: stringResource(R.string.kospi_method_unavailable),
                    )
                }

                SectionHeader(stringResource(R.string.kospi_method_missing_header))
                if (snapshot.missingSignals.isEmpty()) {
                    MethodLabel(Icons.Default.CheckCircle, stringResource(R.string.kospi_method_no_missing))
                } else {
                    val names = snapshot.missingSignals
                        .map { stringResource(KospiSignalText.signalNameResId(it)) }
                        .joinToString(", ")
                    MethodLabel(Icons.Default.ErrorOutline, names)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun MethodLabel(icon: ImageVector, text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LabeledRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SheetSignalRow(signal: KospiSignalScore) {
    val roundedScore = signal.score.roundToInt().coerceIn(0, 100)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(KospiSignalText.signalNameResId(signal.name)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.kospi_method_signal_weight,
                    stringResource(KospiSignalText.clusterNameResId(signal.cluster)),
                    (signal.weight * 100).roundToInt().toString(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = roundedScore.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = fearScoreColor(roundedScore),
        )
    }
}

/** KST 기준 계산 시각 — iOS `KospiMethodInfoSheet.timestamp` 대응. */
private fun kstTimestamp(instant: Instant): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z", Locale.US)
        .withZone(ZoneId.of("Asia/Seoul"))
        .format(instant)
