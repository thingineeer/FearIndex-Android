package com.thingineer.fearindex.presentation.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thingineer.fearindex.presentation.R
import com.thingineer.fearindex.presentation.theme.ExtremeFear
import com.thingineer.fearindex.presentation.theme.ExtremeGreed
import com.thingineer.fearindex.presentation.theme.Fear
import com.thingineer.fearindex.presentation.theme.Greed
import com.thingineer.fearindex.presentation.theme.Neutral
import com.thingineer.fearindex.presentation.theme.fearScoreColor

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 공포지수 게이지 (스켈레톤)
        FearGauge(score = 41)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun FearGauge(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val gaugeColors = listOf(ExtremeFear, Fear, Neutral, Greed, ExtremeGreed)
    val sweepAngles = listOf(48f, 40f, 22f, 40f, 30f)
    val scoreColor = fearScoreColor(score)
    val needleAngle = 180f + (score / 100f) * 180f

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 24.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            var startAngle = 180f
            gaugeColors.forEachIndexed { i, color ->
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngles[i],
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                startAngle += sweepAngles[i]
            }

            // Needle
            val cx = size.width / 2
            val cy = size.height / 2
            val needleLength = size.width / 2 - strokeWidth - 16.dp.toPx()
            val radians = Math.toRadians(needleAngle.toDouble())
            val endX = cx + needleLength * Math.cos(radians).toFloat()
            val endY = cy + needleLength * Math.sin(radians).toFloat()

            drawCircle(color = Color.Black, radius = 8.dp.toPx(), center = Offset(cx, cy))
            drawLine(
                color = Color.Black,
                start = Offset(cx, cy),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 60.dp),
        ) {
            Text(
                text = "$score",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = fearRatingLabel(score),
                color = scoreColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun fearRatingLabel(score: Int): String = when {
    score <= 24 -> "Extreme Fear"
    score <= 44 -> "Fear"
    score <= 55 -> "Neutral"
    score <= 75 -> "Greed"
    else -> "Extreme Greed"
}
