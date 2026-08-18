package th1ngjin.fearindex.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import th1ngjin.fearindex.MainActivity
import th1ngjin.fearindex.R
import th1ngjin.fearindex.notification.di.MessagingEntryPoint
import timber.log.Timber

/**
 * FCM 메시지 수신 + 토큰 갱신 서비스.
 *
 * Hilt의 EntryPoint를 사용해 NotificationRepository에 접근.
 * Service는 AndroidEntryPoint를 직접 사용할 수 없으므로 EntryPointAccessors 활용.
 */
class FearIndexMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Token refreshed: ${token.take(10)}...")
        registerTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        showNotification(title, body, message)
        // 알림 내역 기록 (경로 1: 포그라운드 수신 / data-only) — 서버 변경 0.
        historyRecorder().recordRemoteMessage(message)
    }

    private fun historyRecorder(): NotificationHistoryRecorder {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            MessagingEntryPoint::class.java,
        )
        return NotificationHistoryRecorder(entryPoint.notificationHistoryUseCase())
    }

    private fun registerTokenToServer(token: String) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            MessagingEntryPoint::class.java,
        )
        val repository = entryPoint.notificationRepository()
        val deviceIdProvider = entryPoint.deviceIdProvider()
        val deviceId = deviceIdProvider.loadDeviceId()

        serviceScope.launch {
            try {
                repository.registerFCMToken(deviceId, token)
                Timber.d("FCM token registered to server")
            } catch (e: Exception) {
                Timber.e(e, "FCM token registration failed")
            }
        }
    }

    private fun showNotification(title: String, body: String, message: RemoteMessage) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // 알림 탭 시 내역 기록(경로 2)용 payload — FCM 백그라운드 탭 인텐트와 동일 키 규약.
            putExtras(historyExtras(title, body, message))
        }
        // requestCode 를 알림마다 다르게 — 같은 requestCode 재사용 시 이전 알림의 extras 로 덮이는 것 방지.
        val requestCode = (System.currentTimeMillis() and 0xFFFF).toInt()
        val pendingIntent = PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.FEAR_INDEX_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // 알림 센터 잔류분 동기화(경로 3)에서 kind/score/id 를 복원할 수 있도록 extras 에 보존.
            .addExtras(historyExtras(title, body, message))
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /** 내역 기록용 공통 extras — `LaunchIntentPayload`/`syncActiveNotifications` 가 읽는 키. */
    private fun historyExtras(title: String, body: String, message: RemoteMessage): Bundle =
        bundleOf(
            "title" to title,
            "body" to body,
            "type" to message.data["type"],
            "score" to message.data["score"],
            "google.message_id" to message.messageId,
            "google.sent_time" to message.sentTime,
        )
}
