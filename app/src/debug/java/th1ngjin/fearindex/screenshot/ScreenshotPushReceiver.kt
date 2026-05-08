package th1ngjin.fearindex.screenshot

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import th1ngjin.fearindex.MainActivity
import th1ngjin.fearindex.R
import th1ngjin.fearindex.notification.NotificationChannels

/**
 * Play Store promo 스크린샷 촬영용 BroadcastReceiver. **debug 빌드에서만 등록**.
 *
 * 사용:
 *   adb shell am broadcast -n th1ngjin.fearindex.debug/th1ngjin.fearindex.screenshot.ScreenshotPushReceiver \
 *     -a th1ngjin.fearindex.SCREENSHOT_PUSH \
 *     --es title "공포 지수가 25 이하로 내려갔습니다" \
 *     --es body "역사적으로 매수 기회였습니다"
 *
 * 실제 FCM 경로 거치지 않고 NotificationCompat 으로 즉시 표시 → 45 locale × 1초 캡처 가능.
 */
class ScreenshotPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        val body = intent.getStringExtra("body") ?: return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            context,
            NotificationChannels.FEAR_INDEX_ALERTS,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        // 같은 ID 로 재발사 시 heads-up 이 다시 뜨지 않으므로 매번 unique ID 사용
        // (45 locale × 1번 발사이므로 ID 가 누적되어도 부담 없음)
        val uniqueId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
        manager.cancelAll()
        manager.notify(uniqueId, notification)
    }
}
