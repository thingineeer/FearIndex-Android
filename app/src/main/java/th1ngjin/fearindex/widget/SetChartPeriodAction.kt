package th1ngjin.fearindex.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

/** 차트 위젯 기간 세그먼트 탭 — 이 위젯 인스턴스의 기간을 저장하고 즉시 재렌더. */
class SetChartPeriodAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val period = WidgetChartPeriod.fromName(parameters[PERIOD_PARAM])
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[CHART_PERIOD_KEY] = period.name
        }
        ChartFearWidget().update(context, glanceId)
    }

    companion object {
        val CHART_PERIOD_KEY = stringPreferencesKey("chart_period")
        val PERIOD_PARAM = ActionParameters.Key<String>("period")

        fun params(period: WidgetChartPeriod) = actionParametersOf(PERIOD_PARAM to period.name)
    }
}
