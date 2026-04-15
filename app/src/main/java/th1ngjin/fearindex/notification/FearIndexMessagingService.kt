package th1ngjin.fearindex.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
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
        showNotification(title, body)
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

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, "fear_index_alerts")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
