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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.domain.entity.VoteChoice
import th1ngjin.fearindex.domain.entity.VoteResult

// =============================================================================
// Colors — iOS와 동일
// =============================================================================

private val BuyGreen = Color(0xFF4CAF50)
private val HoldGray = Color(0xFF9E9E9E)
private val SellRed = Color(0xFFE53935)

/**
 * Buy/Hold/Sell 투표 카드.
 *
 * - 투표 전: 3개 버튼 가로 배치
 * - 투표 후: 3색 막대 그래프 + 내 투표 하이라이트
 * - 로딩 중: CircularProgressIndicator
 */
@Composable
fun VoteCardView(
    voteResult: VoteResult,
    isSubmitting: Boolean,
    onVote: (VoteChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasVoted = voteResult.myVote != null

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
            // Header
            VoteHeader()

            if (isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                    )
                }
            } else if (hasVoted) {
                VoteResultBars(voteResult = voteResult)
                VoteOnceNotice()
            } else {
                VoteButtons(onVote = onVote)
            }
        }
    }
}

// MARK: - Header

@Composable
private fun VoteHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "\uD83D\uDDF3\uFE0F",
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "\uC624\uB298\uC758 \uD22C\uD45C",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// MARK: - Vote Buttons (투표 전)

@Composable
private fun VoteButtons(onVote: (VoteChoice) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VoteButton(
            label = "Buy",
            emoji = "\uD83D\uDCC8",
            color = BuyGreen,
            onClick = { onVote(VoteChoice.BUY) },
            modifier = Modifier.weight(1f),
        )
        VoteButton(
            label = "Hold",
            emoji = "\u270B",
            color = HoldGray,
            onClick = { onVote(VoteChoice.HOLD) },
            modifier = Modifier.weight(1f),
        )
        VoteButton(
            label = "Sell",
            emoji = "\uD83D\uDCC9",
            color = SellRed,
            onClick = { onVote(VoteChoice.SELL) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun VoteButton(
    label: String,
    emoji: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$label \uD22C\uD45C"
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}

// MARK: - Vote Result Bars (투표 후)

@Composable
private fun VoteResultBars(voteResult: VoteResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VoteResultBar(
            label = "Buy",
            percentage = voteResult.buyPercentage,
            count = voteResult.buyCount,
            color = BuyGreen,
            isMyVote = voteResult.myVote == VoteChoice.BUY,
        )
        VoteResultBar(
            label = "Hold",
            percentage = voteResult.holdPercentage,
            count = voteResult.holdCount,
            color = HoldGray,
            isMyVote = voteResult.myVote == VoteChoice.HOLD,
        )
        VoteResultBar(
            label = "Sell",
            percentage = voteResult.sellPercentage,
            count = voteResult.sellCount,
            color = SellRed,
            isMyVote = voteResult.myVote == VoteChoice.SELL,
        )

        // 총 투표 수
        Text(
            text = "\uCD1D ${voteResult.totalCount}\uBA85 \uCC38\uC5EC",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun VoteResultBar(
    label: String,
    percentage: Double,
    count: Int,
    color: Color,
    isMyVote: Boolean,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = (percentage / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "voteFraction_$label",
    )

    val barColor by animateColorAsState(
        targetValue = if (isMyVote) color else color.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 300),
        label = "voteBarColor_$label",
    )

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isMyVote) FontWeight.Bold else FontWeight.Medium,
                    color = if (isMyVote) color else MaterialTheme.colorScheme.onSurface,
                )
                if (isMyVote) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "MY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
            Text(
                text = "${String.format("%.1f", percentage)}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isMyVote) FontWeight.Bold else FontWeight.Normal,
                color = if (isMyVote) color else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedFraction.coerceAtLeast(0.01f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor),
            )
        }
    }
}

// MARK: - Notice

@Composable
private fun VoteOnceNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF2A2A2A).copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "\u2705 \uC77C\uC77C \uD22C\uD45C\uB294 1\uD68C\uB9CC \uAC00\uB2A5\uD569\uB2C8\uB2E4",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
