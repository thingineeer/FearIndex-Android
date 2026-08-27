package th1ngjin.fearindex.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import th1ngjin.fearindex.MainActivity
import th1ngjin.fearindex.domain.entity.FearIndexType

/**
 * 2×2 단일 지수 위젯. Fear score 색으로 배경을 채우고 라벨/점수/등급을 중앙 정렬한다.
 * 데이터가 null 이면 중립 placeholder("—")를 표시한다.
 */
@Composable
fun FearIndexWidgetContent(
    context: Context,
    indexType: FearIndexType,
    data: WidgetIndexData?,
) {
    val background = data?.let { widgetFearScoreColor(it.score) } ?: WidgetPlaceholderColor
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(ColorProvider(background))
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = indexLabel(context, indexType),
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(WidgetTextColorDim),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = data?.score?.toString() ?: "—",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(WidgetTextColor),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = data?.let { ratingLabel(context, it.rating) } ?: "",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(WidgetTextColor),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/**
 * 4×2 대시보드 위젯. 상단 제목 + 3개 행(지수 라벨 / 점수 / 등급). 각 행 배경을 해당 지수의
 * fear score 색으로 틴트한다.
 */
@Composable
fun DashboardWidgetContent(
    context: Context,
    market: WidgetIndexData?,
    kospi: WidgetIndexData?,
    crypto: WidgetIndexData?,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(ColorProvider(WidgetDashboardBackground))
            .clickable(actionStartActivity<MainActivity>())
            .padding(10.dp),
    ) {
        Text(
            text = context.getString(th1ngjin.fearindex.presentation.R.string.app_name),
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(WidgetTextColorDim),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(6.dp))
        DashboardRow(context, FearIndexType.MARKET, market)
        Spacer(GlanceModifier.height(4.dp))
        DashboardRow(context, FearIndexType.KOSPI, kospi)
        Spacer(GlanceModifier.height(4.dp))
        DashboardRow(context, FearIndexType.CRYPTO, crypto)
    }
}

@Composable
private fun DashboardRow(
    context: Context,
    indexType: FearIndexType,
    data: WidgetIndexData?,
) {
    val background = data?.let { widgetFearScoreColor(it.score) } ?: WidgetPlaceholderColor
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(10.dp)
            .background(ColorProvider(background))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = indexLabel(context, indexType),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(WidgetTextColorDim),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = data?.score?.toString() ?: "—",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(WidgetTextColor),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = data?.let { ratingLabel(context, it.rating) } ?: "",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(WidgetTextColor),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * 1×1 컴팩트 단일 지수 위젯. 점수만 크게 — 등급/라벨 생략(공간 부족), 배경색이 등급을 전달한다.
 * 데이터가 null 이면 중립 placeholder("—").
 */
@Composable
fun CompactFearIndexWidgetContent(
    context: Context,
    indexType: FearIndexType,
    data: WidgetIndexData?,
) {
    val background = data?.let { widgetFearScoreColor(it.score) } ?: WidgetPlaceholderColor
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(ColorProvider(background))
            .clickable(actionStartActivity<MainActivity>())
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = indexLabel(context, indexType),
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(WidgetTextColorDim),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
        Text(
            text = data?.score?.toString() ?: "—",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(WidgetTextColor),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
