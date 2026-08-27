package th1ngjin.fearindex.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

/** 위젯 ↻ 탭 → 4종 전부 즉시 재로드. */
class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        MarketFearWidget().updateAll(context)
        KospiFearWidget().updateAll(context)
        CryptoFearWidget().updateAll(context)
        DashboardFearWidget().updateAll(context)
        ChartFearWidget().updateAll(context)
    }
}
