package th1ngjin.fearindex.variant

import android.content.Context
import androidx.compose.runtime.Composable
import th1ngjin.fearindex.core.purchases.PurchaseManager

/**
 * 빌드 변형별 훅 — release: 전부 no-op. (debug 소스셋의 동명 object 가 개발자 도구를 주입한다.)
 * release 빌드에는 개발자 결제 테스트 관련 심볼이 존재하지 않는다.
 */
object VariantHooks {
    fun onApplicationCreate(context: Context, purchaseManager: PurchaseManager) = Unit

    /** 설정 화면 하단 debug 섹션 — release 없음. */
    fun settingsDebugSection(purchaseManager: PurchaseManager): (@Composable () -> Unit)? = null
}
