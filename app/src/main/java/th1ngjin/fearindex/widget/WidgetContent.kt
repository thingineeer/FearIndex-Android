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
) {
    val size = LocalSize.current
    val entries = listOf(
        FearIndexType.MARKET to market,
        FearIndexType.KOSPI to kospi,
        FearIndexType.CRYPTO to crypto,
    )
    when (WidgetLayoutMode.dashboardArrangement(size.width.value, size.height.value)) {
        DashboardArrangement.ROW -> CombinedCellFlow(context, entries, horizontal = true, showTime = size.width.value >= 200f)
        DashboardArrangement.COLUMN -> CombinedCellFlow(context, entries, horizontal = false, showTime = true)
        DashboardArrangement.LIST -> CombinedListLayout(context, entries, size.height.value)
    }
}

/** ROW/COLUMN 공용 — 셀(게이지 위, 이름·등급 아래 중앙)을 가로 또는 세로로 나열. */
@Composable
private fun CombinedCellFlow(
    context: Context,
    entries: List<Pair<FearIndexType, WidgetIndexData?>>,
    horizontal: Boolean,
    showTime: Boolean,
) {
    Box(modifier = cardModifier(20)) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
            RefreshHeader(context, showTime = showTime)
            if (horizontal) {
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                    entries.forEach { (type, data) ->
                        // Glance: Row 자식에 fillMaxSize 겹치면 weight 가 무시된다 → weight+fillMaxHeight 만
                        CombinedCell(context, type, data, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                    }
                }
            } else {
                entries.forEach { (type, data) ->
                    CombinedCell(context, type, data, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun CombinedCell(
    context: Context,
    type: FearIndexType,
    data: WidgetIndexData?,
    modifier: GlanceModifier,
) {
    Column(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GaugeWithScore(data, gaugePx = 200, scoreSp = 14, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
        Text(
            text = WidgetGaugeSpec.indexName(type),
            maxLines = 1,
            style = TextStyle(color = ColorProvider(WidgetTextColor), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        )
        if (data != null) {
            Text(
                text = ratingLabel(context, data.rating),
                maxLines = 1,
                style = TextStyle(color = ColorProvider(widgetFearScoreColor(data.rating)), fontSize = 9.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
            )
        }
    }
}

/** 2×2+: 세로 리스트 — 행마다 [게이지+점수 | 이름/등급]. 등급은 잘리지 않게 2줄 허용. */
@Composable
private fun CombinedListLayout(
    context: Context,
    entries: List<Pair<FearIndexType, WidgetIndexData?>>,
    heightDp: Float,
) {
    // 헤더(~22dp)·패딩 제외 후 3행 균등 — 게이지는 행 높이에 딱 맞는 정사각
    val rowGaugeDp = ((heightDp - 38f) / 3f).coerceIn(30f, 64f)
    Box(modifier = cardModifier(20)) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
            RefreshHeader(context, showTime = true)
            entries.forEach { (type, data) ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GaugeWithScore(
                        data,
                        gaugePx = 200,
                        scoreSp = (rowGaugeDp * 0.30f).toInt().coerceAtLeast(11),
                        modifier = GlanceModifier.size(rowGaugeDp.dp),
                    )
                    Spacer(GlanceModifier.width(10.dp))
                    Column(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = WidgetGaugeSpec.indexName(type),
                            maxLines = 1,
                            style = TextStyle(color = ColorProvider(WidgetTextColor), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        )
                        if (data != null) {
                            val ratingColor = widgetFearScoreColor(data.rating)
                            Text(
                                text = ratingLabel(context, data.rating),
                                maxLines = 2, // l10n 긴 등급명("Extreme Greed" 등)도 말줄임 없이 줄바꿈
                                style = TextStyle(color = ColorProvider(ratingColor), fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 상단 헤더 — 우측 정렬 [갱신시각] [↻]. ↻ 는 패딩을 키워 실터치 영역 ≈40dp. */
@Composable
private fun RefreshHeader(context: Context, showTime: Boolean) {
    Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showTime) {
                Text(
                    text = context.getString(th1ngjin.fearindex.presentation.R.string.widget_updated_at, LocalTime.now().format(timeFormat)),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(WidgetChangeFlatColor), fontSize = 9.sp),
                )
            }
            Text(
                text = "↻",
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsAction>()).padding(horizontal = 10.dp, vertical = 8.dp),
                style = TextStyle(color = ColorProvider(WidgetTextColorDim), fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
        }
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
    large: Boolean,
) {
    val ratingColor = data?.let { widgetFearScoreColor(it.rating) } ?: WidgetPlaceholderColor
    Box(modifier = cardModifier(20)) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = WidgetGaugeSpec.indexName(indexType),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(WidgetTextColorDim), fontSize = (if (large) 11 else 9).sp, fontWeight = FontWeight.Medium),
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text = data?.score?.toString() ?: "—",
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(WidgetTextColor), fontSize = (if (large) 21 else 16).sp, fontWeight = FontWeight.Bold),
                )
                if (large) {
                    Spacer(GlanceModifier.width(8.dp))
                    RatingChangeRow(context, data, fontSp = 11, dotDp = 6)
                } else {
                    Spacer(GlanceModifier.width(6.dp))
                    ChangeText(data, fontSp = 9)
                }
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
        RefreshButton(modifier = GlanceModifier.padding(top = 4.dp, end = 6.dp), alignTopEnd = true)
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
private fun RefreshButton(modifier: GlanceModifier, alignTopEnd: Boolean) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Text(
            text = "↻",
            modifier = modifier.clickable(actionRunCallback<RefreshWidgetsAction>()).padding(horizontal = 10.dp, vertical = 8.dp),
            style = TextStyle(color = ColorProvider(WidgetTextColorDim), fontSize = 15.sp, fontWeight = FontWeight.Bold),
        )
    }
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
