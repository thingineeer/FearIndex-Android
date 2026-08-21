package th1ngjin.fearindex.notification

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.tasks.await
import th1ngjin.fearindex.notification.di.MessagingEntryPoint
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * FCM 토큰 서버 등록 재시도 워커.
 *
 * 앱 시작/onNewToken 의 즉시 등록은 fire-and-forget 이라 한 번 실패하면(App Check 토큰 미발급,
 * 네트워크 등) 다음 프로세스 기동까지 기기가 미등록 상태로 남는다 → 지수 백오프로 재시도해
 * 푸시 등록 공백을 줄인다. 서버 registerFCMToken 은 멱등(upsert)이라 중복 호출은 무해.
 */
class FcmRegistrationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            MessagingEntryPoint::class.java,
        )
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            val deviceId = entryPoint.deviceIdProvider().loadDeviceId()
            entryPoint.notificationRepository().registerFCMToken(deviceId, token)
            Timber.d("FCM token registered by retry worker (attempt %d)", runAttemptCount + 1)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                Timber.w(e, "FCM token registration retry exhausted after %d attempts", MAX_ATTEMPTS)
                Result.failure()
            } else {
                Timber.d(e, "FCM token registration retry %d failed — backoff", runAttemptCount + 1)
                Result.retry()
            }
        }
    }

    companion object {
        private const val UNIQUE_NAME = "fcm_registration_retry"
        private const val MAX_ATTEMPTS = 8
        private const val INITIAL_BACKOFF_MINUTES = 1L

        /** 즉시 등록 실패 시 호출 — 이미 예약된 재시도가 있으면 유지(KEEP). */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<FcmRegistrationWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_MINUTES, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
