package th1ngjin.fearindex.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.presentation.R

/**
 * 프리미엄 잠금 기능 공용 row (iOS `PremiumLockRow` parity, v1.9.4).
 *
 * 자물쇠 아이콘 + 제목/본문 + 1차 CTA "프리미엄으로 잠금 해제 · {가격}" + 2차 "구매 복원".
 * - [showsPurchase] false 면 복원 버튼만 남긴다.
 * - [isBusy] 면 CTA 를 스피너로 바꾸고 두 버튼 모두 비활성.
 * 구매/복원 실행·결과 다이얼로그는 호출자(ViewModel)가 담당한다.
 */
@Composable
fun PremiumLockRow(
    title: String,
    body: String,
    priceText: String?,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false,
    showsPurchase: Boolean = true,
    onPurchase: () -> Unit = {},
    onRestore: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth().testTag(PREMIUM_LOCK_ROW_TAG)) {
        PremiumLockHeader(title = title, body = body)
        Spacer(modifier = Modifier.height(14.dp))
        if (showsPurchase) {
            PremiumUnlockButton(priceText = priceText, isBusy = isBusy, onClick = onPurchase)
        }
        TextButton(onClick = onRestore, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.premium_lock_restore), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PremiumLockHeader(title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PremiumUnlockButton(priceText: String?, isBusy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth().testTag(PREMIUM_LOCK_CTA_TAG),
    ) {
        if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text(text = premiumUnlockTitle(priceText), fontWeight = FontWeight.SemiBold)
        }
    }
}

/** "프리미엄으로 잠금 해제 · ₩7,500" (가격 없으면 CTA 만) — iOS purchaseTitle 동일 규칙. */
@Composable
fun premiumUnlockTitle(priceText: String?): String {
    val cta = stringResource(R.string.premium_lock_cta)
    return if (priceText.isNullOrBlank()) cta else "$cta · $priceText"
}

/** "PREMIUM" 캡슐 배지 (카드 헤더용). */
@Composable
fun PremiumBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.premium_badge),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 저표본 경고 배지 — "표본 부족 — 참고만" (기존 similarEvents 카피 재사용). */
@Composable
fun LowSampleWarningBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(R.string.insight_similar_events_low_sample_warning),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

const val PREMIUM_LOCK_ROW_TAG = "premium-lock-row"
const val PREMIUM_LOCK_CTA_TAG = "premium-lock-cta"
