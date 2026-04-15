package th1ngjin.fearindex.presentation.component

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Shimmer Brush

/**
 * LinearGradient shimmer brush (iOS와 동일한 좌->우 1.5초 주기 반복).
 *
 * - Light: base `surfaceVariant` + highlight `surface`
 * - Dark: base `surfaceVariant` + highlight `surface`
 * - `ANIMATOR_DURATION_SCALE == 0` (접근성 모션 감소) 시 정적 회색만 표시
 */
@Composable
fun shimmerBrush(showShimmer: Boolean = true): Brush {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface

    // Android 접근성: 애니메이터 비활성화 시 정적 색상
    val context = LocalContext.current
    val isMotionReduced = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }

    if (!showShimmer || isMotionReduced) {
        return Brush.linearGradient(
            colors = listOf(baseColor, baseColor),
            start = Offset.Zero,
            end = Offset.Zero,
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f),
    )
}

// MARK: - Skeleton Box

/**
 * shimmer가 적용된 RoundedCornerShape(12.dp) Box.
 */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(shimmerBrush()),
    )
}

// MARK: - Skeleton Circle

/**
 * shimmer가 적용된 원형 Box.
 */
@Composable
fun SkeletonCircle(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(shimmerBrush()),
    )
}
