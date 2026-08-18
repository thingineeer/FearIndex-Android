package th1ngjin.fearindex.core.purchases

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 개발자 모드 결제 상태 시뮬레이션 값 (iOS `DebugPremiumOverride` parity).
 *
 * **debug 소스셋 전용** — release 빌드에는 이 타입 자체가 존재하지 않는다 (`core/src/debug`).
 * 설정 탭 "DEBUG: 결제 테스트" 카드가 [DebugPremiumOverrideStore] 로 영속·적용한다.
 */
enum class DebugPremiumOverride(val storageValue: String, val forcedAdFree: Boolean?) {
    /** 오버라이드 없음 — 실제 Play Billing entitlement 평가 */
    REAL("real", null),
    /** 구매한 유저로 강제 (isAdFree = true) */
    PURCHASED("purchased", true),
    /** 구매 안 한 유저로 강제 (isAdFree = false) */
    NOT_PURCHASED("notPurchased", false);

    companion object {
        fun fromStorage(value: String?): DebugPremiumOverride =
            entries.firstOrNull { it.storageValue == value } ?: REAL
    }
}

/**
 * [DebugPremiumOverride] 영속(SharedPreferences) + [PurchaseManager.setEntitlementOverride] 적용.
 * 앱 시작 시 저장값을 다시 적용해 재시작 후에도 유지된다 (iOS UserDefaults `iap.debug.premiumOverride` 대응).
 */
class DebugPremiumOverrideStore(
    private val prefs: SharedPreferences,
    private val purchaseManager: PurchaseManager,
) {
    constructor(context: Context, purchaseManager: PurchaseManager) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        purchaseManager,
    )

    private val _current = MutableStateFlow(load())

    /** 현재 저장된 오버라이드 (설정 카드 세그먼트 바인딩용). */
    val current: StateFlow<DebugPremiumOverride> = _current.asStateFlow()

    /** 마지막으로 적용된 소스 표시용 ("저장값" / "없음"). */
    val appliedSource: String
        get() = if (_current.value == DebugPremiumOverride.REAL) "없음" else "저장값"

    fun load(): DebugPremiumOverride =
        DebugPremiumOverride.fromStorage(prefs.getString(KEY_OVERRIDE, null))

    /** 저장 + 즉시 적용 (REAL 은 강제 해제 → 실제 상태 복귀). */
    fun apply(override: DebugPremiumOverride) {
        prefs.edit().putString(KEY_OVERRIDE, override.storageValue).apply()
        _current.value = override
        purchaseManager.setEntitlementOverride(override.forcedAdFree, "debug override(${override.storageValue})")
    }

    /** 앱 시작 시 저장값 재적용 (REAL 이면 no-op). */
    fun applyPersisted() {
        val saved = load()
        if (saved == DebugPremiumOverride.REAL) return
        purchaseManager.setEntitlementOverride(saved.forcedAdFree, "debug override(persisted ${saved.storageValue})")
    }

    companion object {
        const val PREFS_NAME = "iap_debug_prefs"
        const val KEY_OVERRIDE = "iap.debug.premiumOverride"
    }
}
