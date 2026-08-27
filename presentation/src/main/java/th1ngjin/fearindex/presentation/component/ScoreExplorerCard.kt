package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.domain.entity.DateRange
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.ReturnHorizon
import th1ngjin.fearindex.domain.util.ScoreExplorerPoint
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.common.GreedFrame
import th1ngjin.fearindex.presentation.common.LowSampleWarningBadge
import th1ngjin.fearindex.presentation.common.PremiumBadge
import th1ngjin.fearindex.presentation.common.PremiumLockRow
import th1ngjin.fearindex.presentation.common.ratingLabel
import th1ngjin.fearindex.presentation.feature.chart.ScoreExplorerUiState
import th1ngjin.fearindex.presentation.theme.Negative
import th1ngjin.fearindex.presentation.theme.Positive
import th1ngjin.fearindex.presentation.theme.fearScoreColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** 계측 QA 태그 (app 모듈 PremiumQA 테스트가 참조). */
const val SCORE_EXPLORER_CARD_TAG = "score-explorer-card"
const val SCORE_EXPLORER_SCORE_TAG = "score-explorer-score"
const val SCORE_EXPLORER_SLIDER_TAG = "score-explorer-slider"
const val SCORE_EXPLORER_RESET_TAG = "score-explorer-reset"

/** 탐욕 구간(≥75)은 셀 라벨을 ".greed" 변형으로 (iOS statLabel 동일). */

/**
 * "점수별 과거 수익률" 프리미엄 슬라이더 카드 — iOS `ScoreExplorerCardView` 1:1 (v1.9.4, 스펙 §3.3).
 *
 * 정확 버킷 통계만 표시(보간 없음). 잠금 상태는 같은 카드 프레임 안에 티저 슬라이더 + [PremiumLockRow].
 * 상태 변경은 전부 호출자(ViewModel)가 담당하고 이 컴포저블은 콜백만 올린다.
 */
@Composable
fun ScoreExplorerCard(
    state: ScoreExplorerUiState,
    onMove: (Int) -> Unit,
    onMoveEnded: (ReturnHorizon) -> Unit,
    onReset: () -> Unit,
    onUnlock: () -> Unit,
    onRestore: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag(SCORE_EXPLORER_CARD_TAG),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ExplorerHeader(onInfo = onInfo, selectedScore = state.selectedScore)
            if (state.isPremium) {
                UnlockedContent(state, onMove, onMoveEnded, onReset)
            } else {
                LockedContent(state, onUnlock, onRestore)
            }
        }
    }
}

// MARK: - Header

@Composable
private fun ExplorerHeader(onInfo: () -> Unit, selectedScore: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.score_explorer_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            PremiumBadge()
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onInfo, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.score_explorer_info_accessibility),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = stringResource(
                if (GreedFrame.isGreed(selectedScore)) R.string.score_explorer_subtitle_greed else R.string.score_explorer_subtitle,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// MARK: - Unlocked

@Composable
private fun UnlockedContent(
    state: ScoreExplorerUiState,
    onMove: (Int) -> Unit,
    onMoveEnded: (ReturnHorizon) -> Unit,
    onReset: () -> Unit,
) {
    val range = state.range
    if (range == null) {
        if (state.isLoading) LoadingRow() else EmptyState()
        return
    }
    var period by rememberSaveable { mutableStateOf(ReturnHorizon.ONE_YEAR) }
    val scoreColor = fearScoreColor(state.selectedScore)

    ScoreHeadline(score = state.selectedScore, color = scoreColor, isAtCurrent = state.isAtCurrent, onReset = onReset)
    ScoreSlider(state.selectedScore, range, scoreColor, onMove, onMoveEnded = { onMoveEnded(period) })
    PeriodPicker(selected = period, onSelect = { period = it })
    val point = state.point
    if (point != null) StatsRow(point, period, state.selectedScore) else EmptyState()
    ExplorerFooter(state.indexType, point, period, state.sourceRange)
}

@Composable
private fun ScoreHeadline(score: Int, color: Color, isAtCurrent: Boolean, onReset: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = score.toString(),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.testTag(SCORE_EXPLORER_SCORE_TAG),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = ratingLabel(score),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Spacer(modifier = Modifier.weight(1f))
        ResetButton(enabled = !isAtCurrent, onClick = onReset)
    }
}

@Composable
private fun ResetButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.heightIn(min = 32.dp).testTag(SCORE_EXPLORER_RESET_TAG),
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = stringResource(R.string.score_explorer_reset), style = MaterialTheme.typography.labelMedium)
    }
}

// MARK: - Slider

/**
 * step 1 정수 슬라이더. 반올림 값이 실제로 바뀔 때만 햅틱 + onMove (iOS sliderBinding 동일).
 * 눈금(tick)은 투명 처리 — steps 는 접근성 증감 단위로만 쓴다.
 */
@Composable
private fun ScoreSlider(
    score: Int,
    range: IntRange,
    color: Color,
    onMove: (Int) -> Unit,
    onMoveEnded: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sliderLabel = stringResource(R.string.score_explorer_slider_accessibility)
    val sliderValue = stringResource(R.string.a11y_gauge_value, score, ratingLabel(score))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RangeLabel(range.first)
        Slider(
            value = score.toFloat(),
            onValueChange = { raw ->
                val next = raw.roundToInt()
                if (next != score) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMove(next)
                }
            },
            onValueChangeFinished = onMoveEnded,
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(SCORE_EXPLORER_SLIDER_TAG)
                .semantics {
                    contentDescription = sliderLabel
                    stateDescription = sliderValue
                },
        )
        RangeLabel(range.last)
    }
}

@Composable
private fun RangeLabel(score: Int) {
    Text(
        text = score.toString(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// MARK: - Period

/** 1M / 3M / 6M / 1Y — SimilarEvents 카드와 동일 세그먼트, 기본 1Y. */
@Composable
private fun PeriodPicker(selected: ReturnHorizon, onSelect: (ReturnHorizon) -> Unit) {
    val horizons = ReturnHorizon.entries
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.insight_current_score_period_selector),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f).height(32.dp)) {
            horizons.forEachIndexed { index, horizon ->
                SegmentedButton(
                    selected = horizon == selected,
                    onClick = { onSelect(horizon) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = horizons.size),
                    icon = {},
                    label = { Text(text = horizonLabel(horizon), style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

@Composable
private fun horizonLabel(horizon: ReturnHorizon): String = stringResource(
    when (horizon) {
        ReturnHorizon.ONE_MONTH -> R.string.period_month_1
        ReturnHorizon.THREE_MONTH -> R.string.period_month_3
        ReturnHorizon.SIX_MONTH -> R.string.period_month_6
        ReturnHorizon.ONE_YEAR -> R.string.period_year_1
    },
)

// MARK: - Stats

/** 평균 / 비관(p10) / 낙관(p90). horizon 표본 0 이면 셀 "—". */
@Composable
private fun StatsRow(point: ScoreExplorerPoint, horizon: ReturnHorizon, selectedScore: Int) {
    val hasSample = point.hasSample(horizon)
    val greed = GreedFrame.isGreed(selectedScore)
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        StatCell(
            label = stringResource(if (greed) R.string.insight_current_score_avg_return_greed else R.string.insight_current_score_avg_return),
            value = horizon.value(point.returns),
            hasSample = hasSample,
        )
        VerticalDivider(modifier = Modifier.height(28.dp))
        StatCell(
            label = stringResource(if (greed) R.string.insight_current_score_max_drawdown_greed else R.string.insight_current_score_max_drawdown),
            value = horizon.value(point.worstCase),
            hasSample = hasSample,
        )
        VerticalDivider(modifier = Modifier.height(28.dp))
        StatCell(
            label = stringResource(if (greed) R.string.insight_current_score_best_return_greed else R.string.insight_current_score_best_return),
            value = horizon.value(point.bestCase),
            hasSample = hasSample,
        )
    }
}

@Composable
private fun RowScope.StatCell(label: String, value: Double, hasSample: Boolean) {
    Column(
        modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (hasSample) formatSignedPercent(value) else "—",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = statValueColor(value, hasSample),
        )
    }
}

@Composable
private fun statValueColor(value: Double, hasSample: Boolean): Color = when {
    !hasSample -> MaterialTheme.colorScheme.onSurfaceVariant
    value >= 0 -> Positive
    else -> Negative
}

private fun formatSignedPercent(value: Double): String = String.format("%+.1f%%", value)

// MARK: - Empty / Footer

/** 테이블 로드 중 (Repository 캐시 전 첫 진입) — 빈 상태 문구 깜빡임 방지. */
@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun EmptyState() {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.EventBusy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.score_explorer_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "표본 N일 · S&P 500 기준 · 데이터 2011.01–2026.08" + 저표본 배지(n<5). */
@Composable
private fun ExplorerFooter(
    indexType: FearIndexType,
    point: ScoreExplorerPoint?,
    horizon: ReturnHorizon,
    sourceRange: DateRange?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (point != null && point.isLowSample(horizon)) LowSampleWarningBadge()
        Text(
            text = footerText(indexType, point, horizon, sourceRange),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun footerText(
    indexType: FearIndexType,
    point: ScoreExplorerPoint?,
    horizon: ReturnHorizon,
    sourceRange: DateRange?,
): String {
    val parts = mutableListOf<String>()
    if (point != null) parts += stringResource(R.string.score_explorer_footer_samples, point.sampleCount(horizon))
    parts += stringResource(historicalReturnBasisRes(indexType))
    if (sourceRange != null) {
        parts += stringResource(
            R.string.score_explorer_footer_data_range,
            formatMonth(sourceRange.start),
            formatMonth(sourceRange.end),
        )
    }
    return parts.joinToString(" · ")
}

/** 자산 기준 라벨 리소스 ("S&P 500 기준" 등). */
fun historicalReturnBasisRes(indexType: FearIndexType): Int = when (indexType) {
    FearIndexType.MARKET -> R.string.insight_historical_return_basis_market
    FearIndexType.KOSPI -> R.string.insight_historical_return_basis_kospi
    FearIndexType.CRYPTO -> R.string.insight_historical_return_basis_crypto
}

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM")

private fun formatMonth(instant: Instant): String =
    monthFormatter.format(instant.atZone(ZoneId.systemDefault()))

// MARK: - Locked

@Composable
private fun LockedContent(state: ScoreExplorerUiState, onUnlock: () -> Unit, onRestore: () -> Unit) {
    LockedSliderPreview()
    PremiumLockRow(
        title = stringResource(R.string.score_explorer_lock_title),
        body = stringResource(R.string.score_explorer_lock_body),
        priceText = state.priceText,
        isBusy = state.isBusy,
        onPurchase = onUnlock,
        onRestore = onRestore,
    )
}

/** 잠긴 슬라이더 미리보기 — 숫자 없이 흐리게 (티저). 접근성 트리에서 제외. */
@Composable
private fun LockedSliderPreview() {
    Slider(
        value = 0.5f,
        onValueChange = {},
        enabled = false,
        valueRange = 0f..1f,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.35f)
            .clearAndSetSemantics {},
    )
}
