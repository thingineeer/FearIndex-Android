package th1ngjin.fearindex.presentation.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Rect
import th1ngjin.fearindex.presentation.R

/**
 * 온보딩 코치마크 투어 8단계 명세 — iOS `OnboardingTourView.steps` 1:1 미러.
 * 순서·개수·문자열 키 고정. destination 은 투어가 수행할 탭/세그먼트/스크롤 목적지.
 */

/** 단계 진입 시 투어가 이동시킬 목적지. 실제 전환은 NavHost 가 수행. */
enum class OnboardingDestination {
    HOME_MARKET, // 홈 + 세그먼트 market
    HOME_KOSPI, // 홈 + 세그먼트 kospi
    HOME_CRYPTO, // 홈 + 세그먼트 crypto
    HOME_INSIGHT, // 홈 market + 인사이트 카드로 스크롤
    VOTE, // 투표 탭
    SETTINGS, // 설정 탭 (알림 설정 행)
    SETTINGS_WIDGET, // 설정 탭 + 위젯 사용법 행으로 스크롤
}

/** 하이라이트 대상 앵커 ID (iOS anchorID 와 동일). */
object OnboardingAnchor {
    const val GAUGE = 1 // 게이지 (1~3단계 공용)
    const val INSIGHT = 4 // 인사이트 카드
    const val VOTE = 5 // 투표(물렸어요) 카드
    const val NOTIFICATION = 6 // 설정 알림 설정 행
    const val WIDGET = 7 // 설정 위젯 사용법 행
}

/**
 * 투어 한 단계.
 * @param anchorId 하이라이트 대상 앵커 (null = 앵커 없이 중앙 카드)
 * @param showSymbol 8단계처럼 대상이 없을 때 아이콘 표시
 */
data class OnboardingStep(
    val anchorId: Int?,
    val showSymbol: Boolean,
    val destination: OnboardingDestination,
    @StringRes val titleRes: Int,
    @StringRes val detailRes: Int,
    /** true 면 하이라이트 안쪽 탭이 딤을 통과해 실제 UI 에 전달된다 (투표 단계에서 바로 눌러보게). */
    val interactiveAnchor: Boolean = false,
)

object OnboardingSteps {
    val ALL: List<OnboardingStep> = listOf(
        OnboardingStep(OnboardingAnchor.GAUGE, false, OnboardingDestination.HOME_MARKET,
            R.string.onboarding_step1_title, R.string.onboarding_step1_detail),
        OnboardingStep(OnboardingAnchor.GAUGE, false, OnboardingDestination.HOME_KOSPI,
            R.string.onboarding_step2_title, R.string.onboarding_step2_detail),
        OnboardingStep(OnboardingAnchor.GAUGE, false, OnboardingDestination.HOME_CRYPTO,
            R.string.onboarding_step3_title, R.string.onboarding_step3_detail),
        OnboardingStep(OnboardingAnchor.INSIGHT, false, OnboardingDestination.HOME_INSIGHT,
            R.string.onboarding_step4_title, R.string.onboarding_step4_detail),
        OnboardingStep(OnboardingAnchor.VOTE, false, OnboardingDestination.VOTE,
            R.string.onboarding_step5_title, R.string.onboarding_step5_detail,
            interactiveAnchor = true),
        OnboardingStep(OnboardingAnchor.NOTIFICATION, false, OnboardingDestination.SETTINGS,
            R.string.onboarding_step6_title, R.string.onboarding_step6_detail),
        OnboardingStep(OnboardingAnchor.WIDGET, false, OnboardingDestination.SETTINGS_WIDGET,
            R.string.onboarding_step7_title, R.string.onboarding_step7_detail),
        // 8단계: 앵커 없이 중앙 심볼 카드. "시작하기" 순간 오버레이만 걷히면 홈 market 최상단이 그대로 남는다.
        OnboardingStep(null, true, OnboardingDestination.HOME_MARKET,
            R.string.onboarding_step8_title, R.string.onboarding_step8_detail),
    )

    val COUNT: Int get() = ALL.size
}

/** 카드 배치 위치 — 대상 위쪽이면 아래, 아래쪽이면 위, 대상 없으면 중앙. */
enum class OnboardingCardPlacement { TOP, BOTTOM, CENTER }

/** 이 비율 이상을 덮는 대상은 "대형 앵커" — 카드를 하단(탭바 쪽)에 붙인다. */
private const val TALL_ANCHOR_FRACTION = 0.55f

/**
 * 카드 배치 계산: 대상이 화면 높이 대부분을 덮으면(4단계 인사이트 카드 등) 카드 머리글
 * 가림을 피해 하단(탭바 쪽)에 배치. 그 외엔 iOS 규칙 — 대상 중앙이 하단 절반이면 TOP,
 * 상단 절반이면 BOTTOM, 대상 없으면 CENTER.
 */
fun onboardingCardPlacement(anchor: Rect?, screenHeight: Float): OnboardingCardPlacement =
    when {
        anchor == null -> OnboardingCardPlacement.CENTER
        anchor.height > screenHeight * TALL_ANCHOR_FRACTION -> OnboardingCardPlacement.BOTTOM
        anchor.center.y > screenHeight * 0.5f -> OnboardingCardPlacement.TOP
        else -> OnboardingCardPlacement.BOTTOM
    }

/**
 * 카드 상단 y(px) — 하이라이트에 밀착시킨다. 화면 끝에 붙여두면 대상과 설명이 멀어지고,
 * 짧은 화면에서는 카드가 올가미를 덮는다.
 *
 * @param gapPx 하이라이트와 카드 사이 간격
 * @param minTopPx 카드가 올라갈 수 있는 최상단 (상태바 아래)
 * @param maxBottomPx 카드가 내려갈 수 있는 최하단 (탭바 위)
 */
fun onboardingCardTopPx(
    placement: OnboardingCardPlacement,
    anchor: Rect?,
    cardHeightPx: Float,
    screenHeightPx: Float,
    gapPx: Float,
    minTopPx: Float,
    maxBottomPx: Float,
): Float {
    val lowestTop = maxBottomPx - cardHeightPx
    val desired = when {
        anchor == null || placement == OnboardingCardPlacement.CENTER ->
            (screenHeightPx - cardHeightPx) / 2f
        placement == OnboardingCardPlacement.BOTTOM -> anchor.bottom + gapPx
        else -> anchor.top - gapPx - cardHeightPx
    }
    // 카드가 가용 높이보다 크면 상단 한계에 고정한다.
    return if (lowestTop <= minTopPx) minTopPx else desired.coerceIn(minTopPx, lowestTop)
}
