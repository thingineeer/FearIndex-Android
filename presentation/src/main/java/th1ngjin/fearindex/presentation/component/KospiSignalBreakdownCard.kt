package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.core.util.MarketQuoteFormat
import th1ngjin.fearindex.domain.entity.ExchangeRateQuote
import th1ngjin.fearindex.domain.entity.KospiFearIndex
import th1ngjin.fearindex.domain.entity.KospiSignalScore
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.common.KospiSignalText
import th1ngjin.fearindex.presentation.theme.fearScoreColor
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * KOSPI 신호 분해 카드 — iOS `kospiSignalBreakdownSection` 대응.
 * 서버 스냅샷의 신호별 점수/가중치/클러스터 + 환율 보조 지표를 표시.
 * ⓘ 탭은 상위에 위임(산출 방식 시트 표시).
 */

/** 환율 상승 = 원화 약세(빨강), 하락 = 원화 강세(파랑) — 한국식 등락 색. */
private val FxUpRed = Color(0xFFE53935)
private val FxDownBlue = Color(0xFF1E88E5)

@Composable
fun KospiSignalBreakdownCard(
    snapshot: KospiFearIndex,
    usdKrwRate: ExchangeRateQuote?,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ListAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.kospi_signals_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.kospi_method_accessibility),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (usdKrwRate != null) {
                ExchangeRateRow(usdKrwRate)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (snapshot.signals.isEmpty()) {
                EmptySignalRow()
            } else {
                snapshot.signals.take(MAX_SIGNAL_ROWS).forEachIndexed { index, signal ->
                    if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                    SignalRow(signal)
                }
            }

            if (snapshot.missingSignals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                MissingSignalsRow(snapshot.missingSignals)
            }
        }
    }
}

private const val MAX_SIGNAL_ROWS = 8

@Composable
private fun SignalRow(signal: KospiSignalScore) {
    val roundedScore = signal.score.roundToInt().coerceIn(0, 100)
    val scoreColor = fearScoreColor(roundedScore)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(KospiSignalText.signalNameResId(signal.name)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = roundedScore.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { (signal.score.coerceIn(0.0, 100.0) / 100.0).toFloat() },
                color = scoreColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.kospi_method_signal_weight,
                    stringResource(KospiSignalText.clusterNameResId(signal.cluster)),
                    (signal.weight * 100).roundToInt().toString(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ExchangeRateRow(quote: ExchangeRateQuote) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CurrencyExchange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.kospi_fx_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(
                    R.string.kospi_fx_updated,
                    exchangeRateFeedDate(quote.lastUpdated),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = String.format(Locale.US, "%,.1f", quote.rate),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val hasChange = quote.changePercent != null
            val badgeColor = when {
                !hasChange -> MaterialTheme.colorScheme.onSurfaceVariant
                quote.isPositive -> FxUpRed
                else -> FxDownBlue
            }
            Text(
                text = MarketQuoteFormat.changePercentText(quote.changePercent),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = badgeColor,
            )
        }
    }
}

@Composable
private fun EmptySignalRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.kospi_signals_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MissingSignalsRow(missingSignals: List<String>) {
    val names = missingSignals
        .map { stringResource(KospiSignalText.signalNameResId(it)) }
        .joinToString(", ")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.kospi_signals_missing, names),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** iOS `ExchangeRatePresentationText.feedDate` 대응 — UTC 기준 짧은 날짜. */
internal fun exchangeRateFeedDate(instant: Instant, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(ZoneOffset.UTC)
        .format(instant)
