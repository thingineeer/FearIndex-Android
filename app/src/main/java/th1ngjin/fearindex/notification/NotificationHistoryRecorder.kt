package th1ngjin.fearindex.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import th1ngjin.fearindex.domain.entity.NotificationRecord
import th1ngjin.fearindex.domain.entity.NotificationRecordMapper
import th1ngjin.fearindex.domain.usecase.NotificationHistoryUseCase
import timber.log.Timber
import java.time.Instant

/**
 * 수신 푸시 → 로컬 알림 내역 기록 (v1.9.4, iOS `NotificationHistoryRecorder` 대응). 서버 전송 없음.
 *
 * 기록 경로 3곳:
 * 1. [recordRemoteMessage] — `onMessageReceived` (포그라운드 수신 / data-only)
 * 2. [recordLaunchIntent] — 알림 탭 (백그라운드 notification 메시지는 FCM SDK 가 렌더 후
 *    data + `google.message_id` 를 런처 인텐트 extras 로 전달)
 * 3. [syncActiveNotifications] — 앱 포그라운드 진입 시 알림 센터에 남아 있는(미탭) 알림 동기화
 * dedup 은 저장소가 id(FCM message id) 기준으로 처리 — 같은 알림이 여러 경로로 와도 1건.
 */
class NotificationHistoryRecorder(
    private val useCase: NotificationHistoryUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    /** 경로 1 — 수신 시각은 FCM sentTime 우선 (0 이면 현재 시각) */
    fun recordRemoteMessage(message: RemoteMessage) {
        val receivedAt =
            if (message.sentTime > 0) Instant.ofEpochMilli(message.sentTime) else Instant.now()
        record(
            NotificationRecordMapper.fromPush(
                data = message.data,
                messageId = message.messageId,
                title = message.notification?.title ?: message.data["title"],
                body = message.notification?.body ?: message.data["body"],
                receivedAt = receivedAt,
            ),
        )
    }

    /** 경로 2 — FCM 관련 extras 가 아니면(payload null) 무시 */
    fun recordLaunchIntent(extras: Bundle?) {
        val payload = LaunchIntentPayload.from(extras) ?: return
        val receivedAt = payload.sentTimeMillis?.let(Instant::ofEpochMilli) ?: Instant.now()
        record(
            NotificationRecordMapper.fromPush(
                data = payload.data,
                messageId = payload.messageId,
                title = payload.title,
                body = payload.body,
                receivedAt = receivedAt,
            ),
        )
    }

    /** 경로 3 — 우리 채널의 활성 알림만. type 없으면 OTHER 로 기록(이후 탭 시 저장소가 승격). */
    fun syncActiveNotifications(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val active = runCatching { manager.activeNotifications }.getOrNull() ?: return
        val records = active
            .filter { it.notification.channelId == NotificationChannels.FEAR_INDEX_ALERTS }
            .mapNotNull(::recordFromStatusBar)
        if (records.isEmpty()) return
        scope.launch {
            records.forEach { useCase.record(it) }
            Timber.d("[NotificationHistory] active 동기화 %d건", records.size)
        }
    }

    private fun recordFromStatusBar(sbn: StatusBarNotification): NotificationRecord? {
        val extras = sbn.notification.extras
        val data = listOf("type", "score", "google.message_id")
            .mapNotNull { key -> extras.getString(key)?.let { key to it } }
            .toMap()
        return NotificationRecordMapper.fromPush(
            data = data,
            messageId = data["google.message_id"],
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            receivedAt = Instant.ofEpochMilli(sbn.postTime),
        )
    }

    private fun record(record: NotificationRecord?) {
        val resolved = record ?: return
        scope.launch { useCase.record(resolved) }
    }
}
