package th1ngjin.fearindex.presentation.feature.onboarding

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.presentation.R

/**
 * 온보딩 코치마크 오버레이 — iOS `OnboardingTourView` 미러.
 * 전체 딤(45%) + 대상 라운드 컷아웃 + 마칭앤츠 링 + 단계 카드.
 * 대상 앵커가 아직 없으면 카드만 중앙에 먼저 표시하고, 앵커가 도착하면 컷아웃/링이 붙는다.
 */
@Composable
fun OnboardingTourOverlay(
    stepNumber: Int, // 1-based
    totalSteps: Int,
    step: OnboardingStep,
    anchor: Rect?, // window 좌표. null = 앵커 없음/미도착
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val reduceMotion = rememberReduceMotion()
    var screenHeightPx by remember { mutableStateOf(0f) }

    val insetPx = with(density) { 6.dp.toPx() }
    val cornerPx = with(density) { 14.dp.toPx() }
    val cutout = anchor?.let {
        Rect(it.left - insetPx, it.top - insetPx, it.right + insetPx, it.bottom + insetPx)
    }
    val placement = onboardingCardPlacement(anchor, screenHeightPx)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { screenHeightPx = it.height.toFloat() },
    ) {
        // 1. 딤 + 컷아웃 (뒤 UI 오조작 방지 — 배경 탭 흡수)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { /* 흡수 */ } },
        ) {
            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                cutout?.let {
                    addRoundRect(RoundRect(it, CornerRadius(cornerPx, cornerPx)))
                }
                fillType = PathFillType.EvenOdd
            }
            drawPath(path, Color.Black.copy(alpha = 0.45f))
        }

        // 2. 마칭앤츠 링
        cutout?.let { MarchingAntsRing(rect = it, cornerPx = cornerPx, color = accent, reduceMotion = reduceMotion) }

        // 3. 단계 카드
        Column(modifier = Modifier.fillMaxSize()) {
            when (placement) {
                OnboardingCardPlacement.TOP -> {
                    StepCard(stepNumber, totalSteps, step, accent, onAdvance, onSkip, Modifier.padding(top = 72.dp))
                    Spacer(Modifier.weight(1f))
                }
                OnboardingCardPlacement.BOTTOM -> {
                    Spacer(Modifier.weight(1f))
                    StepCard(stepNumber, totalSteps, step, accent, onAdvance, onSkip, Modifier.padding(bottom = 108.dp))
                }
                OnboardingCardPlacement.CENTER -> {
                    Spacer(Modifier.weight(1f))
                    StepCard(stepNumber, totalSteps, step, accent, onAdvance, onSkip)
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: Int,
    totalSteps: Int,
    step: OnboardingStep,
    accent: Color,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLast = stepNumber == totalSteps
    Surface(
        modifier = modifier.padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$stepNumber/$totalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accent)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (step.showSymbol) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 6.dp)
                        .size(44.dp),
                )
            }
            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(step.detailRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onAdvance,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
            ) {
                Text(
                    text = stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** 하이라이트 올가미 — 점선 링을 dashPhase 애니메이션으로 무한 회전 (이음새 없음). */
@Composable
private fun MarchingAntsRing(rect: Rect, cornerPx: Float, color: Color, reduceMotion: Boolean) {
    val density = LocalDensity.current
    val dashOn = with(density) { 7.dp.toPx() }
    val dashOff = with(density) { 6.dp.toPx() }
    val strokePx = with(density) { 2.5.dp.toPx() }
    val cycle = dashOn + dashOff // 13dp — 한 주기만큼 밀면 이음새 없는 루프

    val phase = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "ants")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = -cycle,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "phase",
        )
        p
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 은은한 글로우 (블러 대신 넓은 저투명 스트로크로 근사)
        drawRoundRect(
            color = color.copy(alpha = 0.35f),
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = strokePx * 2f),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(
                width = strokePx,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff), phase),
            ),
        )
    }
}

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
