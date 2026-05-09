package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.domain.entity.StuckCounterResult
import th1ngjin.fearindex.domain.entity.StuckStatus as DomainStuckStatus
import th1ngjin.fearindex.presentation.R

private const val DISTRIBUTION_THRESHOLD = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StuckDetailSheet(
    result: StuckCounterResult,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.stuck_detail_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            ParticipantsHeader(totalResponded = result.totalResponded)
            if (result.totalResponded >= DISTRIBUTION_THRESHOLD) {
                DistributionSection(result = result)
            } else {
                LowSamplePlaceholder()
            }
            if (result.myStatus != DomainStuckStatus.NONE) {
                MyStatusSection(myStatus = result.myStatus)
            }
            NotReturnGuide()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ParticipantsHeader(totalResponded: Int) {
    Text(
        text = stringResource(R.string.stuck_detail_participants, totalResponded),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun DistributionSection(result: StuckCounterResult) {
    val rounded = roundPercentages(result.stuckPercentage, result.safePercentage)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DetailBar(
                label = stringResource(
                    R.string.stuck_detail_stuck,
                    result.stuckCount,
                    rounded[0],
                ),
                percentage = result.stuckPercentage,
                color = StuckRedColor,
                isSelected = result.myStatus == DomainStuckStatus.STUCK,
            )
            DetailBar(
                label = stringResource(
                    R.string.stuck_detail_safe,
                    result.safeCount,
                    rounded[1],
                ),
                percentage = result.safePercentage,
                color = StuckGreenColor,
                isSelected = result.myStatus == DomainStuckStatus.SAFE,
            )
        }
    }
}

@Composable
private fun DetailBar(
    label: String,
    percentage: Double,
    color: Color,
    isSelected: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
        )
        val fraction = (percentage / 100.0).coerceAtLeast(0.02).toFloat()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = fraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = if (isSelected) 0.85f else 0.45f)),
            )
        }
    }
}

@Composable
private fun LowSamplePlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.insight_stuck_aggregating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.stuck_detail_low_sample_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MyStatusSection(myStatus: DomainStuckStatus) {
    val (textRes, color) = when (myStatus) {
        DomainStuckStatus.STUCK -> R.string.stuck_detail_my_status_stuck to StuckRedColor
        DomainStuckStatus.SAFE -> R.string.stuck_detail_my_status_safe to StuckGreenColor
        DomainStuckStatus.NONE -> return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.stuck_detail_my_status, stringResource(textRes)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun NotReturnGuide() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "ⓘ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.insight_stuck_not_return_guide),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val StuckRedColor = Color(0xFFE53935)
private val StuckGreenColor = Color(0xFF4CAF50)

/** Largest remainder method: 합산 100% 보장 */
private fun roundPercentages(vararg values: Double): IntArray {
    val total = values.sum()
    if (total <= 0.0) return IntArray(values.size)
    val floored = IntArray(values.size) { values[it].toInt() }
    val remainders = values.mapIndexed { index, v -> index to (v - v.toInt()) }
        .sortedByDescending { it.second }
    val diff = 100 - floored.sum()
    for (i in 0 until diff.coerceIn(0, remainders.size)) {
        floored[remainders[i].first] += 1
    }
    return floored
}
