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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.core.util.indexTypeLabel
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.FearVelocity
import th1ngjin.fearindex.domain.entity.HistoricalEvent
import th1ngjin.fearindex.domain.entity.HistoricalReturns
import th1ngjin.fearindex.domain.entity.InsightType
import th1ngjin.fearindex.domain.entity.MarketInsight
import th1ngjin.fearindex.domain.entity.VelocityTrend
import th1ngjin.fearindex.presentation.R
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

            // 현재 점수 통계 카드 (returns 있을 때만, iOS v1.7.9 v2)
            CurrentScoreStatsCard(insight = insight)

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
                text = insightTitle(insight),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = insightSummary(insight),
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
    val basis = indexTypeLabel(insight.indexType)

    if (returns == null) {
        Text(
            text = stringResource(R.string.insight_detail_no_returns_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    SectionTitle(stringResource(R.string.insight_detail_scenario_compare_title, basis))
    Spacer(modifier = Modifier.height(8.dp))

    // Header row
    ReturnGridRow(
        label = "",
        m1 = stringResource(R.string.period_month_1),
        m3 = stringResource(R.string.period_month_3),
        m6 = stringResource(R.string.period_month_6),
        y1 = stringResource(R.string.period_year_1),
        isHeader = true,
    )
    returns.let {
        ReturnGridRow(
            stringResource(R.string.insight_detail_avg_return_basis, basis),
            it.oneMonth,
            it.threeMonth,
            it.sixMonth,
            it.oneYear,
        )
    }
    worstCase?.let {
        ReturnGridRow(
            stringResource(R.string.insight_detail_worst_case),
            it.oneMonth,
            it.threeMonth,
            it.sixMonth,
            it.oneYear,
        )
    }
    bestCase?.let {
        ReturnGridRow(
            stringResource(R.string.insight_detail_best_case),
            it.oneMonth,
            it.threeMonth,
            it.sixMonth,
            it.oneYear,
        )
    }

    insight.sampleCount?.let { count ->
        Spacer(modifier = Modifier.height(8.dp))
        val sampleBasis = when (insight.indexType) {
            FearIndexType.MARKET -> stringResource(R.string.insight_detail_sample_basis_market, count)
            FearIndexType.KOSPI -> stringResource(R.string.insight_detail_sample_basis_kospi, count)
            FearIndexType.CRYPTO -> stringResource(R.string.insight_detail_sample_basis_crypto)
        }
        Text(
            text = sampleBasis,
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
            text = stringResource(R.string.insight_detail_no_similar_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    HistoricalContextCard(score = insight.score)
    Spacer(modifier = Modifier.height(12.dp))

    SectionTitle(
        stringResource(
            R.string.insight_detail_past_events_title,
            indexTypeLabel(insight.indexType),
        ),
    )
    Spacer(modifier = Modifier.height(8.dp))

    insight.historicalEvents.forEach { event ->
        HistoricalEventCard(event)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * HistoricalReturn 상단 맥락 카드 — "왜 이 이벤트들이 보이는지" 설명.
 */
@Composable
private fun HistoricalContextCard(score: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.insight_detail_similar_past_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.insight_detail_similar_past_body, score),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                text = th1ngjin.fearindex.presentation.common.localizedEventTitle(event.description),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.insight_detail_event_score_label, event.score),
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
            text = stringResource(R.string.insight_detail_no_returns_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    var investAmount by remember { mutableStateOf("1000000") }
    val amount = investAmount.toLongOrNull() ?: 0L

    SectionTitle(
        stringResource(
            R.string.insight_detail_simulation_title,
            indexTypeLabel(insight.indexType),
        ),
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = investAmount,
        onValueChange = { investAmount = it.filter { c -> c.isDigit() } },
        label = { Text(stringResource(R.string.insight_detail_investment_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (amount > 0) {
        SimulationRow(stringResource(R.string.period_month_1_after), amount, returns.oneMonth)
        SimulationRow(stringResource(R.string.period_month_3_after), amount, returns.threeMonth)
        SimulationRow(stringResource(R.string.period_month_6_after), amount, returns.sixMonth)
        SimulationRow(stringResource(R.string.period_year_1_after), amount, returns.oneYear)
    }

    Spacer(modifier = Modifier.height(12.dp))
    DisclaimerText(indexType = insight.indexType)
}

/**
 * 수익률 관련 카드 하단 공통 면책 조항 — iOS와 동일.
 */
@Composable
private fun DisclaimerText(indexType: FearIndexType) {
    val text = when (indexType) {
        FearIndexType.MARKET -> stringResource(R.string.insight_detail_disclaimer_market)
        FearIndexType.KOSPI -> stringResource(R.string.insight_detail_disclaimer_kospi)
        FearIndexType.CRYPTO -> stringResource(R.string.insight_detail_disclaimer_crypto)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
                text = stringResource(
                    R.string.insight_detail_amount_won,
                    formatter.format(result),
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val signedDiff = "${if (diff >= 0) "+" else ""}${formatter.format(diff)}"
            Text(
                text = stringResource(
                    R.string.insight_detail_amount_diff,
                    signedDiff,
                    formatPercent(returnRate),
                ),
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
    val worstCase = insight.worstCase
    val returns = insight.returns
    val basis = indexTypeLabel(insight.indexType)

    // 맥락 카드 — "이 수치는 어디서 나왔나요?"
    DrawdownContextCard(score = insight.score, indexTypeLabel = basis)
    Spacer(modifier = Modifier.height(12.dp))

    SectionTitle(stringResource(R.string.insight_detail_drawdown_title, basis))
    Spacer(modifier = Modifier.height(8.dp))

    if (worstCase == null || returns == null) {
        Text(
            text = stringResource(R.string.insight_detail_no_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    DrawdownCompareRow(stringResource(R.string.drawdown_period_1m), worstCase.oneMonth)
    DrawdownCompareRow(stringResource(R.string.drawdown_period_3m), worstCase.threeMonth)
    DrawdownCompareRow(stringResource(R.string.drawdown_period_6m), worstCase.sixMonth)
    DrawdownCompareRow(stringResource(R.string.drawdown_period_1y), worstCase.oneYear)

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.insight_detail_avg_1y_return),
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

/**
 * Drawdown 상단 맥락 카드 — 이 수치의 출처와 의미 설명.
 */
@Composable
private fun DrawdownContextCard(score: Int, indexTypeLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.insight_detail_where_from_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.insight_detail_where_from_body,
                score,
                indexTypeLabel,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    // 메인 메시지 카드 — 현재 score에 맞춘 제목/본문
    NudgeMainCard(score = insight.score)
    Spacer(modifier = Modifier.height(16.dp))

    // 투자 행동 가이드
    SectionTitle(stringResource(R.string.insight_detail_action_guide_title))
    Spacer(modifier = Modifier.height(8.dp))

    val tipResIds = nudgeTipResIds(insight.score)
    tipResIds.forEachIndexed { index, tipRes ->
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
                text = stringResource(tipRes),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Nudge 상단 메인 메시지 카드 — score 구간별 제목과 본문.
 */
@Composable
private fun NudgeMainCard(score: Int) {
    val (titleRes, bodyRes) = nudgeMainMessageResIds(score)
    val accent = fearScoreColor(score)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.1f))
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(bodyRes, score),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Score 구간별 Nudge 메시지 리소스 ID 쌍 (title, body).
 * body는 `%1$d` 파라미터에 score를 받음.
 */
private fun nudgeMainMessageResIds(score: Int): Pair<Int, Int> = when {
    score <= 25 -> R.string.nudge_title_extreme_fear to R.string.nudge_body_extreme_fear
    score >= 75 -> R.string.nudge_title_extreme_greed to R.string.nudge_body_extreme_greed
    else -> R.string.nudge_title_stable to R.string.nudge_body_stable
}

/**
 * Score 구간별 Nudge 팁 3개의 리소스 ID 리스트.
 */
private fun nudgeTipResIds(score: Int): List<Int> = when {
    score <= 25 -> listOf(
        R.string.nudge_tip_fear_1,
        R.string.nudge_tip_fear_2,
        R.string.nudge_tip_fear_3,
    )
    score >= 75 -> listOf(
        R.string.nudge_tip_greed_1,
        R.string.nudge_tip_greed_2,
        R.string.nudge_tip_greed_3,
    )
    else -> listOf(
        R.string.nudge_tip_stable_1,
        R.string.nudge_tip_stable_2,
        R.string.nudge_tip_stable_3,
    )
}

// ---------------------------------------------------------------------------
// Fear Velocity
// ---------------------------------------------------------------------------

@Composable
private fun VelocityContent(insight: MarketInsight) {
    val velocity = insight.velocity ?: return

    SectionTitle(stringResource(R.string.velocity_analysis_title))
    Spacer(modifier = Modifier.height(8.dp))

    // 일간/7일 변화 + 모멘텀
    VelocityRow(stringResource(R.string.velocity_daily_change), velocity.daily)
    VelocityRow(stringResource(R.string.velocity_weekly_change), velocity.weekly)

    Spacer(modifier = Modifier.height(4.dp))

    // 모멘텀 상태 (빨라지는 중 / 느려지는 중 / 안정적)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.velocity_momentum_label),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(momentumLabelResId(velocity.trend)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = momentumColor(velocity.trend),
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 추세 (전체 레이블)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.velocity_trend_label),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(trendLabelResId(velocity.trend)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = trendColor(velocity.trend),
        )
    }

    if (velocity.isInflectionPoint) {
        Spacer(modifier = Modifier.height(12.dp))
        InflectionBanner()
    }

    // 스파크라인
    if (insight.velocityHistory.size >= 3) {
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.velocity_sparkline_title))
        Spacer(modifier = Modifier.height(8.dp))
        SparklineChart(
            scores = insight.velocityHistory.map { it.score },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        )
    }
}

/**
 * 변곡점 감지 배너 — iOS와 동일한 문구.
 */
@Composable
private fun InflectionBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFF9800).copy(alpha = 0.1f))
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.velocity_inflection_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE65100),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.velocity_inflection_body),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun momentumLabelResId(trend: VelocityTrend): Int = when (trend) {
    VelocityTrend.CRASH_ACCELERATING, VelocityTrend.RALLY_ACCELERATING -> R.string.momentum_accelerating
    VelocityTrend.CRASH_DECELERATING, VelocityTrend.RALLY_DECELERATING -> R.string.momentum_decelerating
    VelocityTrend.STABLE -> R.string.momentum_stable
}

private fun momentumColor(trend: VelocityTrend): Color = when (trend) {
    VelocityTrend.CRASH_ACCELERATING -> Color(0xFFE53935)
    VelocityTrend.RALLY_ACCELERATING -> Color(0xFF4CAF50)
    VelocityTrend.CRASH_DECELERATING -> Color(0xFF4CAF50)
    VelocityTrend.RALLY_DECELERATING -> Color(0xFFE53935)
    VelocityTrend.STABLE -> Color(0xFF757575)
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

        // Current point = reversed 경로의 마지막 (= scores.first(), 최신값)
        val currentScore = scores.first()
        val lastX = (scores.size - 1) * stepX
        val lastY = padding + (size.height - 2 * padding) *
            (1.0 - (currentScore - minScore) / range).toFloat()
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

private fun trendLabelResId(trend: VelocityTrend): Int = when (trend) {
    VelocityTrend.CRASH_ACCELERATING -> R.string.trend_crash_accelerating
    VelocityTrend.CRASH_DECELERATING -> R.string.trend_crash_decelerating
    VelocityTrend.STABLE -> R.string.trend_stable
    VelocityTrend.RALLY_ACCELERATING -> R.string.trend_rally_accelerating
    VelocityTrend.RALLY_DECELERATING -> R.string.trend_rally_decelerating
}

private fun trendColor(trend: VelocityTrend): Color = when (trend) {
    VelocityTrend.CRASH_ACCELERATING -> Color(0xFFE53935)
    VelocityTrend.CRASH_DECELERATING -> Color(0xFFFF9800)
    VelocityTrend.STABLE -> Color(0xFFB59000)
    VelocityTrend.RALLY_ACCELERATING -> Color(0xFF4CAF50)
    VelocityTrend.RALLY_DECELERATING -> Color(0xFF26A69A)
}

/**
 * 현재 점수 기반 통계 카드 (iOS v1.7.9 v2 InsightDetailSheet 상단 카드).
 * returns/worstCase/bestCase가 있을 때만 표시.
 * 3열: 평균 수익(green) / 최대 낙폭(red) / 최고 수익(green)
 */
@Composable
private fun CurrentScoreStatsCard(insight: MarketInsight) {
    val returns = insight.returns ?: return
    val worstCase = insight.worstCase
    val bestCase = insight.bestCase
    val sampleCount = insight.sampleCount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(
                th1ngjin.fearindex.presentation.R.string.insight_current_score_title,
                insight.score,
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(
                label = stringResource(th1ngjin.fearindex.presentation.R.string.insight_current_score_avg_return),
                value = formatPercent(returns.oneYear),
                color = if (returns.oneYear >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
            )
            worstCase?.let {
                StatItem(
                    label = stringResource(th1ngjin.fearindex.presentation.R.string.insight_current_score_max_drawdown),
                    value = formatPercent(it.oneYear),
                    color = Color(0xFFE53935),
                )
            }
            bestCase?.let {
                StatItem(
                    label = stringResource(th1ngjin.fearindex.presentation.R.string.insight_current_score_best_return),
                    value = formatPercent(it.oneYear),
                    color = Color(0xFF4CAF50),
                )
            }
        }
        if (sampleCount != null && sampleCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    th1ngjin.fearindex.presentation.R.string.insight_current_score_sample_count,
                    sampleCount,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}
