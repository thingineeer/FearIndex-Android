package th1ngjin.fearindex.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import th1ngjin.fearindex.domain.entity.FearIndexType

/**
 * 위젯 ↻ 탭 — 구글 위젯처럼 ↻ 자리가 스피너로 바뀌었다가 완료되면 돌아온다 (2026-08-28 사용자 요청).
 * ① 전 위젯 refreshing=true + 재렌더(캐시라 즉시, 스피너 표시)
 * ② 3개 지수 forceRefresh 로 캐시 갱신
 * ③ refreshing=false + 재렌더(새 데이터, "HH:mm 기준" 갱신)
 */
class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        setRefreshingAll(context, true)
        updateAllWidgets(context)

        val ep = dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        runCatching { ep.getFearIndex().invoke(forceRefresh = true) }
        runCatching { ep.getKospiFearIndex().invoke(forceRefresh = true) }
        runCatching { ep.getCryptoFearIndex().invoke(forceRefresh = true) }

        setRefreshingAll(context, false)
        updateAllWidgets(context)
    }

    private suspend fun setRefreshingAll(context: Context, value: Boolean) {
        val manager = GlanceAppWidgetManager(context)
        listOf(
            MarketFearWidget::class.java,
            KospiFearWidget::class.java,
            CryptoFearWidget::class.java,
            DashboardFearWidget::class.java,
            ChartFearWidget::class.java,
            KospiChartWidget::class.java,
            CryptoChartWidget::class.java,
        ).forEach { cls ->
            runCatching {
                manager.getGlanceIds(cls).forEach { id ->
                    updateAppWidgetState(context, id) { prefs -> prefs[REFRESHING_KEY] = value }
                }
            }
        }
    }

    private suspend fun updateAllWidgets(context: Context) {
        MarketFearWidget().updateAll(context)
        KospiFearWidget().updateAll(context)
        CryptoFearWidget().updateAll(context)
        DashboardFearWidget().updateAll(context)
        ChartFearWidget().updateAll(context)
        KospiChartWidget().updateAll(context)
        CryptoChartWidget().updateAll(context)
    }

    companion object {
        val REFRESHING_KEY = booleanPreferencesKey("refreshing")
    }
}
