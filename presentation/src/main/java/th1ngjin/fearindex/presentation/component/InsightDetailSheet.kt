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
import th1ngjin.fearindex.core.util.indexTypeLabel
import th1ngjin.fearindex.domain.entity.FearIndexType
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
    val basis = indexTypeLabel(insight.indexType)

    if (returns == null) {
        Text(
            text = "수익률 데이터가 부족합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    SectionTitle("시나리오 비교 ($basis)")
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
        ReturnGridRow("평균 수익률 ($basis)", it.oneMonth, it.threeMonth, it.sixMonth, it.oneYear)
    }
    worstCase?.let {
        ReturnGridRow("최악의 경우", it.oneMonth, it.threeMonth, it.sixMonth, it.oneYear)
    }
    bestCase?.let {
        ReturnGridRow("최선의 경우", it.oneMonth, it.threeMonth, it.sixMonth, it.oneYear)
    }

    insight.sampleCount?.let { count ->
        Spacer(modifier = Modifier.height(8.dp))
        val sampleBasis = if (insight.indexType == FearIndexType.CRYPTO) {
            "암호화폐 시장 심리 데이터 기준"
        } else {
            "${count}번의 극단적 구간 기준 (S&P 500)"
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
            text = "유사 시점 데이터가 부족합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    HistoricalContextCard(score = insight.score)
    Spacer(modifier = Modifier.height(12.dp))

    SectionTitle("과거 유사 이벤트 (${indexTypeLabel(insight.indexType)})")
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
            text = "현재와 비슷했던 과거 시점",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "공포지수 ${score}점 근처였던 과거 시점들입니다. 현재 시장 상황과 가장 유사한 역사적 순간을 참고하세요.",
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

    SectionTitle("투자 시뮬레이션 (${indexTypeLabel(insight.indexType)})")
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

    Spacer(modifier = Modifier.height(12.dp))
    DisclaimerText(indexType = insight.indexType)
}

/**
 * 수익률 관련 카드 하단 공통 면책 조항 — iOS와 동일.
 */
@Composable
private fun DisclaimerText(indexType: FearIndexType) {
    val text = if (indexType == FearIndexType.CRYPTO) {
        "⚠️ Bitcoin(BTC) 가격 기준. 암호화폐는 극도로 변동성이 큽니다."
    } else {
        "⚠️ S&P 500 과거 데이터 기준. 과거 성과는 미래 수익을 보장하지 않습니다."
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
    val worstCase = insight.worstCase
    val returns = insight.returns
    val basis = indexTypeLabel(insight.indexType)

    // 맥락 카드 — "이 수치는 어디서 나왔나요?"
    DrawdownContextCard(score = insight.score, indexTypeLabel = basis)
    Spacer(modifier = Modifier.height(12.dp))

    SectionTitle("낙폭 분석 ($basis)")
    Spacer(modifier = Modifier.height(8.dp))

    if (worstCase == null || returns == null) {
        Text(
            text = "데이터가 부족합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    DrawdownCompareRow("최대 낙폭 (1개월)", worstCase.oneMonth)
    DrawdownCompareRow("최대 낙폭 (3개월)", worstCase.threeMonth)
    DrawdownCompareRow("최대 낙폭 (6개월)", worstCase.sixMonth)
    DrawdownCompareRow("최대 낙폭 (1년)", worstCase.oneYear)

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
            text = "이 수치는 어디서 나왔나요?",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "현재 공포지수 ${score}점과 비슷했던 과거 시점들을 기반으로 $indexTypeLabel 수익률을 계산한 값입니다.",
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
    SectionTitle("투자 행동 가이드")
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

/**
 * Nudge 상단 메인 메시지 카드 — score 구간별 제목과 본문.
 */
@Composable
private fun NudgeMainCard(score: Int) {
    val (title, body) = nudgeMainMessage(score)
    val accent = fearScoreColor(score)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.1f))
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun nudgeMainMessage(score: Int): Pair<String, String> = when {
    score <= 25 -> "극도의 공포 구간" to
        "현재 공포지수 ${score}점은 시장이 매우 극도의 공포 상태임을 나타냅니다. " +
        "역사적으로 이런 시기가 장기적으로 매수 기회가 된 경우가 많았습니다. " +
        "단, 단기 추가 하락 가능성도 있습니다."
    score >= 75 -> "극도의 탐욕 구간" to
        "현재 공포지수 ${score}점은 시장이 과열된 상태임을 나타냅니다. " +
        "역사적으로 이런 시기에 무리한 추가 매수보다 리스크 관리가 중요했습니다."
    else -> "시장이 안정적인 구간" to
        "현재 공포지수 ${score}점은 극단적 공포나 탐욕 없이 시장이 비교적 안정적인 상태입니다. " +
        "일관된 장기 투자 전략을 유지하기 좋은 시기입니다."
}

private fun nudgeTips(score: Int): List<String> = when {
    score <= 25 -> listOf(
        "분할 매수로 진입 타이밍 분산",
        "현금 비중 확인 후 여유 자금으로 매수",
        "공포에 즉흥적으로 팔지 말기",
    )
    score >= 75 -> listOf(
        "포트폴리오 비중 점검",
        "목표 수익 도달 시 일부 이익 실현",
        "FOMO에 휩쓸리지 말기",
    )
    else -> listOf(
        "장기 투자 목표 확인",
        "정기 적립 유지",
        "포트폴리오 리밸런싱 검토",
    )
}

// ---------------------------------------------------------------------------
// Fear Velocity
// ---------------------------------------------------------------------------

@Composable
private fun VelocityContent(insight: MarketInsight) {
    val velocity = insight.velocity ?: return

    SectionTitle("속도 분석")
    Spacer(modifier = Modifier.height(8.dp))

    // 일간/7일 변화 + 모멘텀
    VelocityRow("일간 변화", velocity.daily)
    VelocityRow("7일 변화", velocity.weekly)

    Spacer(modifier = Modifier.height(4.dp))

    // 모멘텀 상태 (빨라지는 중 / 느려지는 중 / 안정적)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "모멘텀",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = momentumLabel(velocity.trend),
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
            text = "추세",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = trendLabel(velocity.trend),
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
        SectionTitle("최근 15일 추세")
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
            text = "방향이 바뀌고 있어요!",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE65100),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "최근 추세가 반전됐습니다 — 주목하세요.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun momentumLabel(trend: VelocityTrend): String = when (trend) {
    VelocityTrend.CRASH_ACCELERATING, VelocityTrend.RALLY_ACCELERATING -> "빨라지는 중"
    VelocityTrend.CRASH_DECELERATING, VelocityTrend.RALLY_DECELERATING -> "느려지는 중"
    VelocityTrend.STABLE -> "안정적"
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
