package th1ngjin.fearindex.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

/** 위젯 ↻ 탭 → 토스트로 즉시 피드백 후 5종 전부 재로드 (완료되면 "HH:mm 기준" 갱신). */
class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                context.getString(th1ngjin.fearindex.presentation.R.string.widget_refreshing),
                Toast.LENGTH_SHORT,
            ).show()
        }
        MarketFearWidget().updateAll(context)
        KospiFearWidget().updateAll(context)
        CryptoFearWidget().updateAll(context)
        DashboardFearWidget().updateAll(context)
        ChartFearWidget().updateAll(context)
    }
}
