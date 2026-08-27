package th1ngjin.fearindex.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
        ChartFearWidget().updateAll(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Timber.w(e, "Fear widget update failed")
        Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "fear_widget_periodic_update"
        private const val RETRY_UNIQUE_NAME = "fear_widget_load_retry"
        private const val INTERVAL_MINUTES = 180L
        private const val RETRY_DELAY_MINUTES = 10L

        /**
         * 위젯 데이터 로드 실패(placeholder 렌더) 시 10분 뒤 1회 재갱신 예약.
         * unique KEEP 이라 대기 중 재시도는 1개만 유지 — provideGlance 재실패 → 재예약 루프여도
         * 10분 간격 이상으로만 돈다. (Play 리뷰 "위젯 안 됨" 대응: placeholder 가 3시간 주기까지
         * 방치되던 공백 축소, 2026-08-27)
         */
        fun enqueueRetry(context: Context) {
            val request = OneTimeWorkRequestBuilder<FearWidgetUpdateWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInitialDelay(RETRY_DELAY_MINUTES, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                RETRY_UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

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
