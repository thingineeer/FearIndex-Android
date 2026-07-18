package th1ngjin.fearindex.data.storage

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import th1ngjin.fearindex.domain.service.OnboardingEligibility
import th1ngjin.fearindex.domain.service.OnboardingStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 온보딩 투어 영속 저장소 — iOS `OnboardingEligibility` + `@AppStorage` 미러.
 *
 * 자격 판별 신호는 기존 버전이 모든 유저에게 남긴 [StuckCounterStorage] 의 `deviceId`.
 * [captureEligibilityIfNeeded] 는 그 값을 **loadDeviceId 를 거치지 않고** raw 로 읽어
 * (읽는 순간 생성해버리면 신호가 오염됨) FCM 초기화 전에 자격을 확정한다.
 */
@Singleton
class OnboardingStorage @Inject constructor(
    @ApplicationContext context: Context,
) : OnboardingStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** deviceId 원본 저장소 — raw 조회 전용(loadDeviceId 는 없으면 생성하므로 사용 금지). */
    private val deviceIdPrefs: SharedPreferences =
        context.getSharedPreferences(DEVICE_PREFS_NAME, Context.MODE_PRIVATE)

    override fun captureEligibilityIfNeeded() {
        if (prefs.contains(KEY_ELIGIBLE)) return
        val deviceIdPresent = deviceIdPrefs.getString(KEY_DEVICE_ID, null) != null
        prefs.edit()
            .putBoolean(KEY_ELIGIBLE, OnboardingEligibility.captureValue(deviceIdPresent))
            .apply()
    }

    override fun isTourEligible(): Boolean = prefs.getBoolean(KEY_ELIGIBLE, false)

    override fun hasSeenTour(): Boolean = prefs.getBoolean(KEY_SEEN_TOUR, false)

    override fun markTourSeen() {
        prefs.edit().putBoolean(KEY_SEEN_TOUR, true).apply()
    }

    override fun hasSeenWidgetGuide(): Boolean = prefs.getBoolean(KEY_SEEN_WIDGET_GUIDE, false)

    override fun markWidgetGuideSeen() {
        prefs.edit().putBoolean(KEY_SEEN_WIDGET_GUIDE, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_ELIGIBLE = "onboardingTourEligibleV1"
        private const val KEY_SEEN_TOUR = "hasSeenOnboardingTourV1"
        private const val KEY_SEEN_WIDGET_GUIDE = "hasSeenWidgetGuideV1"

        // deviceId 신호 (StuckCounterStorage 와 동일한 파일/키 — v1.0.0부터 불변)
        private const val DEVICE_PREFS_NAME = "stuck_counter_prefs"
        private const val KEY_DEVICE_ID = "deviceId"
    }
}
