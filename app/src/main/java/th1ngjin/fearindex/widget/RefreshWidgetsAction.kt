package th1ngjin.fearindex.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * 위젯 ↻ 탭. ⚠️ 여기서 직접 fetch/updateAll 을 하면 브로드캐스트 10초 예산 초과로 ANR —
 * 워커에 위임하고 즉시 반환한다(2026-08-28 에뮬 재현·수정, WidgetRefreshWorker 주석 참조).
 */
class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetRefreshWorker.enqueue(context)
    }

    companion object {
        val REFRESHING_KEY = booleanPreferencesKey("refreshing")
    }
}
