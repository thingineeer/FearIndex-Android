package th1ngjin.fearindex.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

/**
 * 위젯 ↻ 새로고침 본작업 — 반드시 워커에서.
 * ActionCallback(브로드캐스트) 안에서 네트워크 3연타 + 위젯 7종 갱신을 하면 10초 예산을 넘겨
 * "Broadcast of Intent glance-action" ANR 이 난다(2026-08-28 에뮬 재현). 리시버는 enqueue 만 한다.
 *
 * 순서: ① refreshing=true + 재렌더(캐시라 즉시, ↻ 자리 스피너) ② 3개 지수 forceRefresh(병렬)
 * ③ refreshing=false + 재렌더(새 데이터, "HH:mm 기준" 갱신).
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        setRefreshingAll(true)
        updateAllFearWidgets(applicationContext)

        val ep = EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
        coroutineScope {
            listOf(
                async { runCatching { ep.getFearIndex().invoke(forceRefresh = true) } },
                async { runCatching { ep.getKospiFearIndex().invoke(forceRefresh = true) } },
                async { runCatching { ep.getCryptoFearIndex().invoke(forceRefresh = true) } },
            ).forEach { it.await() }
        }

        setRefreshingAll(false)
        updateAllFearWidgets(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Timber.w(e, "Widget manual refresh failed")
        runCatching { setRefreshingAll(false); updateAllFearWidgets(applicationContext) }
        Result.failure()
    }

    private suspend fun setRefreshingAll(value: Boolean) {
        val manager = GlanceAppWidgetManager(applicationContext)
        ALL_WIDGET_CLASSES.forEach { cls ->
            runCatching {
                manager.getGlanceIds(cls).forEach { id ->
                    updateAppWidgetState(applicationContext, id) { prefs ->
                        prefs[RefreshWidgetsAction.REFRESHING_KEY] = value
                    }
                }
            }
        }
    }

    companion object {
        private const val UNIQUE_NAME = "fear_widget_manual_refresh"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.KEEP, // 이미 새로고침 중이면 연타 무시
                request,
            )
        }
    }
}
