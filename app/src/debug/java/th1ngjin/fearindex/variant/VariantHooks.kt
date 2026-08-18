package th1ngjin.fearindex.variant

import android.content.Context
import androidx.compose.runtime.Composable
import th1ngjin.fearindex.core.purchases.DebugPremiumOverrideStore
import th1ngjin.fearindex.core.purchases.PurchaseManager
import th1ngjin.fearindex.debug.DebugPurchaseTestCard

/**
 * 빌드 변형별 훅 — debug: 결제 테스트 오버라이드(재시작 유지) 적용 + 설정 화면 "DEBUG: 결제 테스트" 카드 주입.
 * release 소스셋의 동명 object 는 전부 no-op.
 */
object VariantHooks {
    @Volatile
    private var overrideStore: DebugPremiumOverrideStore? = null

    /** 앱 시작 시 저장된 개발자 오버라이드를 다시 적용한다 (iOS `applyDebugPremiumOverrideIfNeeded`). */
    fun onApplicationCreate(context: Context, purchaseManager: PurchaseManager) {
        val store = DebugPremiumOverrideStore(context.applicationContext, purchaseManager)
        overrideStore = store
        store.applyPersisted()
    }

    fun settingsDebugSection(purchaseManager: PurchaseManager): (@Composable () -> Unit)? {
        val store = overrideStore ?: return null
        return { DebugPurchaseTestCard(store = store, purchaseManager = purchaseManager) }
    }
}
