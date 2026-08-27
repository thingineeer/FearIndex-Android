package th1ngjin.fearindex.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
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

/** 단일 지수 — 1×1(기본): 게이지 + 점수 + 풀네임. */
@Composable
fun CompactFearIndexWidgetContent(
    context: Context,
    indexType: FearIndexType,
    data: WidgetIndexData?,
) {
    Column(
        modifier = cardModifier(16).padding(3.dp),
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

/** 통합(대시보드) — 2×2/4×2 responsive: 게이지 3개 + 이름/등급/변화 + ↻ + 갱신시각. */
@Composable
fun CombinedWidgetContent(
    context: Context,
    market: WidgetIndexData?,
    kospi: WidgetIndexData?,
    crypto: WidgetIndexData?,
    large: Boolean,
) {
    val gaugePx = if (large) 280 else 170
    val scoreSp = if (large) 24 else 15
    val nameSp = if (large) 10 else 8
    val ratingSp = if (large) 11 else 9
    Box(modifier = cardModifier(20)) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                listOf(
                    Triple(FearIndexType.MARKET, market, Unit),
                    Triple(FearIndexType.KOSPI, kospi, Unit),
                    Triple(FearIndexType.CRYPTO, crypto, Unit),
                ).forEach { (type, data, _) ->
                    Column(
                        // Glance: Row 자식에 fillMaxSize 를 겹치면 width=fill 이 weight 를 덮어써
                        // 첫 컬럼이 전체 폭을 차지한다 → weight(폭) + fillMaxHeight(높이)만.
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GaugeWithScore(data, gaugePx = gaugePx, scoreSp = scoreSp, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
                        Text(
                            text = WidgetGaugeSpec.indexName(type),
                            maxLines = 1,
                            style = TextStyle(color = ColorProvider(WidgetTextColorDim), fontSize = nameSp.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                        )
                        RatingChangeRow(context, data, fontSp = ratingSp, dotDp = if (large) 7 else 5)
                    }
                }
            }
        }
        RefreshButton(modifier = GlanceModifier.padding(top = 4.dp, end = 6.dp), alignTopEnd = true)
        UpdatedAt(context, large)
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
    val ratingColor = data?.let { widgetFearScoreColor(it.score) } ?: WidgetPlaceholderColor
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
                Image(
                    provider = ImageProvider(
                        WidgetChartRenderer.render(history, xLabels, if (large) 640 else 340, if (large) 200 else 150, ratingColor.toArgb()),
                    ),
                    contentDescription = null,
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
    val ratingColor = widgetFearScoreColor(data.score)
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
            modifier = modifier.clickable(actionRunCallback<RefreshWidgetsAction>()).padding(4.dp),
            style = TextStyle(color = ColorProvider(WidgetTextColorDim), fontSize = 13.sp, fontWeight = FontWeight.Bold),
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
