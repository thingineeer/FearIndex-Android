package th1ngjin.fearindex.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import th1ngjin.fearindex.domain.entity.FearIndexType

/**
 * 통합 위젯 — 2×2 기본(게이지 3개 컴팩트), 4×2 이상으로 늘리면 대형 레이아웃.
 * (구 4×2 대시보드를 2×2 기본으로 개편 — 기존 배치본은 크기 유지된 채 새 레이아웃으로 렌더)
 */
class DashboardFearWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val market = loadWidgetIndex(context, FearIndexType.MARKET)
        val kospi = loadWidgetIndex(context, FearIndexType.KOSPI)
        val crypto = loadWidgetIndex(context, FearIndexType.CRYPTO)
        if (market == null || kospi == null || crypto == null) FearWidgetUpdateWorker.enqueueRetry(context)
        val refreshing = runCatching {
            androidx.glance.appwidget.state.getAppWidgetState(
                context,
                androidx.glance.state.PreferencesGlanceStateDefinition,
                id,
            )[RefreshWidgetsAction.REFRESHING_KEY]
        }.getOrNull() ?: false
        provideContent {
            val size = LocalSize.current
            CombinedWidgetContent(context, market, kospi, crypto, large = size.width.value >= 250f, refreshing = refreshing)
        }
    }
}

class DashboardFearWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DashboardFearWidget()
}
