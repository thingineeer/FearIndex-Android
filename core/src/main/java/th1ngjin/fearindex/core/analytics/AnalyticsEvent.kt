package th1ngjin.fearindex.core.analytics

/**
 * Analytics 이벤트 정의 — iOS [AnalyticsEvent.swift](../../../../FearIndex-iOS/LocalPackages/Core/Sources/Core/Analytics/AnalyticsEvent.swift) 1:1 매핑.
 *
 * **이벤트 이름은 한국어 그대로 유지**한다. iOS와 Firebase Console 대시보드를 공유하므로
 * Android에서 다른 이름을 쓰면 플랫폼 비교 분석이 깨진다.
 *
 * 추가/변경 시 반드시 iOS 측을 먼저 수정한 뒤 이쪽도 동기화할 것 (@.claude/rules/ios-parity.md).
 */
sealed class AnalyticsEvent(val name: String, val parameters: Map<String, Any>? = null) {

    // MARK: - 앱 라이프사이클
    data object 앱시작 : AnalyticsEvent("앱시작")
    data object 앱백그라운드 : AnalyticsEvent("앱백그라운드")
    data object 앱포그라운드 : AnalyticsEvent("앱포그라운드")

    // MARK: - 탭 이동
    data class 탭선택(val 탭이름: String) : AnalyticsEvent("탭선택", mapOf("탭이름" to 탭이름))

    // MARK: - 데이터 새로고침
    data class 수동새로고침(val 화면: String) : AnalyticsEvent("수동새로고침", mapOf("화면" to 화면))
    data object 자동새로고침 : AnalyticsEvent("자동새로고침")

    // MARK: - 차트 상호작용
    data class 차트기간선택(val 기간: String) : AnalyticsEvent("차트기간선택", mapOf("기간" to 기간))

    // MARK: - 시장 지수
    data class 시장지수조회(val 지수이름: String) : AnalyticsEvent("시장지수조회", mapOf("지수이름" to 지수이름))

    // MARK: - 에러
    data class API에러(val 에러유형: String, val 에러메시지: String) :
        AnalyticsEvent("API에러", mapOf("에러유형" to 에러유형, "에러메시지" to 에러메시지))

    data class 네트워크에러(val 에러메시지: String) :
        AnalyticsEvent("네트워크에러", mapOf("에러메시지" to 에러메시지))

    // MARK: - 광고
    data class 배너광고노출(val 화면: String) : AnalyticsEvent("배너광고노출", mapOf("화면" to 화면))
    data class 배너광고클릭(val 화면: String) : AnalyticsEvent("배너광고클릭", mapOf("화면" to 화면))
    data class 배너광고실패(val 에러메시지: String) :
        AnalyticsEvent("배너광고실패", mapOf("에러메시지" to 에러메시지))

    // MARK: - 설정
    data class 설정변경(val 설정항목: String, val 변경값: String) :
        AnalyticsEvent("설정변경", mapOf("설정항목" to 설정항목, "변경값" to 변경값))

    // MARK: - 위젯/워치 (Android에서는 위젯만 해당)
    data object 위젯업데이트 : AnalyticsEvent("위젯업데이트")

    // MARK: - 사용자 행동
    data class 공포지수조회(val 현재점수: Int, val 등급: String) :
        AnalyticsEvent("공포지수조회", mapOf("현재점수" to 현재점수, "등급" to 등급))

    data object 비교데이터조회 : AnalyticsEvent("비교데이터조회")

    // MARK: - 푸시 알림
    data object 알림설정화면진입 : AnalyticsEvent("알림설정화면진입")
    data class 알림설정변경(val 활성화: Boolean) :
        AnalyticsEvent("알림설정변경", mapOf("활성화" to 활성화))

    data class 알림임계값변경(val 하한값: Int, val 상한값: Int) :
        AnalyticsEvent("알림임계값변경", mapOf("하한값" to 하한값, "상한값" to 상한값))

    data class 푸시알림수신(val 공포지수: Int, val 조건: String) :
        AnalyticsEvent("푸시알림수신", mapOf("공포지수" to 공포지수, "조건" to 조건))

    data class 푸시알림탭(val 공포지수: Int, val 조건: String) :
        AnalyticsEvent("푸시알림탭", mapOf("공포지수" to 공포지수, "조건" to 조건))

    data class 푸시알림후체류시간(val 체류초: Int, val 공포지수: Int) :
        AnalyticsEvent("푸시알림후체류시간", mapOf("체류초" to 체류초, "공포지수" to 공포지수))

    // MARK: - 암호화폐 공포지수
    data class 지수타입전환(
        val 타입: String,
        val 화면: String,
        val 이전타입: String? = null,
    ) : AnalyticsEvent(
        name = "지수타입전환",
        parameters = buildMap {
            put("타입", 타입)
            put("화면", 화면)
            이전타입?.let { put("이전타입", it) }
        },
    )

    data class 암호화폐공포지수조회(val 현재점수: Int, val 등급: String) :
        AnalyticsEvent("암호화폐공포지수조회", mapOf("현재점수" to 현재점수, "등급" to 등급))

    data class 암호화폐차트조회(val 기간: String) :
        AnalyticsEvent("암호화폐차트조회", mapOf("기간" to 기간))

    data class 암호화폐알림설정변경(val 활성화: Boolean) :
        AnalyticsEvent("암호화폐알림설정변경", mapOf("활성화" to 활성화))

    data class 암호화폐알림임계값변경(val 하한값: Int, val 상한값: Int) :
        AnalyticsEvent("암호화폐알림임계값변경", mapOf("하한값" to 하한값, "상한값" to 상한값))

    // MARK: - 투표 (Stuck Counter)
    data class 투표참여(val 선택: String, val 지수타입: String, val 현재점수: Int) :
        AnalyticsEvent("투표참여", mapOf("선택" to 선택, "지수타입" to 지수타입, "현재점수" to 현재점수))

    data class 투표결과조회(val 지수타입: String, val 총투표수: Int) :
        AnalyticsEvent("투표결과조회", mapOf("지수타입" to 지수타입, "총투표수" to 총투표수))

    data class 투표중복시도(val 지수타입: String) :
        AnalyticsEvent("투표중복시도", mapOf("지수타입" to 지수타입))

    data class 투표탭진입(val 지수타입: String) :
        AnalyticsEvent("투표탭진입", mapOf("지수타입" to 지수타입))

    data class 투표세그먼트전환(val 지수타입: String, val 이전타입: String) :
        AnalyticsEvent("투표세그먼트전환", mapOf("지수타입" to 지수타입, "이전타입" to 이전타입))

    data class 투표제출실패(val 지수타입: String, val 에러: String) :
        AnalyticsEvent("투표제출실패", mapOf("지수타입" to 지수타입, "에러" to 에러))

    // MARK: - 공유 카드
    data class 공유버튼탭(val 지수타입: String, val 현재점수: Int) :
        AnalyticsEvent("공유버튼탭", mapOf("지수타입" to 지수타입, "현재점수" to 현재점수))

    data class 공유카드생성(val 지수타입: String, val 현재점수: Int, val 등급: String) :
        AnalyticsEvent("공유카드생성", mapOf("지수타입" to 지수타입, "현재점수" to 현재점수, "등급" to 등급))

    data class 공유완료(val 지수타입: String, val 공유대상: String) :
        AnalyticsEvent("공유완료", mapOf("지수타입" to 지수타입, "공유대상" to 공유대상))

    data class 공유취소(val 지수타입: String) :
        AnalyticsEvent("공유취소", mapOf("지수타입" to 지수타입))

    // MARK: - 인터스티셜 광고 (Android는 현재 미사용)
    data class 인터스티셜광고노출(val 노출횟수: Int) :
        AnalyticsEvent("인터스티셜광고노출", mapOf("노출횟수" to 노출횟수))

    data class 인터스티셜광고닫기(val 시청초: Int) :
        AnalyticsEvent("인터스티셜광고닫기", mapOf("시청초" to 시청초))

    data class 인터스티셜광고실패(val 에러메시지: String) :
        AnalyticsEvent("인터스티셜광고실패", mapOf("에러메시지" to 에러메시지))

    // MARK: - 인사이트 (iOS는 영문 키 사용 — 그대로 따름)
    data class 인사이트카드노출(val 카드타입: String, val 지수타입: String) :
        AnalyticsEvent("insight_card_viewed", mapOf("card_type" to 카드타입, "index_type" to 지수타입))

    data class 인사이트목록진입(val 진입경로: String, val 지수타입: String) :
        AnalyticsEvent("insight_list_opened", mapOf("source" to 진입경로, "index_type" to 지수타입))

    data class 인사이트세그먼트전환(val 이전타입: String, val 변경타입: String) :
        AnalyticsEvent("insight_segment_switched", mapOf("from_type" to 이전타입, "to_type" to 변경타입))

    data class 인사이트상세조회(val 카드타입: String, val 점수: Int) :
        AnalyticsEvent("insight_detail_viewed", mapOf("card_type" to 카드타입, "score" to 점수))

    // MARK: - 온보딩 투어 (iOS는 영문 GA 이름/키 사용 — 그대로 따름, v1.9.3 parity)
    data class 온보딩완료(val 단계: Int) :
        AnalyticsEvent("onboarding_done", mapOf("step" to 단계))

    data class 온보딩건너뛰기(val 단계: Int) :
        AnalyticsEvent("onboarding_skip", mapOf("step" to 단계))

    // MARK: - 인앱결제 (광고 제거 = 프리미엄)
    // v1.9.4 parity: 진입 경로 구분용 `source` ("settings" | "score_explorer" | "notification_history").
    // 기본값 settings 라 기존 호출처 무수정 (iOS AnalyticsEvent 동일).
    data class 광고제거구매시작(val source: String = PremiumPurchaseSource.SETTINGS) :
        AnalyticsEvent("광고제거구매시작", mapOf("source" to source))

    data class 광고제거구매완료(val source: String = PremiumPurchaseSource.SETTINGS) :
        AnalyticsEvent("광고제거구매완료", mapOf("source" to source))

    data class 광고제거구매실패(val 에러메시지: String, val source: String = PremiumPurchaseSource.SETTINGS) :
        AnalyticsEvent("광고제거구매실패", mapOf("에러메시지" to 에러메시지, "source" to source))

    data class 광고제거복원(val 성공여부: Boolean, val source: String = PremiumPurchaseSource.SETTINGS) :
        AnalyticsEvent("광고제거복원", mapOf("성공여부" to 성공여부, "source" to source))

    // MARK: - 프리미엄 (v1.9.4 parity, GA 이름은 iOS 와 동일한 영문 snake_case)
    /** 잠금 row 탭. feature = PremiumFeature.analyticsKey ("score_explorer" | "notification_history_unlimited"). */
    data class 프리미엄잠금탭(val feature: String) :
        AnalyticsEvent("premium_lock_tapped", mapOf("feature" to feature))

    /** 점수 탐색기 슬라이더 드래그 종료 시 1회. period = ReturnHorizon.analyticsKey ("oneMonth"…"oneYear"). */
    data class 점수탐색기조작(val indexType: String, val score: Int, val period: String) :
        AnalyticsEvent("score_explorer_moved", mapOf("index_type" to indexType, "score" to score, "period" to period))

    /** 알림 내역 화면 진입 (보이는 건수). */
    data class 알림내역조회(val 개수: Int) :
        AnalyticsEvent("notification_history_viewed", mapOf("count" to 개수))

    // MARK: - 수익 최적화 이벤트
    data class 화면체류시간(val 화면: String, val 체류초: Int) :
        AnalyticsEvent("화면체류시간", mapOf("화면" to 화면, "체류초" to 체류초))

    data class 차트상호작용(val 타입: String) :
        AnalyticsEvent("차트상호작용", mapOf("타입" to 타입))

    data class 알림설정진입경로(val 경로: String) :
        AnalyticsEvent("알림설정진입경로", mapOf("경로" to 경로))

    data class 세션종료(val 세션길이초: Int) :
        AnalyticsEvent("세션종료", mapOf("세션길이초" to 세션길이초))
}

/**
 * 화면 이름 정의 — iOS [AnalyticsScreen] 매핑.
 */
enum class AnalyticsScreen(val screenName: String, val screenClass: String) {
    홈("홈", "HomeScreen"),
    차트("차트", "ChartScreen"),
    설정("설정", "SettingsScreen"),
    투표("투표", "VoteScreen"),
    알림설정("알림설정", "NotificationSettingsScreen");
}

/** 프리미엄(광고 제거) 구매 진입 경로 — iOS PremiumPurchaseSource 와 동일 문자열. */
object PremiumPurchaseSource {
    const val SETTINGS = "settings"
    const val SCORE_EXPLORER = "score_explorer"
    const val NOTIFICATION_HISTORY = "notification_history"
}
