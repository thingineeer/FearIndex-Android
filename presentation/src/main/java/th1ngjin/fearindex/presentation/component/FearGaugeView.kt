package th1ngjin.fearindex.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.presentation.common.ratingLabel
import th1ngjin.fearindex.presentation.theme.ExtremeFear
import th1ngjin.fearindex.presentation.theme.ExtremeGreed
import th1ngjin.fearindex.presentation.theme.Fear
import th1ngjin.fearindex.presentation.theme.Greed
import th1ngjin.fearindex.presentation.theme.Neutral
import th1ngjin.fearindex.presentation.theme.fearScoreColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Arc segment 정의. 매 프레임 재생성 피하기 위해 파일 레벨 상수.
 */
private data class ArcSegment(val fraction: Float, val color: Color)

private val arcSegments: List<ArcSegment> = listOf(
    ArcSegment(0.25f, ExtremeFear),    // 0~25
    ArcSegment(0.20f, Fear),           // 25~45
    ArcSegment(0.10f, Neutral),        // 45~55
    ArcSegment(0.20f, Greed),          // 55~75
    ArcSegment(0.25f, ExtremeGreed),   // 75~100
)

/**
 * iOS FearGaugeView 1:1 포팅.
 *
 * iOS 기준 수치 (scaleFactor=1.0):
 * - 전체 프레임: 280×240
 * - 호(arc): 260×260, offset y=30, lineWidth=24, radius≈118
 * - 바늘: width=8, height=90, offset y=-15
 * - 센터 원: 14×14, offset y=30
 * - 점수 텍스트: offset y=90
 * - tick: offset y=-105 (반지름 방향), major 12dp / minor 6dp
 * - 회전 범위: -135° ~ +135° (270°)
 */
@Composable
fun FearGaugeView(
    score: Int,
    modifier: Modifier = Modifier,
    // 원점수 기준 등급 — 주어지면 라벨/색을 이 등급으로 표시 (반올림 재판정 금지)
    rating: FearIndex.Rating? = null,
) {
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animatedScore.animateTo(
            targetValue = score.toFloat(),
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f),
        )
    }

    val isDark = isSystemInDarkTheme()
    val needleColor = if (isDark) Color.White else Color.Black
    val scoreColor = rating?.let { fearScoreColor(it) } ?: fearScoreColor(score)
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    // iOS: frame(280, 240). arcCenter=160dp, 바늘 tip 최저=213dp, 텍스트 시작=230dp → 총 300dp
    Box(
        modifier = modifier.size(width = 280.dp, height = 300.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.size(width = 280.dp, height = 260.dp)) {
            // iOS: arc frame 260×260, offset y=30
            // arc 중심 = (280/2, 30 + 260/2) = (140, 160) in dp
            val arcDiameterPx = 260.dp.toPx()
            val arcCenterX = size.width / 2f
            val arcCenterY = 30.dp.toPx() + arcDiameterPx / 2f

            val strokeWidth = 24.dp.toPx()
            // iOS: radius = min(w,h)/2 - 12 = 130 - 12 = 118
            val arcRadius = arcDiameterPx / 2f - 12.dp.toPx()

            val arcTopLeft = Offset(
                arcCenterX - arcRadius,
                arcCenterY - arcRadius,
            )
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

            // --- 5색 arc (iOS startAngle/endAngle을 Compose 좌표계로 변환) ---
            // iOS 좌표: 12시가 -90도, 시계방향
            // Compose 좌표: 3시가 0도, 시계방향
            // iOS arc segments: 0~0.25, 0.25~0.45, 0.45~0.55, 0.55~0.75, 0.75~1.0
            // → 270도에 매핑: sweep = fraction * 270
            // Compose startAngle = 135도 (= 270 - 135, 좌하단)
            val totalSweep = 270f
            var currentAngle = 135f // 좌하단에서 시작
            arcSegments.forEach { seg ->
                val sweep = seg.fraction * totalSweep
                drawArc(
                    color = seg.color,
                    startAngle = currentAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                currentAngle += sweep
            }

            // --- tick marks ---
            // iOS: TickMark offset y=-105 (from center), width=2, height=12/6
            drawTickMarks(
                centerX = arcCenterX,
                centerY = arcCenterY,
                tickRadius = 105.dp.toPx(),
                tickColor = tickColor,
            )

            // --- needle ---
            // iOS: frame(8, 90), offset y=-15, rotationEffect(needleAngle)
            // Android에서 dp↔pt 밀도 차이 보정: 약간 짧게
            val needleAngleDeg = -135f + (animatedScore.value / 100f) * 270f
            drawNeedle(
                centerX = arcCenterX,
                centerY = arcCenterY,
                needleWidth = 8.dp.toPx(),
                needleHeight = 75.dp.toPx(),
                needleOffsetY = 10.dp.toPx(),
                angleDeg = needleAngleDeg,
                color = needleColor,
            )

            // --- center dot ---
            // iOS: Circle 14×14, offset y=30 (= arc center)
            drawCircle(
                color = needleColor,
                radius = 7.dp.toPx(),
                center = Offset(arcCenterX, arcCenterY),
            )
        }

        // --- score + rating 텍스트 ---
        // arcCenter=160dp, 바늘 tip 최저=213dp. 폰트 34sp+등급17sp≈55dp → offset 230dp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 230.dp),
        ) {
            Text(
                text = "$score",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = rating?.let { ratingLabel(it) } ?: ratingLabel(score),
                color = scoreColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tick Marks
// ---------------------------------------------------------------------------

/**
 * iOS TickMark 포팅.
 * - 11개 (index 0~10), 27도 간격
 * - iOS: offset y = -105 (중심에서 바깥 방향), width=2, height=12(major)/6(minor)
 * - major: index % 5 == 0 (0, 5, 10)
 * - 각도: -135 + index * 27
 */
private fun DrawScope.drawTickMarks(
    centerX: Float,
    centerY: Float,
    tickRadius: Float,
    tickColor: Color,
) {
    val tickWidth = 2.dp.toPx()

    for (i in 0..10) {
        val isMajor = i % 5 == 0
        val tickLength = if (isMajor) 12.dp.toPx() else 6.dp.toPx()

        // iOS 각도: -135 + i * 27 (12시=0, 시계방향)
        // Compose 각도: 3시=0, 시계방향
        // iOS -135도 → Compose: -135 + 90 = -45 → 하지만 우리가 쓰는 건
        // 동일한 좌표계 변환: iOS rotationEffect는 시계방향, 12시 기준
        // Compose Canvas: 3시가 0도, 시계방향
        // iOS -135° (12시 기준) = Compose 135° (3시 기준)
        // 변환: compose_angle = ios_angle + 90 + 180 = ios_angle - 90
        // iOS: -135 + i*27 → Compose: -135 + i*27 - 90 = -225 + i*27
        // 그런데 Compose에서 225° = 좌하단 = 올바름

        // 더 단순하게: Compose에서 startAngle=135가 좌하단(score=0)
        // tick i의 Compose 각도 = 135 + i * 27
        val angleDeg = 135f + i * 27f
        val angleRad = Math.toRadians(angleDeg.toDouble())

        // 바깥쪽 끝 (호 바깥)
        val outerX = centerX + (tickRadius + tickLength / 2f) * cos(angleRad).toFloat()
        val outerY = centerY + (tickRadius + tickLength / 2f) * sin(angleRad).toFloat()
        // 안쪽 끝 (호 안쪽)
        val innerX = centerX + (tickRadius - tickLength / 2f) * cos(angleRad).toFloat()
        val innerY = centerY + (tickRadius - tickLength / 2f) * sin(angleRad).toFloat()

        drawLine(
            color = tickColor,
            start = Offset(outerX, outerY),
            end = Offset(innerX, innerY),
            strokeWidth = tickWidth,
            cap = StrokeCap.Round,
        )
    }
}

// ---------------------------------------------------------------------------
// Needle
// ---------------------------------------------------------------------------

/**
 * iOS NeedleShape 포팅.
 *
 * iOS 기준:
 * - arrowWidth = width * 1.2 = 9.6
 * - shaftWidth = width * 0.25 = 2
 * - arrowHeight = height * 0.25 = 22.5
 *
 * 로컬 좌표에서 "위"를 향하는 화살표를 그린 후 angleDeg만큼 회전.
 * angleDeg: -135(score=0) ~ +135(score=100), 0=12시 방향
 */
private fun DrawScope.drawNeedle(
    centerX: Float,
    centerY: Float,
    needleWidth: Float,
    needleHeight: Float,
    needleOffsetY: Float,
    angleDeg: Float,
    color: Color,
) {
    val arrowWidth = needleWidth * 1.2f
    val shaftWidth = needleWidth * 0.25f
    val arrowHeight = needleHeight * 0.25f

    // 바늘은 "위"(12시)를 향하도록 그림.
    // iOS: offset y=-15 → 바늘 하단이 중심에서 15dp 위에 위치
    // → 바늘 끝(tip)은 중심에서 (needleHeight - needleOffsetY) = 75dp 위
    // → 바늘 하단은 중심에서 needleOffsetY = 15dp 아래

    val tipY = centerY - (needleHeight - needleOffsetY)  // 바늘 끝
    val baseY = centerY + needleOffsetY                   // 바늘 하단

    val path = Path().apply {
        // 화살촉 끝 (뾰족한 tip)
        moveTo(centerX, tipY)

        // 왼쪽 화살촉
        lineTo(centerX - arrowWidth / 2f, tipY + arrowHeight)

        // 왼쪽 샤프트
        lineTo(centerX - shaftWidth / 2f, tipY + arrowHeight)
        lineTo(centerX - shaftWidth / 2f, baseY)

        // 오른쪽 샤프트
        lineTo(centerX + shaftWidth / 2f, baseY)
        lineTo(centerX + shaftWidth / 2f, tipY + arrowHeight)

        // 오른쪽 화살촉
        lineTo(centerX + arrowWidth / 2f, tipY + arrowHeight)

        close()
    }

    // angleDeg: iOS 기준 -135~+135, 12시가 0도
    // Compose rotate: 시계방향, 현재 orientation이 이미 12시 → angleDeg 그대로 사용
    rotate(
        degrees = angleDeg,
        pivot = Offset(centerX, centerY),
    ) {
        drawPath(path = path, color = color)
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun FearGaugePreviewFear() {
    FearGaugeView(score = 41)
}

@Preview(showBackground = true)
@Composable
private fun FearGaugePreviewNeutral() {
    FearGaugeView(score = 50)
}

@Preview(showBackground = true)
@Composable
private fun FearGaugePreviewGreed() {
    FearGaugeView(score = 85)
}
