package th1ngjin.fearindex.qa

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.EntryPointAccessors
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import th1ngjin.fearindex.MainActivity
import th1ngjin.fearindex.core.purchases.DebugPremiumOverride
import th1ngjin.fearindex.core.purchases.DebugPremiumOverrideStore
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.di.AdsEntryPoint

/**
 * v1.9.4 프리미엄 QA (iOS `PremiumQAUITests` 이식, 헤드리스 에뮬레이터 전제).
 *
 * T1 결제 안 함: 점수 탐색기 잠금 + '광고 제거됨' 미표시 + 알림 내역 진입(빈 상태/잠금 row)
 * T2 결제함:     점수 탐색기 해제(슬라이더 실조작으로 점수 변경) + '광고 제거됨' + 알림 내역 잠금 row 없음
 * T3 결제 전환:  DEBUG 결제 테스트 카드 — 구매 안 함 → 잠금 → 구매함 → 즉시 해제 → 구매 안 함 → 재잠금
 *
 * 결제 상태 강제 = debug 소스셋 DebugPremiumOverrideStore(설정 카드와 동일 경로). 광고 로딩은 assert 하지 않는다.
 */
@RunWith(AndroidJUnit4::class)
class PremiumQaTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else GrantPermissionRule.grant()

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var purchaseManager: PurchaseManager
    private lateinit var overrideStore: DebugPremiumOverrideStore
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        purchaseManager = EntryPointAccessors
            .fromApplication(context.applicationContext, AdsEntryPoint::class.java)
            .purchaseManager()
        overrideStore = DebugPremiumOverrideStore(context, purchaseManager)
        // 온보딩 투어가 카드를 가리지 않도록 '이미 봄' 처리 (bugs-fixed 35번)
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("hasSeenOnboardingTourV1", true).apply()
    }

    @After
    fun tearDown() {
        // 다음 실행 오염 방지 — '실제' 복귀
        overrideStore.apply(DebugPremiumOverride.REAL)
        scenario?.close()
    }

    // MARK: - T1 결제 안 함 → 프리미엄 잠김

    @Test
    fun test01_freeUser_premiumFeaturesLocked() {
        launch(DebugPremiumOverride.NOT_PURCHASED)

        openChartExplorer()
        composeRule.onNodeWithTag(SCORE_EXPLORER_CARD).assertIsDisplayed()
        composeRule.waitUntilExists(hasText(str(R.string.score_explorer_lock_title)))
        composeRule.onNodeWithTag(PREMIUM_LOCK_CTA).assertExists()
        assertTrue("무료인데 슬라이더 리셋 버튼 노출", composeRule.nodeCount(SCORE_EXPLORER_RESET) == 0)

        openTab("settings")
        composeRule.waitForIdle()
        assertTrue("무료인데 광고 제거됨 표시", composeRule.textCount(str(R.string.settings_remove_ads_purchased)) == 0)

        openNotificationHistory()
        val empty = composeRule.nodeCount(HISTORY_EMPTY) > 0
        val lock = composeRule.nodeCount(HISTORY_LOCK_ROW) > 0
        assertTrue("무료 유저 알림 내역: 빈 상태도 잠금 row 도 없음", empty || lock)
    }

    // MARK: - T2 결제함 → 프리미엄 해제 + 슬라이더 조작

    @Test
    fun test02_premiumUser_featuresUnlocked_sliderWorks() {
        launch(DebugPremiumOverride.PURCHASED)

        openChartExplorer()
        assertTrue("프리미엄인데 잠금 CTA 노출", composeRule.nodeCount(PREMIUM_LOCK_CTA) == 0)
        composeRule.waitUntilExists(hasTestTag(SCORE_EXPLORER_RESET))
        composeRule.waitUntilExists(hasTestTag(SCORE_EXPLORER_SLIDER))

        val before = composeRule.scoreText()
        composeRule.onNodeWithTag(SCORE_EXPLORER_SLIDER).performScrollTo().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        val after = composeRule.scoreText()
        assertNotEquals("슬라이더 조작 후 값 불변", before, after)
        composeRule.onNodeWithTag(SCORE_EXPLORER_RESET).assertIsEnabled()

        openTab("settings")
        composeRule.waitUntilExists(hasText(str(R.string.settings_remove_ads_purchased)))

        openNotificationHistory()
        assertTrue("프리미엄인데 잠금 row 노출", composeRule.nodeCount(HISTORY_LOCK_ROW) == 0)
    }

    // MARK: - T3 결제 전환 (DEBUG 결제 테스트 카드 = 결제 시나리오)

    @Test
    fun test03_purchaseTransition_unlocksAndRelocks() {
        launch(DebugPremiumOverride.REAL)

        // ① 구매 안 함 → 미구매 상태
        tapDebugSegment("notPurchased")
        composeRule.waitUntilGone(hasText(str(R.string.settings_remove_ads_purchased)))
        openChartExplorer()
        composeRule.waitUntilExists(hasTestTag(PREMIUM_LOCK_CTA))

        // ② 구매함(=결제 완료 시뮬레이션) → 즉시 해제
        tapDebugSegment("purchased")
        composeRule.waitUntilExists(hasText(str(R.string.settings_remove_ads_purchased)))
        openChartExplorer()
        assertTrue("결제 후에도 잠금 CTA 노출", composeRule.nodeCount(PREMIUM_LOCK_CTA) == 0)
        composeRule.waitUntilExists(hasTestTag(SCORE_EXPLORER_RESET))

        // ③ 다시 구매 안 함 → 재잠금 (revoke 반영)
        tapDebugSegment("notPurchased")
        openChartExplorer()
        composeRule.waitUntilExists(hasTestTag(PREMIUM_LOCK_CTA))
    }

    // MARK: - Helpers

    private fun launch(override: DebugPremiumOverride) {
        overrideStore.apply(override)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitUntilExists(hasTestTag("nav-home"), timeoutMillis = 30_000)
    }

    private fun openTab(route: String) {
        composeRule.onNodeWithTag("nav-$route").performClick()
        composeRule.waitForIdle()
    }

    /** 차트 탭 → 데이터 로드 후 점수 탐색기 카드 출현까지 대기(최대 60s) → 카드로 스크롤. */
    private fun openChartExplorer() {
        openTab("chart")
        composeRule.waitUntilExists(hasTestTag(SCORE_EXPLORER_CARD), timeoutMillis = 60_000)
        composeRule.onNodeWithTag(SCORE_EXPLORER_CARD).performScrollTo()
        composeRule.waitForIdle()
    }

    /** 홈 🔔 → 알림 내역 진입 (빈 상태/리스트/잠금 row 중 하나가 뜰 때까지 대기). */
    private fun openNotificationHistory() {
        openTab("home")
        composeRule.waitUntilExists(hasTestTag(HOME_HISTORY_BUTTON), timeoutMillis = 15_000)
        composeRule.onNodeWithTag(HOME_HISTORY_BUTTON).performClick()
        composeRule.waitUntilExists(hasTestTag(HISTORY_VIEW), timeoutMillis = 15_000)
        composeRule.waitUntil(15_000) {
            composeRule.nodeCount(HISTORY_EMPTY) > 0 || composeRule.nodeCount(HISTORY_LOCK_ROW) > 0 ||
                composeRule.nodeCount(HISTORY_LIST) > 0
        }
    }

    private fun tapDebugSegment(storageValue: String) {
        openTab("settings")
        composeRule.waitUntilExists(hasTestTag(DEBUG_IAP_CARD), timeoutMillis = 15_000)
        composeRule.onNodeWithTag(DEBUG_IAP_CARD).performScrollTo()
        composeRule.onNodeWithTag("debug-iap-seg-$storageValue").performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    private fun str(id: Int): String = context.getString(id)

    private fun ComposeTestRule.scoreText(): String {
        val config = onNodeWithTag(SCORE_EXPLORER_SCORE, useUnmergedTree = true).fetchSemanticsNode().config
        val key = androidx.compose.ui.semantics.SemanticsProperties.Text
        return if (config.contains(key)) config[key].joinToString { it.text } else ""
    }

    private fun ComposeTestRule.nodeCount(tag: String): Int =
        onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun ComposeTestRule.textCount(text: String): Int =
        onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun ComposeTestRule.waitUntilExists(
        matcher: androidx.compose.ui.test.SemanticsMatcher,
        timeoutMillis: Long = 10_000,
    ) = waitUntil(timeoutMillis) {
        onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun ComposeTestRule.waitUntilGone(
        matcher: androidx.compose.ui.test.SemanticsMatcher,
        timeoutMillis: Long = 10_000,
    ) = waitUntil(timeoutMillis) {
        onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }

    private companion object {
        const val SCORE_EXPLORER_CARD = "score-explorer-card"
        const val SCORE_EXPLORER_SLIDER = "score-explorer-slider"
        const val SCORE_EXPLORER_RESET = "score-explorer-reset"
        const val SCORE_EXPLORER_SCORE = "score-explorer-score"
        const val PREMIUM_LOCK_CTA = "premium-lock-cta"
        const val HOME_HISTORY_BUTTON = "home-notification-history-button"
        const val HISTORY_VIEW = "notification-history-view"
        const val HISTORY_LIST = "notification-history-list"
        const val HISTORY_EMPTY = "notification-history-empty"
        const val HISTORY_LOCK_ROW = "notification-history-lock-row"
        const val DEBUG_IAP_CARD = "debug-iap-card"
    }
}
