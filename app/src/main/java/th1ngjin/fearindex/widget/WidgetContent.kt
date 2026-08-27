package th1ngjin.fearindex.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.Image
import androidx.glance.layout.ContentScale
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import th1ngjin.fearindex.MainActivity
import th1ngjin.fearindex.domain.entity.FearIndexType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * 위젯 리디자인 (2026-08-27, docs/design/widget-redesign-mock-v2.html 확정 시안):
 * 다크 카드 + iOS식 270° 아크 게이지 + 풀네임(Global/KOSPI/Crypto) + 전일변화 + ↻ 새로고침.
 */

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun cardModifier(radius: Int) = GlanceModifier
    .fillMaxSize()
    .cornerRadius(radius.dp)
    .background(ColorProvider(WidgetCardBackground))
    .clickable(actionStartActivity<MainActivity>())

/** 단일 지수 — 1×1(기본): 게이지 + 점수 + 풀네임. One UI 셀은 세로가 길어 짧은 변 정사각으로 중앙 배치. */
@Composable
fun CompactFearIndexWidgetContent(
    context: Context,
    indexType: FearIndexType,
    data: WidgetIndexData?,
) {
    val size = LocalSize.current
    val side = WidgetLayoutMode.squareCardSideDp(size.width.value, size.height.value)
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SquareCompactCard(indexType, data, side)
    }
}

@Composable
private fun SquareCompactCard(indexType: FearIndexType, data: WidgetIndexData?, sideDp: Float) {
    Column(
        modifier = GlanceModifier
            .size(sideDp.dp)
            .cornerRadius(16.dp)
            .background(ColorProvider(WidgetCardBackground))
            .clickable(actionStartActivity<MainActivity>())
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GaugeWithScore(data, gaugePx = 220, scoreSp = 19, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
        Text(
            text = WidgetGaugeSpec.indexName(indexType),
            maxLines = 1,
            style = TextStyle(color = ColorProvider(WidgetTextColorDim), fontSize = 9.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
        )
    }
}

/** 단일 지수 — 확대(2×2 이상): 게이지 + 풀네임 + 등급·전일변화. */
@Composable
fun FearIndexWidgetContent(
    context: Context,
    indexType: FearIndexType,
    data: WidgetIndexData?,
) {
    Column(
        modifier = cardModifier(20).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GaugeWithScore(data, gaugePx = 360, scoreSp = 30, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
        Text(
            text = WidgetGaugeSpec.indexName(indexType),
            maxLines = 1,
            style = TextStyle(color = ColorProvider(WidgetTextColorDim), fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
        )
        Spacer(GlanceModifier.height(2.dp))
        RatingChangeRow(context, data, fontSp = 12, dotDp = 8)
    }
}

/**
 * 통합(대시보드) — 리사이즈에 따라 3모드 (2026-08-27 사용자 요청: 어느 크기든 글자 안 잘리게).
 * ROW(낮음): 게이지+이름+등급 3개 가로 / COLUMN(좁음): 같은 셀 세로 스택 / LIST(넉넉): [게이지|이름/등급] 행.
 */
@Composable
fun CombinedWidgetContent(
    context: Context,
    market: WidgetIndexData?,
    kospi: WidgetIndexData?,
    crypto: WidgetIndexData?,
    large: Boolean,
    refreshing: Boolean = false,
) {
    val size = LocalSize.current
    val entries = listOf(
        FearIndexType.MARKET to market,
        FearIndexType.KOSPI to kospi,
        FearIndexType.CRYPTO to crypto,
    )
    when (WidgetLayoutMode.dashboardArrangement(size.width.value, size.height.value)) {
        DashboardArrangement.ROW -> CombinedRowLayout(context, entries, size.width.value, size.height.value, refreshing)
        DashboardArrangement.LIST -> CombinedListLayout(context, entries, size.width.value, size.height.value, refreshing)
    }
}

/** ROW(낮음): 게이지 3개 가로 — 각 게이지 아래 이름(+높이 되면 등급). */
@Composable
private fun CombinedRowLayout(
    context: Context,
    entries: List<Pair<FearIndexType, WidgetIndexData?>>,
    widthDp: Float,
    heightDp: Float,
    refreshing: Boolean,
) {
    val showRating = WidgetLayoutMode.rowModeShowsRating(heightDp)
    // ⚠️ Glance 는 weight 안에 또 weight 를 중첩하면 안쪽이 0 으로 붕괴한다(게이지 소실) → 고정 크기
    // 헤더 실측: ↻(15sp≈20dp) + 상하 패딩 16dp ≈ 36dp — 과소 잡으면 하단 이름이 잘린다
    val textBlockDp = if (showRating) 30f else 16f
    val gaugeDp = minOf(heightDp - 36f - textBlockDp - 14f, widthDp / 3f - 14f).coerceIn(24f, 72f)
    Box(modifier = cardModifier(20)) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
            RefreshHeader(context, showTime = widthDp >= 200f, refreshing = refreshing)
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                entries.forEach { (type, data) ->
                    Column(
                        // Row 자식: weight(폭) + fillMaxHeight — fillMaxSize 겹치면 weight 무시(73번 함정)
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GaugeWithScore(data, gaugePx = 200, scoreSp = (gaugeDp * 0.32f).toInt().coerceIn(10, 18), modifier = GlanceModifier.size(gaugeDp.dp))
                        Text(
                            text = WidgetGaugeSpec.indexName(type),
                            maxLines = 1,
                            style = TextStyle(color = ColorProvider(WidgetTextColor), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        )
                        if (showRating && data != null) {
                            Text(
                                text = ratingLabel(context, data.rating),
                                maxLines = 1,
                                style = TextStyle(color = ColorProvider(widgetFearScoreColor(data.rating)), fontSize = 9.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** LIST: 행마다 [게이지+점수 | 이름/등급]. 좁은 폭은 게이지·글씨를 축소해 잘림 없이 수용. */
@Composable
private fun CombinedListLayout(
    context: Context,
    entries: List<Pair<FearIndexType, WidgetIndexData?>>,
    widthDp: Float,
    heightDp: Float,
    refreshing: Boolean,
) {
    val narrow = widthDp < 170f
    // 행 높이(3행 균등)와 폭 양쪽에 맞춰 게이지 정사각 — 좁으면 텍스트 폭을 남기려 더 줄인다
    val rowHeightDp = (heightDp - 42f) / 3f
    val rowGaugeDp = minOf(rowHeightDp, widthDp * if (narrow) 0.26f else 0.40f).coerceIn(24f, 64f)
    // 행이 낮으면 이름/등급 두 줄이 안 들어간다 → 등급을 이름 옆 한 줄로
    val twoLine = rowHeightDp >= 34f
    val nameSp = if (narrow) 11 else 13
    val ratingSp = if (narrow) 9 else 11
    Box(modifier = cardModifier(20)) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            RefreshHeader(context, showTime = true, refreshing = refreshing) // 헤더 행 공백 활용 — 좁아도 "HH:mm 기준"은 들어간다
            entries.forEach { (type, data) ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GaugeWithScore(
                        data,
                        gaugePx = 200,
                        scoreSp = (rowGaugeDp * 0.30f).toInt().coerceAtLeast(10),
                        modifier = GlanceModifier.size(rowGaugeDp.dp),
                    )
                    Spacer(GlanceModifier.width(if (narrow) 6.dp else 10.dp))
                    if (twoLine) {
                        Column(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = WidgetGaugeSpec.indexName(type),
                                maxLines = 1,
                                style = TextStyle(color = ColorProvider(WidgetTextColor), fontSize = nameSp.sp, fontWeight = FontWeight.Bold),
                            )
                            if (data != null) {
                                val ratingColor = widgetFearScoreColor(data.rating)
                                Text(
                                    text = ratingLabel(context, data.rating),
                                    maxLines = 2, // l10n 긴 등급명("Extreme Greed" 등)도 말줄임 없이 줄바꿈
                                    style = TextStyle(color = ColorProvider(ratingColor), fontSize = ratingSp.sp, fontWeight = FontWeight.Medium),
                                )
                            }
                        }
                    } else {
                        Text(
                            text = WidgetGaugeSpec.indexName(type),
                            maxLines = 1,
                            style = TextStyle(color = ColorProvider(WidgetTextColor), fontSize = nameSp.sp, fontWeight = FontWeight.Bold),
                        )
                        if (data != null) {
                            Spacer(GlanceModifier.width(8.dp))
                            Text(
                                text = ratingLabel(context, data.rating),
                                maxLines = 1,
                                style = TextStyle(color = ColorProvider(widgetFearScoreColor(data.rating)), fontSize = ratingSp.sp, fontWeight = FontWeight.Medium),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 상단 헤더 — 우측 정렬 [갱신시각] [↻]. 새로고침 중엔 ↻ 자리가 스피너로 바뀐다 (구글 위젯식). */
@Composable
private fun RefreshHeader(context: Context, showTime: Boolean, refreshing: Boolean) {
    Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showTime) {
                Text(
                    text = context.getString(th1ngjin.fearindex.presentation.R.string.widget_updated_at, LocalTime.now().format(timeFormat)),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(WidgetChangeFlatColor), fontSize = 9.sp),
                )
            }
            RefreshGlyph(refreshing, large = true)
        }
    }
}

/** ↻ 또는 진행 스피너. ↻ 는 패딩 포함 실터치 영역 ≈44dp. */
@Composable
private fun RefreshGlyph(refreshing: Boolean, large: Boolean) {
    if (refreshing) {
        Box(modifier = GlanceModifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            CircularProgressIndicator(
                modifier = GlanceModifier.size(if (large) 18.dp else 15.dp),
                color = ColorProvider(WidgetTextColorDim),
            )
        }
    } else {
        Text(
            text = "↻",
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsAction>()).padding(horizontal = 10.dp, vertical = 8.dp),
            style = TextStyle(color = ColorProvider(WidgetTextColorDim), fontSize = (if (large) 18 else 15).sp, fontWeight = FontWeight.Bold),
        )
    }
}

/** 차트 위젯 — 헤더(이름·점수·등급·변화) + 30일 라인차트 + ↻. */
@Composable
fun ChartWidgetContent(
    context: Context,
    indexType: FearIndexType,
    data: WidgetIndexData?,
    history: List<Int>,
    xLabels: List<String>,
    period: WidgetChartPeriod,
    large: Boolean,
    refreshing: Boolean = false,
) {
    val ratingColor = data?.let { widgetFearScoreColor(it.rating) } ?: WidgetPlaceholderColor
    Box(modifier = cardModifier(20)) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                // 이름 탭 = 지수 순환 전환 (Global → KOSPI → Crypto), ▾ 로 탭 가능함을 표시
                Text(
                    text = WidgetGaugeSpec.indexName(indexType) + " ▾",
                    maxLines = 1,
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<CycleChartIndexAction>())
                        .padding(top = 12.dp, bottom = 12.dp, end = 6.dp),
                    style = TextStyle(color = ColorProvider(WidgetTextColorDim), fontSize = (if (large) 11 else 9).sp, fontWeight = FontWeight.Medium),
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text = data?.score?.toString() ?: "—",
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(WidgetTextColor), fontSize = (if (large) 21 else 16).sp, fontWeight = FontWeight.Bold),
                )
                Spacer(GlanceModifier.defaultWeight())
                // 기간 세그먼트 — 탭하면 이 위젯만 해당 기간으로 재렌더 (2026-08-28, A안)
                WidgetChartPeriod.entries.forEach { p ->
                    val selected = p == period
                    Text(
                        text = p.label,
                        maxLines = 1,
                        modifier = GlanceModifier
                            .clickable(actionRunCallback<SetChartPeriodAction>(SetChartPeriodAction.params(p)))
                            .padding(horizontal = if (large) 8.dp else 6.dp, vertical = 12.dp),
                        style = TextStyle(
                            color = ColorProvider(if (selected) WidgetTextColor else WidgetChangeFlatColor),
                            fontSize = (if (large) 11 else 9).sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                }
                RefreshGlyph(refreshing, large = large)
            }
            Spacer(GlanceModifier.height(4.dp))
            if (history.size >= 2) {
                // 위젯 실측 크기(헤더·패딩 제외)에 맞춰 렌더 — Fit 스케일 letterbox 로 생기던 빈 공간 제거
                val widgetSize = LocalSize.current
                val density = context.resources.displayMetrics.density
                val chartWidthPx = ((widgetSize.width.value - 24f) * density).toInt().coerceAtLeast(120)
                val chartHeightPx = ((widgetSize.height.value - (if (large) 52f else 44f)) * density).toInt().coerceAtLeast(80)
                Image(
                    provider = ImageProvider(
                        WidgetChartRenderer.render(history, xLabels, chartWidthPx, chartHeightPx, ratingColor.toArgb()),
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                )
            } else {
                Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "—", style = TextStyle(color = ColorProvider(WidgetPlaceholderColor), fontSize = 18.sp))
                }
            }
        }
        UpdatedAt(context, large)
    }
}

// --- 공통 조각 ---

@Composable
private fun GaugeWithScore(data: WidgetIndexData?, gaugePx: Int, scoreSp: Int, modifier: GlanceModifier = GlanceModifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(WidgetGaugeRenderer.render(data?.score ?: 0, gaugePx)),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Text(
            text = data?.score?.toString() ?: "—",
            maxLines = 1,
            style = TextStyle(color = ColorProvider(WidgetTextColor), fontSize = scoreSp.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun RatingChangeRow(context: Context, data: WidgetIndexData?, fontSp: Int, dotDp: Int) {
    if (data == null) return
    val ratingColor = widgetFearScoreColor(data.rating)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = GlanceModifier.size(dotDp.dp).cornerRadius((dotDp / 2).dp).background(ColorProvider(ratingColor)),
        ) {}
        Spacer(GlanceModifier.width(3.dp))
        Text(
            text = ratingLabel(context, data.rating),
            maxLines = 1,
            style = TextStyle(color = ColorProvider(ratingColor), fontSize = fontSp.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(GlanceModifier.width(4.dp))
        ChangeText(data, fontSp)
    }
}

@Composable
private fun ChangeText(data: WidgetIndexData?, fontSp: Int) {
    val delta = data?.dailyChange ?: return
    val color = when {
        delta > 0 -> WidgetChangeUpColor
        delta < 0 -> WidgetChangeDownColor
        else -> WidgetChangeFlatColor
    }
    Text(
        text = "${WidgetGaugeSpec.changeGlyph(delta)} ${kotlin.math.abs(delta)}",
        maxLines = 1,
        style = TextStyle(color = ColorProvider(color), fontSize = fontSp.sp, fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun UpdatedAt(context: Context, large: Boolean) {
    Box(modifier = GlanceModifier.fillMaxSize().padding(bottom = 3.dp, start = 10.dp), contentAlignment = Alignment.BottomStart) {
        Text(
            text = context.getString(th1ngjin.fearindex.presentation.R.string.widget_updated_at, LocalTime.now().format(timeFormat)),
            style = TextStyle(color = ColorProvider(WidgetChangeFlatColor), fontSize = (if (large) 8 else 7).sp),
        )
    }
}
