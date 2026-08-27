package th1ngjin.fearindex.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import th1ngjin.fearindex.domain.entity.FearIndexType

/** 차트 위젯 이름 탭 — 지수 순환 전환 (Global → KOSPI → Crypto). 위젯 인스턴스별 저장. */
class CycleChartIndexAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val current = fromName(
            androidx.glance.appwidget.state.getAppWidgetState(
                context,
                androidx.glance.state.PreferencesGlanceStateDefinition,
                glanceId,
            )[CHART_INDEX_KEY],
        )
        val next = FearIndexType.entries[(current.ordinal + 1) % FearIndexType.entries.size]
        updateAppWidgetState(context, glanceId) { prefs -> prefs[CHART_INDEX_KEY] = next.name }
        ChartFearWidget().update(context, glanceId)
    }

    companion object {
        val CHART_INDEX_KEY = stringPreferencesKey("chart_index")

        fun fromName(name: String?): FearIndexType =
            FearIndexType.entries.firstOrNull { it.name == name } ?: FearIndexType.MARKET
    }
}
