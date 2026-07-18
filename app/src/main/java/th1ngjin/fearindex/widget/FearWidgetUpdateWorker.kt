package th1ngjin.fearindex.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * 모든 Fear & Greed 위젯을 주기적으로 갱신하는 워커.
 * 각 위젯의 provideGlance 가 최신 지수를 fetch 하도록 updateAll 을 호출한다.
 */
class FearWidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        MarketFearWidget().updateAll(applicationContext)
        KospiFearWidget().updateAll(applicationContext)
        CryptoFearWidget().updateAll(applicationContext)
        DashboardFearWidget().updateAll(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Timber.w(e, "Fear widget update failed")
        Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "fear_widget_periodic_update"
        private const val INTERVAL_MINUTES = 180L

        /** 3시간 주기 갱신 스케줄 등록 (이미 등록되어 있으면 유지). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FearWidgetUpdateWorker>(
                INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
