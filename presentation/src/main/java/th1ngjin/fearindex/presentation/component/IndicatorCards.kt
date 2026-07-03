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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.domain.entity.FearRSI
import th1ngjin.fearindex.domain.entity.ShortPressure
import th1ngjin.fearindex.presentation.R
import java.util.Locale

/**
 * 공포지수 보조 근거 카드 2종 — iOS `RSIIndicatorCardView`/`ShortPressureCardView` 대응.
 * 값/문구 표시만 담당(SRP). 계산은 Domain, ⓘ 탭은 상위에 위임(시트 표시).
 */

/** 과매수/공매도증가 = 과열(빨강), 과매도/숏커버링 = 매수 압력(파랑) — iOS 색 대응. */
private val IndicatorRed = Color(0xFFE53935)
private val IndicatorBlue = Color(0xFF1E88E5)

@Composable
fun RSIIndicatorCard(
    rsi: FearRSI,
    assetLabel: String,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoneLabel = when (rsi.signal) {
        FearRSI.RSISignal.OVERBOUGHT -> stringResource(R.string.indicator_rsi_zone_overbought)
        FearRSI.RSISignal.NEUTRAL -> stringResource(R.string.indicator_rsi_zone_neutral)
        FearRSI.RSISignal.OVERSOLD -> stringResource(R.string.indicator_rsi_zone_oversold)
    }
    val zoneColor = when (rsi.signal) {
        FearRSI.RSISignal.OVERBOUGHT -> IndicatorRed
        FearRSI.RSISignal.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        FearRSI.RSISignal.OVERSOLD -> IndicatorBlue
    }
    val supportingText = when (rsi.signal) {
        FearRSI.RSISignal.OVERBOUGHT -> stringResource(R.string.indicator_rsi_support_overbought)
        FearRSI.RSISignal.NEUTRAL -> stringResource(R.string.indicator_rsi_support_neutral)
        FearRSI.RSISignal.OVERSOLD -> stringResource(R.string.indicator_rsi_support_oversold)
    }

    IndicatorCard(
        icon = Icons.AutoMirrored.Filled.ShowChart,
        title = stringResource(R.string.indicator_rsi_title, assetLabel),
        valueText = String.format(Locale.US, "%.1f", rsi.value),
        signalLabel = zoneLabel,
        signalColor = zoneColor,
        supportingText = supportingText,
        infoContentDescription = stringResource(R.string.indicator_rsi_info_accessibility),
        onInfoClick = onInfoClick,
        modifier = modifier,
    )
}

@Composable
fun ShortPressureCard(
    shortPressure: ShortPressure,
    assetLabel: String,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signalLabel = when (shortPressure.signal) {
        ShortPressure.Signal.HEAVY_SHORTING -> stringResource(R.string.indicator_short_signal_heavy)
        ShortPressure.Signal.NEUTRAL -> stringResource(R.string.indicator_short_signal_neutral)
        ShortPressure.Signal.SHORT_COVERING -> stringResource(R.string.indicator_short_signal_covering)
    }
    val signalColor = when (shortPressure.signal) {
        ShortPressure.Signal.HEAVY_SHORTING -> IndicatorRed
        ShortPressure.Signal.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        ShortPressure.Signal.SHORT_COVERING -> IndicatorBlue
    }
    val supportingText = when (shortPressure.signal) {
        ShortPressure.Signal.HEAVY_SHORTING -> stringResource(R.string.indicator_short_support_heavy)
        ShortPressure.Signal.NEUTRAL -> stringResource(R.string.indicator_short_support_neutral)
        ShortPressure.Signal.SHORT_COVERING -> stringResource(R.string.indicator_short_support_covering)
    }

    IndicatorCard(
        icon = Icons.AutoMirrored.Filled.TrendingDown,
        title = stringResource(R.string.indicator_short_title, assetLabel),
        valueText = String.format(Locale.US, "%.1f%%", shortPressure.ratioPercent),
        signalLabel = signalLabel,
        signalColor = signalColor,
        supportingText = supportingText,
        infoContentDescription = stringResource(R.string.indicator_short_info_accessibility),
        onInfoClick = onInfoClick,
        modifier = modifier,
    )
}

@Composable
private fun IndicatorCard(
    icon: ImageVector,
    title: String,
    valueText: String,
    signalLabel: String,
    signalColor: Color,
    supportingText: String,
    infoContentDescription: String,
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = infoContentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = signalLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = signalColor,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
