package th1ngjin.fearindex.core.analytics

/**
 * Analytics 이벤트 정의 — iOS [AnalyticsEvent.swift](../../../../FearIndex-iOS/LocalPackages/Core/Sources/Core/Analytics/AnalyticsEvent.swift) 1:1 매핑.
 *
 * **Firebase Analytics 이벤트 이름/파라미터 키는 영문 snake_case** 로 보낸다.
 * Firebase 규격: 이름은 `[A-Za-z][A-Za-z0-9_]{0,39}`. 한글/특수문자가 포함되면 SDK가
 * `Invalid event name` 경고와 함께 이벤트 자체를 drop 하므로 Console 에 도달하지 못한다.
 *
 * Kotlin 식별자(클래스/필드)는 한글 유지 (코드 가독성). 실제 전송되는 `name`/parameters 키
 * 만 영문 raw 로 변환한다. iOS 와 대시보드 공유하므로 iOS 도 같은 영문 raw 이름을 써야 한다.
 */
sealed class AnalyticsEvent(val name: String, val parameters: Map<String, Any>? = null) {

    // MARK: - 앱 라이프사이클
    data object 앱시작 : AnalyticsEvent("app_start")
    data object 앱백그라운드 : AnalyticsEvent("app_background")
    data object 앱포그라운드 : AnalyticsEvent("app_foreground")

    // MARK: - 탭 이동
    data class 탭선택(val 탭이름: String) : AnalyticsEvent("tab_selected", mapOf("tab_name" to 탭이름))

    // MARK: - 데이터 새로고침
    data class 수동새로고침(val 화면: String) : AnalyticsEvent("manual_refresh", mapOf("screen" to 화면))
    data object 자동새로고침 : AnalyticsEvent("auto_refresh")

    // MARK: - 차트 상호작용
    data class 차트기간선택(val 기간: String) : AnalyticsEvent("chart_period_selected", mapOf("period" to 기간))

    // MARK: - 시장 지수
    data class 시장지수조회(val 지수이름: String) :
        AnalyticsEvent("market_index_viewed", mapOf("index_name" to 지수이름))

    // MARK: - 에러
    data class API에러(val 에러유형: String, val 에러메시지: String) :
        AnalyticsEvent("api_error", mapOf("error_type" to 에러유형, "error_message" to 에러메시지))

    data class 네트워크에러(val 에러메시지: String) :
        AnalyticsEvent("network_error", mapOf("error_message" to 에러메시지))

    // MARK: - 광고
    data class 배너광고노출(val 화면: String) :
        AnalyticsEvent("banner_ad_impression", mapOf("screen" to 화면))
    data class 배너광고클릭(val 화면: String) :
        AnalyticsEvent("banner_ad_clicked", mapOf("screen" to 화면))
    data class 배너광고실패(val 에러메시지: String) :
        AnalyticsEvent("banner_ad_failed", mapOf("error_message" to 에러메시지))

    // MARK: - 설정
    data class 설정변경(val 설정항목: String, val 변경값: String) :
        AnalyticsEvent("setting_changed", mapOf("setting_key" to 설정항목, "setting_value" to 변경값))

    // MARK: - 위젯/워치 (Android에서는 위젯만 해당)
    data object 위젯업데이트 : AnalyticsEvent("widget_updated")

    // MARK: - 사용자 행동
    data class 공포지수조회(val 현재점수: Int, val 등급: String) :
        AnalyticsEvent("fear_index_viewed", mapOf("current_score" to 현재점수, "rating" to 등급))

    data object 비교데이터조회 : AnalyticsEvent("comparison_viewed")

    // MARK: - 푸시 알림
    data object 알림설정화면진입 : AnalyticsEvent("notification_settings_opened")
    data class 알림설정변경(val 활성화: Boolean) :
        AnalyticsEvent("notification_toggled", mapOf("enabled" to 활성화))

    data class 알림임계값변경(val 하한값: Int, val 상한값: Int) :
        AnalyticsEvent(
            "notification_threshold_changed",
            mapOf("lower" to 하한값, "upper" to 상한값),
        )

    data class 푸시알림수신(val 공포지수: Int, val 조건: String) :
        AnalyticsEvent(
            "push_received",
            mapOf("fear_index" to 공포지수, "condition" to 조건),
        )

    data class 푸시알림탭(val 공포지수: Int, val 조건: String) :
        AnalyticsEvent(
            "push_tapped",
            mapOf("fear_index" to 공포지수, "condition" to 조건),
        )

    data class 푸시알림후체류시간(val 체류초: Int, val 공포지수: Int) :
        AnalyticsEvent(
            "push_engagement_duration",
            mapOf("dwell_seconds" to 체류초, "fear_index" to 공포지수),
        )

    // MARK: - 암호화폐 공포지수
    data class 지수타입전환(
        val 타입: String,
        val 화면: String,
        val 이전타입: String? = null,
    ) : AnalyticsEvent(
        name = "index_type_switched",
        parameters = buildMap {
            put("type", 타입)
            put("screen", 화면)
            이전타입?.let { put("previous_type", it) }
        },
    )

    data class 암호화폐공포지수조회(val 현재점수: Int, val 등급: String) :
        AnalyticsEvent(
            "crypto_fear_index_viewed",
            mapOf("current_score" to 현재점수, "rating" to 등급),
        )

    data class 암호화폐차트조회(val 기간: String) :
        AnalyticsEvent("crypto_chart_viewed", mapOf("period" to 기간))

    data class 암호화폐알림설정변경(val 활성화: Boolean) :
        AnalyticsEvent("crypto_notification_toggled", mapOf("enabled" to 활성화))

    data class 암호화폐알림임계값변경(val 하한값: Int, val 상한값: Int) :
        AnalyticsEvent(
            "crypto_notification_threshold_changed",
            mapOf("lower" to 하한값, "upper" to 상한값),
        )

    // MARK: - 투표 (Stuck Counter)
    data class 투표참여(val 선택: String, val 지수타입: String, val 현재점수: Int) :
        AnalyticsEvent(
            "vote_cast",
            mapOf("choice" to 선택, "index_type" to 지수타입, "current_score" to 현재점수),
        )

    data class 투표결과조회(val 지수타입: String, val 총투표수: Int) :
        AnalyticsEvent(
            "vote_results_viewed",
            mapOf("index_type" to 지수타입, "total_votes" to 총투표수),
        )

    data class 투표중복시도(val 지수타입: String) :
        AnalyticsEvent("vote_duplicate_attempt", mapOf("index_type" to 지수타입))

    data class 투표탭진입(val 지수타입: String) :
        AnalyticsEvent("vote_tab_opened", mapOf("index_type" to 지수타입))

    data class 투표세그먼트전환(val 지수타입: String, val 이전타입: String) :
        AnalyticsEvent(
            "vote_segment_switched",
            mapOf("index_type" to 지수타입, "previous_type" to 이전타입),
        )

    data class 투표제출실패(val 지수타입: String, val 에러: String) :
        AnalyticsEvent(
            "vote_submit_failed",
            mapOf("index_type" to 지수타입, "error" to 에러),
        )

    // MARK: - 공유 카드
    data class 공유버튼탭(val 지수타입: String, val 현재점수: Int) :
        AnalyticsEvent(
            "share_button_tapped",
            mapOf("index_type" to 지수타입, "current_score" to 현재점수),
        )

    data class 공유카드생성(val 지수타입: String, val 현재점수: Int, val 등급: String) :
        AnalyticsEvent(
            "share_card_generated",
            mapOf("index_type" to 지수타입, "current_score" to 현재점수, "rating" to 등급),
        )

    data class 공유완료(val 지수타입: String, val 공유대상: String) :
        AnalyticsEvent(
            "share_completed",
            mapOf("index_type" to 지수타입, "share_target" to 공유대상),
        )

    data class 공유취소(val 지수타입: String) :
        AnalyticsEvent("share_cancelled", mapOf("index_type" to 지수타입))

    // MARK: - 인터스티셜 광고 (Android는 현재 미사용)
    data class 인터스티셜광고노출(val 노출횟수: Int) :
        AnalyticsEvent("interstitial_ad_impression", mapOf("impression_count" to 노출횟수))

    data class 인터스티셜광고닫기(val 시청초: Int) :
        AnalyticsEvent("interstitial_ad_closed", mapOf("watch_seconds" to 시청초))

    data class 인터스티셜광고실패(val 에러메시지: String) :
        AnalyticsEvent("interstitial_ad_failed", mapOf("error_message" to 에러메시지))

    // MARK: - 인사이트 (이미 영문 raw 사용 중 — 유지)
    data class 인사이트카드노출(val 카드타입: String, val 지수타입: String) :
        AnalyticsEvent("insight_card_viewed", mapOf("card_type" to 카드타입, "index_type" to 지수타입))

    data class 인사이트목록진입(val 진입경로: String, val 지수타입: String) :
        AnalyticsEvent("insight_list_opened", mapOf("source" to 진입경로, "index_type" to 지수타입))

    data class 인사이트세그먼트전환(val 이전타입: String, val 변경타입: String) :
        AnalyticsEvent("insight_segment_switched", mapOf("from_type" to 이전타입, "to_type" to 변경타입))

    data class 인사이트상세조회(val 카드타입: String, val 점수: Int) :
        AnalyticsEvent("insight_detail_viewed", mapOf("card_type" to 카드타입, "score" to 점수))

    // MARK: - 수익 최적화 이벤트
    data class 화면체류시간(val 화면: String, val 체류초: Int) :
        AnalyticsEvent("screen_dwell_time", mapOf("screen" to 화면, "dwell_seconds" to 체류초))

    data class 차트상호작용(val 타입: String) :
        AnalyticsEvent("chart_interaction", mapOf("type" to 타입))

    data class 알림설정진입경로(val 경로: String) :
        AnalyticsEvent("notification_settings_source", mapOf("source" to 경로))

    data class 세션종료(val 세션길이초: Int) :
        AnalyticsEvent("session_ended", mapOf("session_seconds" to 세션길이초))
}

/**
 * 화면 이름 정의 — Firebase Analytics `screen_view` 의 `screen_name` 파라미터.
 * 한글이면 Console 에서 reject 되므로 영문 snake_case 로 보낸다 (iOS 와 동일 기준).
 */
enum class AnalyticsScreen(val screenName: String, val screenClass: String) {
    홈("home", "HomeScreen"),
    차트("chart", "ChartScreen"),
    설정("settings", "SettingsScreen"),
    투표("vote", "VoteScreen"),
    알림설정("notification_settings", "NotificationSettingsScreen");
}
