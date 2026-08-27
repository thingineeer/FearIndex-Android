package th1ngjin.fearindex.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import th1ngjin.fearindex.domain.entity.FearIndexType

/**
 * 4×2 대시보드 위젯. 세 지수를 각각 fetch (하나 실패해도 나머지 표시) 후 [DashboardWidgetContent] 로 렌더한다.
 */
class DashboardFearWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val market = loadWidgetIndex(context, FearIndexType.MARKET)
        val kospi = loadWidgetIndex(context, FearIndexType.KOSPI)
        val crypto = loadWidgetIndex(context, FearIndexType.CRYPTO)
        if (market == null || kospi == null || crypto == null) FearWidgetUpdateWorker.enqueueRetry(context)
        provideContent {
            DashboardWidgetContent(context, market, kospi, crypto)
        }
    }
}

class DashboardFearWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DashboardFearWidget()
}
