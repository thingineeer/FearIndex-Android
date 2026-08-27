package th1ngjin.fearindex.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import th1ngjin.fearindex.domain.entity.FearIndexType

/**
 * 2×2 단일 지수 위젯 공통 베이스. 지정된 [indexType] 을 fetch 한 뒤 [FearIndexWidgetContent] 로 렌더한다.
 */
abstract class IndexFearWidget(private val indexType: FearIndexType) : GlanceAppWidget() {

    // 1×1 리사이즈 지원: 크기별로 컴팩트/풀 레이아웃을 갈아끼우기 위해 Exact 모드 사용
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetIndex(context, indexType)
        if (data == null) FearWidgetUpdateWorker.enqueueRetry(context)
        provideContent {
            val size = LocalSize.current
            when (WidgetLayoutMode.from(size.width.value, size.height.value)) {
                WidgetLayoutMode.COMPACT -> CompactFearIndexWidgetContent(context, indexType, data)
                WidgetLayoutMode.FULL -> FearIndexWidgetContent(context, indexType, data)
            }
        }
    }
}

class MarketFearWidget : IndexFearWidget(FearIndexType.MARKET)

class KospiFearWidget : IndexFearWidget(FearIndexType.KOSPI)

class CryptoFearWidget : IndexFearWidget(FearIndexType.CRYPTO)

class MarketFearWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MarketFearWidget()
}

class KospiFearWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KospiFearWidget()
}

class CryptoFearWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CryptoFearWidget()
}
