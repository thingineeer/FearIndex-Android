package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.domain.entity.FearVelocity
import th1ngjin.fearindex.domain.entity.HistoricalEvent
import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.InsightType
import th1ngjin.fearindex.domain.entity.MarketInsight
import th1ngjin.fearindex.domain.entity.VelocityTrend
import th1ngjin.fearindex.presentation.theme.fearScoreColor
import java.text.NumberFormat
import java.util.Locale

/**
 * 인사이트 상세 BottomSheet (Material 3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightDetailSheet(
    insight: MarketInsight,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // 헤더
            SheetHeader(insight = insight)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 타입별 상세 콘텐츠
            when (insight.type) {
                InsightType.BUY_SIGNAL -> BuySignalContent(insight)
                InsightType.HISTORICAL_RETURN -> HistoricalReturnContent(insight)
                InsightType.RETURN_CHART -> ReturnChartContent(insight)
                InsightType.DRAWDOWN_TOLERANCE -> DrawdownContent(insight)
                InsightType.NUDGE -> NudgeContent(insight)
                InsightType.FEAR_VELOCITY -> VelocityContent(insight)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun SheetHeader(insight: MarketInsight) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 점수 뱃지
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(fearScoreColor(insight.score).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${insight.score}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = fearScoreColor(insight.score),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = insight.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// BuySignal: 3x4 Grid (평균/최악/최선 x 1M/3M/6M/1Y)
// ---------------------------------------------------------------------------

@Composable
private fun BuySignalContent(insight: MarketInsight) {
    val returns = insight.returns
    val worstCase = insight.worstCase
    val bestCase = insight.bestCase

    if (returns == null) {
        Text(
            text = "수익률 데이터가 부족합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    SectionTitle("예상 수익률")
    Spacer(modifier = Modifier.height(8.dp))

    // Header row
    ReturnGridRow(
        label = "",
        m1 = "1개월",
        m3 = "3개월",
        m6 = "6개월",
        y1 = "1년",
        isHeader = true,
    )
    returns.let {
        ReturnGridRow("평균", it.oneMonth, it.threeMonth, it.sixMonth, it.oneYear)
    }
    worstCase?.let {
        ReturnGridRow("최악", it.oneMonth, it.threeMonth, it.sixMonth, it.oneYear)
    }
    bestCase?.let {
        ReturnGridRow("최선", it.oneMonth, it.threeMonth, it.sixMonth, it.oneYear)
    }

    insight.sampleCount?.let { count ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "표본 수: ${count}건",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReturnGridRow(
    label: String,
    m1: String,
    m3: String,
    m6: String,
    y1: String,
    isHeader: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val style = if (isHeader) {
            MaterialTheme.typography.labelSmall
        } else {
            MaterialTheme.typography.bodySmall
        }
        val weight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
        val color = if (isHeader) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        Text(text = label, style = style, fontWeight = weight, color = color, modifier = Modifier.weight(1f))
        Text(text = m1, style = style, fontWeight = weight, color = color, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text(text = m3, style = style, fontWeight = weight, color = color, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text(text = m6, style = style, fontWeight = weight, color = color, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text(text = y1, style = style, fontWeight = weight, color = color, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
private fun ReturnGridRow(
    label: String,
    m1: Double,
    m3: Double,
    m6: Double,
    y1: Double,
) {
    ReturnGridRow(
        label = label,
        m1 = formatPercent(m1),
        m3 = formatPercent(m3),
        m6 = formatPercent(m6),
        y1 = formatPercent(y1),
    )
}

// ---------------------------------------------------------------------------
// HistoricalReturn: 이벤트 타임라인
// ---------------------------------------------------------------------------

@Composable
private fun HistoricalReturnContent(insight: MarketInsight) {
    if (insight.historicalEvents.isEmpty()) {
        Text(
            text = "유사 시점 데이터가 부족합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    SectionTitle("과거 유사 시점")
    Spacer(modifier = Modifier.height(8.dp))

    insight.historicalEvents.forEach { event ->
        HistoricalEventCard(event)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HistoricalEventCard(event: HistoricalEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "점수 ${event.score}",
                style = MaterialTheme.typography.labelSmall,
                color = fearScoreColor(event.score),
            )
        }
        Text(
            text = event.date,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))

        // 이후 수익률
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            event.returnAfter1M?.let { ReturnChip("1M", it) }
            event.returnAfter3M?.let { ReturnChip("3M", it) }
            event.returnAfter6M?.let { ReturnChip("6M", it) }
            event.returnAfter1Y?.let { ReturnChip("1Y", it) }
        }
    }
}

@Composable
private fun ReturnChip(period: String, value: Double) {
    val color = if (value >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = period,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatPercent(value),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

// ---------------------------------------------------------------------------
// ReturnChart: 투자 시뮬레이션
// ---------------------------------------------------------------------------

@Composable
private fun ReturnChartContent(insight: MarketInsight) {
    val returns = insight.returns
    if (returns == null) {
        Text(
            text = "수익률 데이터가 부족합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    var investAmount by remember { mutableStateOf("1000000") }
    val amount = investAmount.toLongOrNull() ?: 0L
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)

    SectionTitle("투자 시뮬레이션")
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = investAmount,
        onValueChange = { investAmount = it.filter { c -> c.isDigit() } },
        label = { Text("투자 금액 (원)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (amount > 0) {
        SimulationRow("1개월 후", amount, returns.oneMonth)
        SimulationRow("3개월 후", amount, returns.threeMonth)
        SimulationRow("6개월 후", amount, returns.sixMonth)
        SimulationRow("1년 후", amount, returns.oneYear)
    }
}

@Composable
private fun SimulationRow(
    period: String,
    amount: Long,
    returnRate: Double,
) {
    val result = amount + (amount * returnRate / 100.0).toLong()
    val diff = result - amount
    val color = if (diff >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = period,
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${formatter.format(result)}원",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${if (diff >= 0) "+" else ""}${formatter.format(diff)}원 (${formatPercent(returnRate)})",
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Drawdown Tolerance
// ---------------------------------------------------------------------------

@Composable
private fun DrawdownContent(insight: MarketInsight) {
    SectionTitle("최대 낙폭 vs 1년 수익률")
    Spacer(modifier = Modifier.height(8.dp))

    val worstCase = insight.worstCase
    val returns = insight.returns

    if (worstCase == null || returns == null) {
        Text(
            text = "데이터가 부족합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    DrawdownCompareRow("최대 낙폭 (1M)", worstCase.oneMonth)
    DrawdownCompareRow("최대 낙폭 (3M)", worstCase.threeMonth)
    DrawdownCompareRow("최대 낙폭 (6M)", worstCase.sixMonth)

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "평균 1년 수익률",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = formatPercent(returns.oneYear),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (returns.oneYear >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
        )
    }
}

@Composable
private fun DrawdownCompareRow(label: String, value: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = formatPercent(value),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE53935),
        )
    }
}

// ---------------------------------------------------------------------------
// Nudge: 심리 조언
// ---------------------------------------------------------------------------

@Composable
private fun NudgeContent(insight: MarketInsight) {
    SectionTitle("시장 심리 분석")
    Spacer(modifier = Modifier.height(8.dp))

    val tips = nudgeTips(insight.score)
    tips.forEachIndexed { index, tip ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = "${index + 1}.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(24.dp),
            )
            Text(
                text = tip,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun nudgeTips(score: Int): List<String> = when {
    score <= 24 -> listOf(
        "극단적 공포는 종종 시장 바닥과 일치합니다.",
        "분할 매수 전략을 고려해보세요.",
        "장기적 관점을 유지하는 것이 중요합니다.",
    )
    score <= 44 -> listOf(
        "공포가 크지만, 기회가 숨어있을 수 있습니다.",
        "포트폴리오 리밸런싱을 검토해보세요.",
        "현금 비중을 조금 줄이는 것도 방법입니다.",
    )
    score <= 55 -> listOf(
        "시장이 방향을 찾고 있는 중립 구간입니다.",
        "기존 포지션을 유지하며 관망하세요.",
        "급격한 포지션 변경은 피하는 것이 좋습니다.",
    )
    score <= 75 -> listOf(
        "탐욕 구간에서는 수익 실현을 고려하세요.",
        "과도한 레버리지를 피하세요.",
        "방어적 자산 비중을 조금 늘려보세요.",
    )
    else -> listOf(
        "극단적 탐욕은 조정의 신호일 수 있습니다.",
        "손절 라인을 명확히 설정하세요.",
        "FOMO(놓칠 것 같은 두려움)에 휘둘리지 마세요.",
    )
}

// ---------------------------------------------------------------------------
// Fear Velocity
// ---------------------------------------------------------------------------

@Composable
private fun VelocityContent(insight: MarketInsight) {
    val velocity = insight.velocity ?: return

    SectionTitle("변화 속도")
    Spacer(modifier = Modifier.height(8.dp))

    // 일일/주간 변화
    VelocityRow("일일 변화", velocity.daily)
    VelocityRow("주간 변화", velocity.weekly)

    Spacer(modifier = Modifier.height(8.dp))

    // 추세
    val trendLabel = trendLabel(velocity.trend)
    val trendColor = trendColor(velocity.trend)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "추세",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = trendLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = trendColor,
        )
    }

    if (velocity.isInflectionPoint) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "변곡점이 감지되었습니다. 추세 전환 가능성이 있습니다.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    // 스파크라인
    if (insight.velocityHistory.size >= 3) {
        Spacer(modifier = Modifier.height(12.dp))
        SparklineChart(
            scores = insight.velocityHistory.map { it.score },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        )
    }
}

@Composable
private fun VelocityRow(label: String, value: Double) {
    val color = when {
        value > 0 -> Color(0xFF4CAF50)
        value < 0 -> Color(0xFFE53935)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sign = if (value > 0) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "$sign${"%.1f".format(value)}pt",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun SparklineChart(
    scores: List<Double>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        if (scores.size < 2) return@Canvas

        val minScore = scores.minOrNull() ?: 0.0
        val maxScore = scores.maxOrNull() ?: 100.0
        val range = (maxScore - minScore).coerceAtLeast(1.0)

        val stepX = size.width / (scores.size - 1).coerceAtLeast(1)
        val padding = 4.dp.toPx()

        val path = Path()
        scores.reversed().forEachIndexed { index, score ->
            val x = index * stepX
            val y = padding + (size.height - 2 * padding) * (1.0 - (score - minScore) / range).toFloat()
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx()),
        )

        // 마지막 포인트 (현재)
        val lastX = (scores.size - 1) * stepX
        val lastY = padding + (size.height - 2 * padding) *
            (1.0 - (scores.last() - minScore) / range).toFloat()
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = Offset(lastX, lastY),
        )
    }
}

// ---------------------------------------------------------------------------
// Utilities
// ---------------------------------------------------------------------------

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun formatPercent(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign${"%.1f".format(value)}%"
}

private fun trendLabel(trend: VelocityTrend): String = when (trend) {
    VelocityTrend.CRASH_ACCELERATING -> "급락 가속"
    VelocityTrend.CRASH_DECELERATING -> "급락 둔화"
    VelocityTrend.STABLE -> "안정"
    VelocityTrend.RALLY_ACCELERATING -> "상승 가속"
    VelocityTrend.RALLY_DECELERATING -> "상승 둔화"
}

private fun trendColor(trend: VelocityTrend): Color = when (trend) {
    VelocityTrend.CRASH_ACCELERATING -> Color(0xFFE53935)
    VelocityTrend.CRASH_DECELERATING -> Color(0xFFFF9800)
    VelocityTrend.STABLE -> Color(0xFFB59000)
    VelocityTrend.RALLY_ACCELERATING -> Color(0xFF4CAF50)
    VelocityTrend.RALLY_DECELERATING -> Color(0xFF26A69A)
}
