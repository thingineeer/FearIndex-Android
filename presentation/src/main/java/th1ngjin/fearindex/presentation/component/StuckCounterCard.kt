package th1ngjin.fearindex.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.presentation.R

enum class StuckStatus { STUCK, NOT_STUCK, NO_RESPONSE }

@Composable
fun StuckCounterCard(
    stuckPercentage: Float,
    myStatus: StuckStatus,
    onToggle: (StuckStatus) -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
    totalResponded: Int = 0,
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = stuckPercentage,
        animationSpec = tween(durationMillis = 500),
        label = "stuckPercentage",
    )
    val roundedPct = animatedPercentage.toInt()
    val gaugeColor = gaugeColor(roundedPct)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeaderSection(onInfoClick = onInfoClick, totalResponded = totalResponded)
            PercentageSection(
                percentage = roundedPct,
                fillFraction = animatedPercentage / 100f,
                gaugeColor = gaugeColor,
            )
            ToggleButtonsSection(
                myStatus = myStatus,
                onToggle = onToggle,
            )
            // Footer 탭 → 상세 시트 오픈 (i 아이콘과 동일 액션). UX: "자세히 보기" 유도 지점 넓힘.
            FooterGuide(onClick = onInfoClick)
        }
    }
}

// MARK: - Header

@Composable
private fun HeaderSection(onInfoClick: () -> Unit, totalResponded: Int = 0) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "\uD83E\uDE78",
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.stuck_card_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(6.dp))
        val infoDesc = stringResource(R.string.stuck_info_content_description)
        Text(
            text = "\u24D8",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(onClick = onInfoClick)
                .semantics {
                    role = Role.Button
                    contentDescription = infoDesc
                },
        )
        if (totalResponded > 0) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.stuck_total_responded, totalResponded),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// MARK: - Percentage + Progress Bar

@Composable
private fun PercentageSection(
    percentage: Int,
    fillFraction: Float,
    gaugeColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "${percentage}%",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = gaugeColor,
            )
            Text(
                text = stringResource(R.string.stuck_percentage_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = fillFraction.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(gaugeColor),
            )
        }
    }
}

// MARK: - Toggle Buttons

@Composable
private fun ToggleButtonsSection(
    myStatus: StuckStatus,
    onToggle: (StuckStatus) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StuckToggleButton(
            label = stringResource(R.string.stuck_button_im_stuck),
            isActive = myStatus == StuckStatus.STUCK,
            activeColor = StuckRed,
            onClick = {
                val next = if (myStatus == StuckStatus.STUCK) StuckStatus.NO_RESPONSE else StuckStatus.STUCK
                onToggle(next)
            },
            modifier = Modifier.weight(1f),
        )
        StuckToggleButton(
            label = stringResource(R.string.stuck_button_im_safe),
            isActive = myStatus == StuckStatus.NOT_STUCK,
            activeColor = StuckGreen,
            onClick = {
                val next = if (myStatus == StuckStatus.NOT_STUCK) StuckStatus.NO_RESPONSE else StuckStatus.NOT_STUCK
                onToggle(next)
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StuckToggleButton(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.9f) else activeColor.copy(alpha = 0.12f),
        animationSpec = tween(durationMillis = 300),
        label = "toggleBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) Color.White else activeColor,
        animationSpec = tween(durationMillis = 300),
        label = "toggleText",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

// MARK: - Footer Guide

@Composable
private fun FooterGuide(onClick: () -> Unit) {
    val footerDesc = stringResource(R.string.stuck_footer_guide)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(Color(0xFF2A2A2A).copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics {
                role = Role.Button
                contentDescription = footerDesc
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = footerDesc,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = ">",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// MARK: - Colors

private val StuckRed = Color(0xFFE53935)
private val StuckGreen = Color(0xFF4CAF50)

private fun gaugeColor(percentage: Int): Color = when {
    percentage < 25 -> Color(0xFF4CAF50)   // green
    percentage < 50 -> Color(0xFFFFCA28)   // yellow
    percentage < 75 -> Color(0xFFFF9800)   // orange
    else -> Color(0xFFE53935)              // red
}
