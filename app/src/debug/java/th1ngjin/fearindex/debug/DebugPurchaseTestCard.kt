package th1ngjin.fearindex.debug

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import th1ngjin.fearindex.core.purchases.DebugPremiumOverride
import th1ngjin.fearindex.core.purchases.DebugPremiumOverrideStore
import th1ngjin.fearindex.core.purchases.PurchaseManager

/**
 * 설정 하단 "DEBUG: 결제 테스트" 카드 (iOS SettingsView DEBUG 카드 parity, debug 소스셋 전용).
 *
 * - 세그먼트 실제 / 구매함 / 구매 안 함 — 영속(재시작 유지), 전환 즉시 광고·프리미엄 잠금 UI 반영
 * - 상태 줄: isAdFree / 상품 가격 / 오버라이드 소스
 * - "실제 구매 흐름 실행" / "구매 복원" — 실제 흐름 진입 시 오버라이드를 '실제' 로 자동 복귀
 * 카피는 iOS 와 동일하게 개발자용 한글 리터럴 (릴리즈 미포함이라 45 locale 대상 아님).
 */
@Composable
fun DebugPurchaseTestCard(store: DebugPremiumOverrideStore, purchaseManager: PurchaseManager) {
    val override by store.current.collectAsStateWithLifecycle()
    val isAdFree by purchaseManager.isAdFree.collectAsStateWithLifecycle()
    val priceText by purchaseManager.priceText.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth().testTag(DEBUG_IAP_CARD_TAG)) {
        Text(
            text = "DEBUG: 결제 테스트",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OverrideSegments(selected = override, onSelect = store::apply)
                Spacer(modifier = Modifier.height(12.dp))
                StatusLines(isAdFree = isAdFree, priceText = priceText, source = store.appliedSource)
                Spacer(modifier = Modifier.height(12.dp))
                ActionButtons(
                    onPurchase = {
                        store.apply(DebugPremiumOverride.REAL)
                        context.findActivity()?.let { purchaseManager.purchaseRemoveAds(it) }
                    },
                    onRestore = {
                        store.apply(DebugPremiumOverride.REAL)
                        scope.launch { purchaseManager.restorePurchases() }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Release 빌드에는 포함되지 않음 · 광고/프리미엄 잠금 UI 즉시 반영 · 버튼은 '실제' 로 전환 후 Play Billing 흐름 실행",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OverrideSegments(selected: DebugPremiumOverride, onSelect: (DebugPremiumOverride) -> Unit) {
    val options = DebugPremiumOverride.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.testTag(debugSegmentTag(option)),
            ) {
                Text(text = option.debugTitle)
            }
        }
    }
}

@Composable
private fun StatusLines(isAdFree: Boolean, priceText: String?, source: String) {
    Column {
        Text(text = "isAdFree: $isAdFree", style = MaterialTheme.typography.bodySmall)
        Text(text = "상품 가격: ${priceText ?: "상품 없음"}", style = MaterialTheme.typography.bodySmall)
        Text(text = "오버라이드 소스: $source", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ActionButtons(onPurchase: () -> Unit, onRestore: () -> Unit) {
    Row {
        OutlinedButton(onClick = onPurchase, modifier = Modifier.weight(1f)) {
            Text(text = "실제 구매 흐름 실행", fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
            Text(text = "구매 복원", fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 세그먼트 라벨 — iOS `DebugPremiumOverride.debugTitle` 동일 (QA 테스트가 이 문자열/태그로 탭한다). */
val DebugPremiumOverride.debugTitle: String
    get() = when (this) {
        DebugPremiumOverride.REAL -> "실제"
        DebugPremiumOverride.PURCHASED -> "구매함"
        DebugPremiumOverride.NOT_PURCHASED -> "구매 안 함"
    }

fun debugSegmentTag(option: DebugPremiumOverride): String = "debug-iap-seg-${option.storageValue}"

const val DEBUG_IAP_CARD_TAG = "debug-iap-card"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
