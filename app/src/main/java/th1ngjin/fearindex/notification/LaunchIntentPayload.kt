package th1ngjin.fearindex.notification

import android.os.Bundle

/**
 * 알림 탭으로 기동된 런처 인텐트 extras → 알림 내역 기록용 payload (순수 매핑, JVM 테스트 대상).
 *
 * 백그라운드 notification 메시지는 FCM SDK 가 트레이에 렌더하고, 탭 시 data 키 + `google.message_id`
 * + `google.sent_time` 을 런처 Activity 인텐트 extras 로 전달한다. 우리 `showNotification` 의
 * content Intent 도 같은 키 규약으로 extras 를 싣는다 — 두 경로 모두 이 매핑 하나로 처리.
 */
data class LaunchIntentPayload(
    val data: Map<String, String>,
    val messageId: String?,
    val title: String?,
    val body: String?,
    val sentTimeMillis: Long?,
) {
    companion object {
        /** FCM 관련 payload 가 아닌 일반 기동(런처/딥링크)은 null — message id 도 type 도 없으면 무시. */
        fun from(extras: Bundle?): LaunchIntentPayload? {
            val bundle = extras ?: return null
            @Suppress("DEPRECATION")
            return fromMap(bundle.keySet().associateWith { key -> bundle.get(key) })
        }

        fun fromMap(map: Map<String, Any?>): LaunchIntentPayload? {
            val messageId = map.string("google.message_id") ?: map.string("gcm.message_id")
            val type = map.string("type")
            if (messageId == null && type == null) return null
            return LaunchIntentPayload(
                data = buildDataMap(map),
                messageId = messageId,
                title = map.string("title") ?: map.string("gcm.notification.title"),
                body = map.string("body") ?: map.string("gcm.notification.body"),
                sentTimeMillis = map.long("google.sent_time"),
            )
        }

        /** 매핑에 쓰는 서버 data 키만 추린다 (`NotificationRecordMapper` 가 읽는 키 + message id). */
        private fun buildDataMap(map: Map<String, Any?>): Map<String, String> =
            listOf("type", "score", "google.message_id", "gcm.message_id")
                .mapNotNull { key -> map.string(key)?.let { key to it } }
                .toMap()

        private fun Map<String, Any?>.string(key: String): String? =
            (this[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }

        private fun Map<String, Any?>.long(key: String): Long? = when (val value = this[key]) {
            is Number -> value.toLong().takeIf { it > 0 }
            is String -> value.toLongOrNull()?.takeIf { it > 0 }
            else -> null
        }
    }
}
