package th1ngjin.fearindex.presentation.feature.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.hilt.navigation.compose.hiltViewModel
import th1ngjin.fearindex.core.util.ChartCoordinates
import th1ngjin.fearindex.core.util.ChartDataFilter
import th1ngjin.fearindex.core.util.IndexTimestampFormatter
import th1ngjin.fearindex.presentation.component.RepresentativeIndexInfoSheet
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexPeak
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.MarketInsight
import th1ngjin.fearindex.domain.usecase.ComputeFearIndexPeaks
import th1ngjin.fearindex.presentation.BuildConfig
import th1ngjin.fearindex.presentation.R
import dagger.hilt.android.EntryPointAccessors
import th1ngjin.fearindex.core.analytics.AnalyticsEvent
import th1ngjin.fearindex.presentation.common.ratingLabel
import th1ngjin.fearindex.presentation.component.AdBanner
import th1ngjin.fearindex.presentation.component.ChartSkeletonView
import th1ngjin.fearindex.presentation.component.InsightDetailSheet
import th1ngjin.fearindex.presentation.component.InsightFeedView
import th1ngjin.fearindex.presentation.component.SegmentedPicker
import th1ngjin.fearindex.presentation.di.AnalyticsEntryPoint
import th1ngjin.fearindex.presentation.feature.home.FearIndexState
import th1ngjin.fearindex.presentation.feature.home.HomeViewModel
import th1ngjin.fearindex.presentation.feature.insight.InsightIndexScope
import th1ngjin.fearindex.presentation.feature.insight.InsightViewModel
import th1ngjin.fearindex.presentation.theme.fearScoreColor
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// MARK: - Chart Period

enum class ChartPeriod(val label: String, val days: Int) {
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    ONE_YEAR("1Y", 365),
    TWO_YEARS("2Y", 730),
    THREE_YEARS("3Y", 1095),
    FIVE_YEARS("5Y", 1825);

    companion object {
        /** ViewModel에 저장된 days로부터 현재 선택된 기간을 역산. */
        fun fromDays(days: Int): ChartPeriod =
            entries.firstOrNull { it.days == days } ?: THREE_MONTHS
    }
}

enum class CryptoChartPeriod(val label: String, val days: Int) {
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    ONE_YEAR("1Y", 365),
    TWO_YEARS("2Y", 730),
    THREE_YEARS("3Y", 1095),
    FIVE_YEARS("5Y", 1825);

    companion object {
        fun fromDays(days: Int): CryptoChartPeriod =
            entries.firstOrNull { it.days == days } ?: THREE_MONTHS
    }
}

// MARK: - Analytics Constants (사용자 UI에 노출되지 않는 Analytics 이벤트 파라미터용 한국어 상수)

private const val ANALYTICS_TYPE_MARKET = "시장"
private const val ANALYTICS_TYPE_KOSPI = "코스피"
private const val ANALYTICS_TYPE_CRYPTO = "암호화폐"

// MARK: - Chart Colors

private val ChartLineColor = Color(0xFFFF9800)
private val ChartAreaTopColor = Color(0xFFFF9800).copy(alpha = 0.5f)
private val ChartAreaBottomColor = Color(0xFFFF9800).copy(alpha = 0f)
private val GridLineColor = Color.Gray.copy(alpha = 0.3f)
private val SelectedPeriodBackground = Color(0xFF1976D2)

/** 고점/저점 peak 마커 색 (iOS SwiftUIChartView.peakMarkerColor = .red 대칭). */
private val PeakMarkerColor = Color(0xFFFF3B30)

// MARK: - Chart Screen

@Composable
fun ChartScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val insightViewModel: InsightViewModel = hiltViewModel()
    val insightState by insightViewModel.uiState.collectAsState()

    // InsightViewModel이 HomeViewModel을 관찰
    LaunchedEffect(Unit) {
        insightViewModel.observeHome(viewModel, InsightIndexScope.CHART)
    }

    val context = LocalContext.current
    val analytics = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, AnalyticsEntryPoint::class.java)
            .analyticsManager()
    }

    val selectedType = uiState.selectedChartType

    val currentState = when (selectedType) {
        FearIndexType.MARKET -> uiState.marketState
        FearIndexType.KOSPI -> uiState.kospiState
        FearIndexType.CRYPTO -> uiState.cryptoState
    }

    // 기간 선택 상태는 ViewModel의 marketHistoryDays/cryptoHistoryDays에서 역산 (SSOT)
    // 탭 재진입 시 로컬 remember가 리셋되어도 ViewModel은 살아있어 UI/데이터 일치 유지
    val currentMarketPeriod = ChartPeriod.fromDays(uiState.marketHistoryDays)
    val currentKospiPeriod = ChartPeriod.fromDays(uiState.kospiHistoryDays)
    val currentCryptoPeriod = CryptoChartPeriod.fromDays(uiState.cryptoHistoryDays)

    // 인사이트 상세 BottomSheet
    insightState.selectedInsight?.let { insight ->
        InsightDetailSheet(
            insight = insight,
            onDismiss = insightViewModel::dismissDetail,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. Title
        Text(
            text = stringResource(R.string.tab_chart),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Segment Picker
        SegmentedPicker(
            items = listOf(
                stringResource(R.string.tab_market),
                stringResource(R.string.tab_kospi),
                stringResource(R.string.tab_crypto),
            ),
            selectedIndex = when (selectedType) {
                FearIndexType.MARKET -> 0
                FearIndexType.KOSPI -> 1
                FearIndexType.CRYPTO -> 2
            },
            onItemSelected = { index ->
                val newType = when (index) {
                    0 -> FearIndexType.MARKET
                    1 -> FearIndexType.KOSPI
                    else -> FearIndexType.CRYPTO
                }
                if (newType != selectedType) {
                    analytics.log(
                        AnalyticsEvent.인사이트세그먼트전환(
                            이전타입 = selectedType.analyticsLabel(),
                            변경타입 = newType.analyticsLabel(),
                        ),
                    )
                }
                viewModel.selectChartIndexType(newType)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3-6. Content based on state
        when (currentState) {
            is FearIndexState.Loading -> {
                ChartSkeletonView()
            }
            is FearIndexState.Loaded -> {
                ChartLoadedContent(
                    fearIndex = currentState.fearIndex,
                    isCrypto = selectedType == FearIndexType.CRYPTO,
                    selectedMarketPeriod = if (selectedType == FearIndexType.KOSPI) {
                        currentKospiPeriod
                    } else {
                        currentMarketPeriod
                    },
                    selectedCryptoPeriod = currentCryptoPeriod,
                    history = when (selectedType) {
                        FearIndexType.MARKET -> uiState.marketHistory
                        FearIndexType.KOSPI -> uiState.kospiHistory
                        FearIndexType.CRYPTO -> uiState.cryptoHistory
                    },
                    isHistoryLoading = when (selectedType) {
                        FearIndexType.MARKET -> uiState.isMarketHistoryLoading
                        FearIndexType.KOSPI -> uiState.isKospiHistoryLoading
                        FearIndexType.CRYPTO -> uiState.isCryptoHistoryLoading
                    },
                    onMarketPeriodSelected = { period ->
                        if (selectedType == FearIndexType.KOSPI) {
                            viewModel.loadKospiHistoryForDays(period.days)
                        } else {
                            viewModel.loadMarketHistoryForDays(period.days)
                        }
                        analytics.log(AnalyticsEvent.차트기간선택(기간 = period.label))
                    },
                    onCryptoPeriodSelected = { period ->
                        viewModel.loadCryptoHistoryForDays(period.days)
                        analytics.log(AnalyticsEvent.암호화폐차트조회(기간 = period.label))
                    },
                    indexType = selectedType,
                    kospiIsFinal = uiState.kospiSnapshot?.isFinal,
                    insights = insightState.insights,
                    onInsightClick = insightViewModel::selectInsight,
                    onCardViewed = insightViewModel::logCardViewed,
                )
            }
            is FearIndexState.Error -> {
                Text(
                    text = currentState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 80.dp),
                )
            }
        }
    }
}

private fun FearIndexType.analyticsLabel(): String = when (this) {
    FearIndexType.MARKET -> ANALYTICS_TYPE_MARKET
    FearIndexType.KOSPI -> ANALYTICS_TYPE_KOSPI
    FearIndexType.CRYPTO -> ANALYTICS_TYPE_CRYPTO
}

// MARK: - Loaded Content

@Composable
private fun ChartLoadedContent(
    fearIndex: FearIndex,
    isCrypto: Boolean,
    selectedMarketPeriod: ChartPeriod,
    selectedCryptoPeriod: CryptoChartPeriod,
    history: List<FearIndex>,
    isHistoryLoading: Boolean,
    onMarketPeriodSelected: (ChartPeriod) -> Unit,
    onCryptoPeriodSelected: (CryptoChartPeriod) -> Unit,
    indexType: FearIndexType = FearIndexType.MARKET,
    kospiIsFinal: Boolean? = null,
    insights: List<MarketInsight> = emptyList(),
    onInsightClick: (MarketInsight) -> Unit = {},
    onCardViewed: (MarketInsight) -> Unit = {},
) {
    val selectedDays = if (isCrypto) selectedCryptoPeriod.days else selectedMarketPeriod.days
    val chartData = remember(history, selectedDays) {
        ChartDataFilter.filter(
            data = history,
            days = selectedDays,
            maxSamplePoints = chartMaxSamplePoints(selectedDays),
        )
    }

    // 3. Current Score Section
    CurrentScoreCard(
        fearIndex = fearIndex,
        indexType = indexType,
        kospiIsFinal = kospiIsFinal,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 4. History header
    Text(
        text = stringResource(R.string.chart_history),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )

    // 5. Chart Canvas
    if (chartData.isNotEmpty() && !isHistoryLoading) {
        ChartCard(
            data = chartData,
            isCrypto = isCrypto,
            selectedMarketPeriod = selectedMarketPeriod,
            selectedCryptoPeriod = selectedCryptoPeriod,
        )
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    strokeWidth = 2.dp,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 6. Period Selector
    if (isCrypto) {
        CryptoPeriodSelector(
            selected = selectedCryptoPeriod,
            onSelect = onCryptoPeriodSelected,
        )
    } else {
        MarketPeriodSelector(
            selected = selectedMarketPeriod,
            onSelect = onMarketPeriodSelected,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 6.5. AdMob 배너 광고 (ChartBanner 단위) — 기간 버튼 아래
    AdBanner(
        adUnitId = BuildConfig.ADMOB_BANNER_CHART,
        screenName = "차트",
    )

    // 7. Insight Feed (iOS 차트 탭 순서: 기간 버튼 아래 → 인사이트 피드)
    if (insights.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        InsightFeedView(
            insights = insights,
            indexType = indexType,
            onInsightClick = onInsightClick,
            onCardViewed = onCardViewed,
        )
    }
}

private fun chartMaxSamplePoints(days: Int): Int? = when {
    days >= ChartPeriod.FIVE_YEARS.days -> 520
    days >= ChartPeriod.THREE_YEARS.days -> 390
    days >= ChartPeriod.TWO_YEARS.days -> 260
    else -> null
}

// MARK: - Current Score Card

@Composable
private fun CurrentScoreCard(
    fearIndex: FearIndex,
    indexType: FearIndexType = FearIndexType.MARKET,
    kospiIsFinal: Boolean? = null,
) {
    val score = fearIndex.roundedScore
    val scoreColor = fearScoreColor(score)
    val change = fearIndex.score - (fearIndex.previousClose ?: fearIndex.score)
    val changeColor = when {
        change > 0 -> Color(0xFF4CAF50)
        change < 0 -> Color(0xFFE53935)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val changePrefix = when {
        change > 0 -> "+ "
        change < 0 -> "- "
        else -> ""
    }
    val changeText = "${changePrefix}${"%.1f".format(kotlin.math.abs(change))}"

    // 대표 기준 설명 시트 (iOS RepresentativeIndexInfoSheet 대칭)
    var showInfoSheet by remember { mutableStateOf(false) }
    if (showInfoSheet) {
        RepresentativeIndexInfoSheet(onDismiss = { showInfoSheet = false })
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: 현재 지수
            Column {
                // "현재 지수" 라벨 + ⓘ 버튼 (iOS currentScoreTitleRow 대칭)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.current_index_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(
                        onClick = { showInfoSheet = true },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.representative_index_info_accessibility),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "$score",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = ratingLabel(score),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scoreColor,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                // KOSPI 장 상태줄 (iOS kospiCurrentStatusLine 대칭): KOSPI일 때만.
                if (indexType == FearIndexType.KOSPI) {
                    val statusText = stringResource(
                        if (kospiIsFinal == true) R.string.kospi_status_final
                        else R.string.kospi_status_intraday,
                    )
                    val updatedLabel = stringResource(R.string.kospi_current_updated_at)
                    val updatedAt = IndexTimestampFormatter.format(fearIndex.timestamp, indexType)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$statusText · $updatedLabel: $updatedAt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }

            // Right: 전일 대비
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.vs_previous_close_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = changeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = changeColor,
                )
            }
        }
    }
}

// MARK: - Chart Card

@Composable
private fun ChartCard(
    data: List<FearIndex>,
    isCrypto: Boolean,
    selectedMarketPeriod: ChartPeriod,
    selectedCryptoPeriod: CryptoChartPeriod,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline
    val tooltipBgColor = MaterialTheme.colorScheme.surfaceVariant
    val haptic = LocalHapticFeedback.current

    // 다국어 rating 문자열 (Canvas draw에서 stringResource 호출 불가하므로 미리 로드)
    val ratingLabels = arrayOf(
        stringResource(R.string.rating_extreme_fear),
        stringResource(R.string.rating_fear),
        stringResource(R.string.rating_neutral),
        stringResource(R.string.rating_greed),
        stringResource(R.string.rating_extreme_greed),
    )

    // 선택된 포인트 인덱스 (null이면 선택 없음)
    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }
    var touchX by remember(data) { mutableStateOf(0f) }

    // 고점/저점 peak 계산 — data 변경 시에만 재계산 (iOS cachedPeaks 대칭).
    // data 는 이미 period 필터링된 차트 데이터라 라인과 index 가 정확히 일치한다.
    val peaks = remember(data) { ComputeFearIndexPeaks().invoke(data) }

    // 기간이 바뀌면 선택 해제
    LaunchedEffect(selectedMarketPeriod, selectedCryptoPeriod, isCrypto) {
        selectedIndex = null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(top = 16.dp, bottom = 24.dp, start = 8.dp, end = 40.dp)
                .pointerInput(data) {
                    if (data.isEmpty()) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // 첫 터치 — 가장 가까운 포인트 찾기
                        val initialIndex = ChartDataFilter.nearestIndex(down.position.x, size.width.toFloat(), data.size)
                        if (initialIndex != selectedIndex) {
                            selectedIndex = initialIndex
                            touchX = down.position.x
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else {
                            touchX = down.position.x
                        }
                        down.consume()

                        // 드래그 추적
                        while (true) {
                            val event = awaitPointerEventOrNull() ?: break
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val newIndex = ChartDataFilter.nearestIndex(change.position.x, size.width.toFloat(), data.size)
                            touchX = change.position.x
                            if (newIndex != selectedIndex) {
                                selectedIndex = newIndex
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            change.consume()
                        }
                    }
                },
        ) {
            drawChart(
                data = data,
                textMeasurer = textMeasurer,
                labelColor = labelColor,
                isCrypto = isCrypto,
                selectedMarketPeriod = selectedMarketPeriod,
                selectedCryptoPeriod = selectedCryptoPeriod,
            )

            // 고점/저점 peak 마커 (빨간 점 + 점수 라벨). 선택 인디케이터보다 먼저 그려
            // 드래그 툴팁이 항상 위에 오도록 한다.
            if (peaks != null) {
                drawPeakMarkers(
                    data = data,
                    high = peaks.first,
                    low = peaks.second,
                    textMeasurer = textMeasurer,
                )
            }

            // 선택 인디케이터 (수직선 + 점 + 툴팁)
            val sel = selectedIndex
            if (sel != null && sel in data.indices) {
                drawSelectionIndicator(
                    data = data,
                    selectedIndex = sel,
                    isCrypto = isCrypto,
                    ratingLabels = ratingLabels,
                    marketPeriod = selectedMarketPeriod,
                    cryptoPeriod = selectedCryptoPeriod,
                    textMeasurer = textMeasurer,
                    lineColor = outlineColor,
                    textColor = onSurfaceColor,
                    tooltipBg = tooltipBgColor,
                )
            }
        }
    }
}

/**
 * Compose 1.x에는 awaitPointerEvent() 확장이 suspend 블록에서 제공됨.
 * null을 반환하는 래퍼는 손가락 뗌 감지를 위한 안전장치.
 */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitPointerEventOrNull() =
    try {
        awaitPointerEvent()
    } catch (e: Exception) {
        null
    }

// MARK: - Peak Markers (고점/저점)

/**
 * 차트 위 고점/저점 마커. 빨간 점 + 점수 라벨.
 * iOS `SwiftUIChartView.peakMarks` 와 시각 대칭:
 * - high == low (단일 포인트) 인 경우 `.high` 만 그려 중복 방지.
 * - high 라벨은 점 위, low 라벨은 점 아래 배치가 기본.
 * - high.score > 85 (Y축 상단 근접) / low.score < 10 (X축 근접) 시 라벨을 점 옆(좌/우)으로.
 *
 * peak.index 가 라인 데이터 인덱스와 동일하므로 X 좌표는 라인과 정확히 일치한다.
 */
private fun DrawScope.drawPeakMarkers(
    data: List<FearIndex>,
    high: FearIndexPeak,
    low: FearIndexPeak,
    textMeasurer: TextMeasurer,
) {
    val isSinglePoint = high.index == low.index
    drawPeakMarker(data, high, textMeasurer)
    if (!isSinglePoint) {
        drawPeakMarker(data, low, textMeasurer)
    }
}

private fun DrawScope.drawPeakMarker(
    data: List<FearIndex>,
    peak: FearIndexPeak,
    textMeasurer: TextMeasurer,
) {
    val chartWidth = size.width
    val chartHeight = size.height
    // 라인과 동일 수식 → peak.index 의 라인 점과 1px 오차 없이 일치.
    val x = ChartCoordinates.xForIndex(peak.index, data.size, chartWidth)
    val y = ChartCoordinates.yForScore(peak.score, chartHeight)

    // 빨간 점
    drawCircle(
        color = PeakMarkerColor,
        radius = 3.5.dp.toPx(),
        center = Offset(x, y),
    )

    // 점수 라벨
    val labelText = peakScoreText(peak.score)
    val textResult = textMeasurer.measure(
        text = labelText,
        style = TextStyle(
            fontSize = 11.sp,
            color = PeakMarkerColor,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    val labelW = textResult.size.width
    val labelH = textResult.size.height
    val gap = 6.dp.toPx()
    val isHigh = peak.kind == FearIndexPeak.Kind.HIGH
    val isLeftHalf = x <= chartWidth / 2f

    // 라벨이 점 옆(좌/우)으로 가야 하는 경계 케이스:
    // - high: 점이 Y축 상단에 붙어 위쪽 공간이 없을 때 (score > 85)
    // - low: 점이 X축 라벨과 겹칠 만큼 아래에 붙을 때 (score < 10)
    val placeSide = if (isHigh) peak.score > 85 else peak.score < 10

    val labelX: Float
    val labelY: Float
    if (placeSide) {
        // 왼쪽 절반 peak → 라벨을 오른쪽에, 오른쪽 절반 → 왼쪽에 (차트 밖으로 안 나가게)
        labelX = if (isLeftHalf) x + gap else x - gap - labelW
        labelY = y - labelH / 2f
    } else {
        // high → 점 위, low → 점 아래
        labelX = x - labelW / 2f
        labelY = if (isHigh) y - gap - labelH else y + gap
    }

    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            x = labelX.coerceIn(0f, (chartWidth - labelW).coerceAtLeast(0f)),
            y = labelY.coerceIn(0f, (chartHeight - labelH).coerceAtLeast(0f)),
        ),
    )
}

/** 점수 포맷 — 정수는 "82", 소수 한 자리는 "2.9" (서양 숫자). iOS peakScoreText 대칭. */
private fun peakScoreText(score: Double): String {
    val tenths = (score * 10).roundToInt()
    return if (tenths % 10 == 0) {
        (tenths / 10).toString()
    } else {
        // 소수 한 자리, 항상 '.' 구분자 (en_US_POSIX 동등). 도메인 score 는 0..100.
        "${tenths / 10}.${tenths % 10}"
    }
}

private fun DrawScope.drawSelectionIndicator(
    data: List<FearIndex>,
    selectedIndex: Int,
    isCrypto: Boolean,
    ratingLabels: Array<String>,
    marketPeriod: ChartPeriod,
    cryptoPeriod: CryptoChartPeriod,
    textMeasurer: TextMeasurer,
    lineColor: Color,
    textColor: Color,
    tooltipBg: Color,
) {
    val chartWidth = size.width
    val chartHeight = size.height
    val point = data[selectedIndex]

    // 선택 포인트의 X, Y 좌표 — 라인/peak 과 동일 수식.
    val x = ChartCoordinates.xForIndex(selectedIndex, data.size, chartWidth)
    val y = ChartCoordinates.yForScore(point.score, chartHeight)

    // 1. 수직선
    drawLine(
        color = lineColor.copy(alpha = 0.6f),
        start = Offset(x, 0f),
        end = Offset(x, chartHeight),
        strokeWidth = 1.dp.toPx(),
    )

    // 2. 선택된 점 (원)
    drawCircle(
        color = ChartLineColor,
        radius = 5.dp.toPx(),
        center = Offset(x, y),
    )
    drawCircle(
        color = Color.White,
        radius = 2.5.dp.toPx(),
        center = Offset(x, y),
    )

    // 3. 툴팁 (점수 + 등급 + 날짜)
    val score = point.score.roundToInt()
    val scoreText = "$score"
    val ratingText = ratingLabelFromArray(score, ratingLabels)
    val days = if (isCrypto) cryptoPeriod.days else marketPeriod.days
    val dateFormatter = tooltipDateFormatter(days)
    val dateText = dateFormatter.format(point.timestamp)

    val scoreLayout = textMeasurer.measure(
        text = scoreText,
        style = TextStyle(fontSize = 14.sp, color = fearScoreColor(score), fontWeight = FontWeight.Bold),
    )
    val ratingLayout = textMeasurer.measure(
        text = ratingText,
        style = TextStyle(fontSize = 10.sp, color = textColor.copy(alpha = 0.75f)),
    )
    val dateLayout = textMeasurer.measure(
        text = dateText,
        style = TextStyle(fontSize = 9.sp, color = textColor.copy(alpha = 0.5f)),
    )

    val padding = 8.dp.toPx()
    val tooltipWidth = maxOf(scoreLayout.size.width, ratingLayout.size.width, dateLayout.size.width) + padding * 2
    val tooltipHeight = scoreLayout.size.height + ratingLayout.size.height + dateLayout.size.height + padding * 2 + 4.dp.toPx()

    // 툴팁 X 위치 (가장자리 클램프)
    val tooltipX = (x - tooltipWidth / 2f).coerceIn(0f, chartWidth - tooltipWidth)
    val tooltipY = 0f.coerceAtMost(y - tooltipHeight - 8.dp.toPx()).let {
        // 위로 표시하되, 상단 벗어나면 아래로
        if (y - tooltipHeight - 8.dp.toPx() < 0f) y + 16.dp.toPx() else y - tooltipHeight - 8.dp.toPx()
    }.coerceAtMost(chartHeight - tooltipHeight)

    // 툴팁 배경
    drawRoundRect(
        color = tooltipBg,
        topLeft = Offset(tooltipX, tooltipY),
        size = Size(tooltipWidth, tooltipHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
    )

    // 텍스트 그리기 (중앙 정렬)
    var textY = tooltipY + padding
    drawText(
        textLayoutResult = scoreLayout,
        topLeft = Offset(tooltipX + (tooltipWidth - scoreLayout.size.width) / 2f, textY),
    )
    textY += scoreLayout.size.height + 2.dp.toPx()
    drawText(
        textLayoutResult = ratingLayout,
        topLeft = Offset(tooltipX + (tooltipWidth - ratingLayout.size.width) / 2f, textY),
    )
    textY += ratingLayout.size.height + 2.dp.toPx()
    drawText(
        textLayoutResult = dateLayout,
        topLeft = Offset(tooltipX + (tooltipWidth - dateLayout.size.width) / 2f, textY),
    )
}

private fun tooltipDateFormatter(days: Int): DateTimeFormatter {
    val pattern = when {
        days <= 90 -> "yyyy/M/d"
        else -> "yyyy/M/d"
    }
    return DateTimeFormatter.ofPattern(pattern)
        .withZone(ZoneId.of("America/New_York"))
}

// MARK: - Canvas Drawing

private fun DrawScope.drawChart(
    data: List<FearIndex>,
    textMeasurer: TextMeasurer,
    labelColor: Color,
    isCrypto: Boolean,
    selectedMarketPeriod: ChartPeriod,
    selectedCryptoPeriod: CryptoChartPeriod,
) {
    if (data.isEmpty()) return

    val chartWidth = size.width
    val chartHeight = size.height

    // Y-axis grid lines and labels at 0, 25, 50, 75, 100
    val yValues = listOf(0, 25, 50, 75, 100)
    yValues.forEach { value ->
        val y = chartHeight * (1f - value / 100f)

        // Grid line
        if (value != 0 && value != 100) {
            drawLine(
                color = GridLineColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 0.5.dp.toPx(),
            )
        }

        // Y-axis label (right side, outside the chart area)
        val labelText = "$value"
        val textResult = textMeasurer.measure(
            text = labelText,
            style = TextStyle(
                fontSize = 10.sp,
                color = labelColor,
            ),
        )
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(
                x = chartWidth + 4.dp.toPx(),
                y = y - textResult.size.height / 2f,
            ),
        )
    }

    // Build data points — 라인/peak/선택점이 동일 수식을 공유하도록 ChartCoordinates 사용.
    val points = data.mapIndexed { i, item ->
        Offset(
            ChartCoordinates.xForIndex(i, data.size, chartWidth),
            ChartCoordinates.yForScore(item.score, chartHeight),
        )
    }

    // Catmull-Rom spline interpolation
    val splinePoints = catmullRomSpline(points, segments = 8)

    // Area fill (gradient from orange to transparent)
    val areaPath = Path().apply {
        if (splinePoints.isNotEmpty()) {
            moveTo(splinePoints.first().x, chartHeight)
            splinePoints.forEach { lineTo(it.x, it.y) }
            lineTo(splinePoints.last().x, chartHeight)
            close()
        }
    }

    drawPath(
        path = areaPath,
        brush = Brush.verticalGradient(
            colors = listOf(ChartAreaTopColor, ChartAreaBottomColor),
            startY = 0f,
            endY = chartHeight,
        ),
    )

    // Line
    val linePath = Path().apply {
        splinePoints.forEachIndexed { i, point ->
            if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }

    drawPath(
        path = linePath,
        color = ChartLineColor,
        style = Stroke(width = 2.dp.toPx()),
    )

    // X-axis date labels
    drawXAxisLabels(
        data = data,
        chartWidth = chartWidth,
        chartHeight = chartHeight,
        textMeasurer = textMeasurer,
        labelColor = labelColor,
        isCrypto = isCrypto,
        selectedMarketPeriod = selectedMarketPeriod,
        selectedCryptoPeriod = selectedCryptoPeriod,
    )
}

// MARK: - X-Axis Labels

private fun DrawScope.drawXAxisLabels(
    data: List<FearIndex>,
    chartWidth: Float,
    chartHeight: Float,
    textMeasurer: TextMeasurer,
    labelColor: Color,
    isCrypto: Boolean,
    selectedMarketPeriod: ChartPeriod,
    selectedCryptoPeriod: CryptoChartPeriod,
) {
    if (data.size < 2) return

    val targetLabelCount = 5
    val step = (data.size - 1) / targetLabelCount.coerceAtLeast(1)
    if (step < 1) return

    val days = if (isCrypto) selectedCryptoPeriod.days else selectedMarketPeriod.days
    val formatter = xAxisFormatter(days)

    for (i in 0..targetLabelCount) {
        val dataIndex = (i * step).coerceAtMost(data.size - 1)
        val x = chartWidth * dataIndex / (data.size - 1).coerceAtLeast(1)
        val dateStr = formatter.format(data[dataIndex].timestamp)

        val textResult = textMeasurer.measure(
            text = dateStr,
            style = TextStyle(fontSize = 10.sp, color = labelColor),
        )

        val labelX = (x - textResult.size.width / 2f)
            .coerceIn(0f, chartWidth - textResult.size.width)

        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(labelX, chartHeight + 4.dp.toPx()),
        )
    }
}

private fun xAxisFormatter(days: Int): DateTimeFormatter {
    val pattern = when {
        days <= 14 -> "M/d"           // 1W: 10/8, 10/15
        days <= 90 -> "M/d"           // 1M, 3M: 9/1, 10/1
        days <= 365 -> "M/d"          // 6M, 1Y: 월/일
        else -> "yyyy/M"              // 2Y+: 2024/1, 2025/4 (연도 구분 명확)
    }
    return DateTimeFormatter.ofPattern(pattern)
        .withZone(ZoneId.of("America/New_York"))
}

// MARK: - Catmull-Rom Spline

private fun catmullRomSpline(
    points: List<Offset>,
    segments: Int = 8,
): List<Offset> {
    if (points.size < 2) return points
    if (points.size == 2) return points

    val result = mutableListOf<Offset>()

    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]

        for (t in 0 until segments) {
            val frac = t.toFloat() / segments
            val x = catmullRomValue(p0.x, p1.x, p2.x, p3.x, frac)
            val y = catmullRomValue(p0.y, p1.y, p2.y, p3.y, frac)
            result.add(Offset(x, y))
        }
    }

    // Add the last point
    result.add(points.last())
    return result
}

private fun catmullRomValue(
    v0: Float,
    v1: Float,
    v2: Float,
    v3: Float,
    t: Float,
): Float {
    val t2 = t * t
    val t3 = t2 * t
    return 0.5f * (
        (2f * v1) +
            (-v0 + v2) * t +
            (2f * v0 - 5f * v1 + 4f * v2 - v3) * t2 +
            (-v0 + 3f * v1 - 3f * v2 + v3) * t3
        )
}

// MARK: - Market Period Selector

@Composable
private fun MarketPeriodSelector(
    selected: ChartPeriod,
    onSelect: (ChartPeriod) -> Unit,
) {
    PeriodSelectorRow(
        labels = ChartPeriod.entries.map { it.label },
        selectedIndex = ChartPeriod.entries.indexOf(selected),
        onSelect = { index -> onSelect(ChartPeriod.entries[index]) },
    )
}

// MARK: - Crypto Period Selector

@Composable
private fun CryptoPeriodSelector(
    selected: CryptoChartPeriod,
    onSelect: (CryptoChartPeriod) -> Unit,
) {
    PeriodSelectorRow(
        labels = CryptoChartPeriod.entries.map { it.label },
        selectedIndex = CryptoChartPeriod.entries.indexOf(selected),
        onSelect = { index -> onSelect(CryptoChartPeriod.entries[index]) },
    )
}

// MARK: - Period Selector Row

@Composable
private fun PeriodSelectorRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) SelectedPeriodBackground else Color.Transparent,
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// MARK: - Rating Label

/**
 * Canvas drawSelectionIndicator 내부용 — stringResource를 쓸 수 없으므로
 * 상위 Composable에서 미리 로드한 배열을 받아 등급 문자열 선택.
 */
private fun ratingLabelFromArray(score: Int, labels: Array<String>): String = when {
    score <= 24 -> labels[0]
    score <= 44 -> labels[1]
    score <= 55 -> labels[2]
    score <= 75 -> labels[3]
    else -> labels[4]
}
