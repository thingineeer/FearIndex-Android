package th1ngjin.fearindex.domain.entity

import java.time.Instant

/**
 * 수신 푸시 알림의 채널 분류 (v1.9.4 알림 내역, iOS `NotificationKind` 1:1).
 *
 * 서버 FCM `data.type` 은 [NotificationRecordMapper.kindFromType] 이 이 값으로 정규화한다.
 * [storageValue] 는 JSONL 저장 포맷 — 변경 금지.
 */
enum class NotificationKind(val storageValue: String) {
    /** S&P 500 (CNN Fear & Greed) 임계값 알림 */
    MARKET("market"),
    /** KOSPI 공포지수 임계값 알림 */
    KOSPI("kospi"),
    /** 암호화폐(BTC) 공포지수 임계값 알림 */
    CRYPTO("crypto"),
    /** 주간 리포트 */
    WEEKLY("weekly"),
    /** 그 외 (온보딩 드립 등 미분류) */
    OTHER("other"),
    ;

    companion object {
        /** 알 수 없는 저장값(미래 버전이 쓴 파일)은 [OTHER] 로 폴백 — 한 줄이 전체 내역을 막지 않도록. */
        fun fromStorage(value: String?): NotificationKind =
            entries.firstOrNull { it.storageValue == value } ?: OTHER
    }
}

/**
 * 알림 내역 1건 — 기기 로컬(JSONL)에만 저장되며 서버로 전송하지 않는다.
 * SRP: 수신 알림의 불변 스냅샷만 담당.
 */
data class NotificationRecord(
    /** dedup 키 — FCM message id 우선, 없으면 `kind-receivedAt(초)` 파생 ([fallbackId]) */
    val id: String,
    val kind: NotificationKind,
    /** 서버가 사용자 언어로 localize 해 보낸 제목 원문 */
    val title: String,
    /** 서버가 사용자 언어로 localize 해 보낸 본문 원문 */
    val body: String,
    /** 발송 시점 지수 점수 (주간 리포트 등 점수 없는 알림은 null) */
    val score: Int?,
    val receivedAt: Instant,
) {
    /** id 가 FCM message id 없이 파생된 키인지 — 나중에 실제 id 가 오면 승격 대상. */
    val hasFallbackId: Boolean
        get() = id == fallbackId(kind, receivedAt)

    companion object {
        /** FCM message id 부재 시의 파생 dedup 키 (초 단위 — 같은 초 같은 채널 2건은 1건으로 합쳐진다) */
        fun fallbackId(kind: NotificationKind, receivedAt: Instant): String =
            "${kind.storageValue}-${receivedAt.epochSecond}"
    }
}
