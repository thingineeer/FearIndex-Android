package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.theme.fearScoreColor
import kotlin.math.abs

/**
 * Comparison card matching the iOS FearComparisonCard.
 *
 * Shows 4 columns: 전일, 1주전, 1개월전, 1년전.
 * Each column displays the label, the historical score (colored by fearScoreColor),
 * and a change arrow (up/down/same) relative to the current score.
 */
@Composable
fun ComparisonCard(
    currentScore: Int,
    previousClose: Double?,
    previous1Week: Double?,
    previous1Month: Double?,
    previous1Year: Double?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header
            Text(
                text = stringResource(R.string.comparison_card_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ComparisonColumn(stringResource(R.string.comparison_previous_close), previousClose, currentScore, Modifier.weight(1f))
                ComparisonColumn(stringResource(R.string.comparison_1w_ago), previous1Week, currentScore, Modifier.weight(1f))
                ComparisonColumn(stringResource(R.string.comparison_1m_ago), previous1Month, currentScore, Modifier.weight(1f))
                ComparisonColumn(stringResource(R.string.comparison_1y_ago), previous1Year, currentScore, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ComparisonColumn(
    label: String,
    previousScore: Double?,
    currentScore: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        // Label
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (previousScore != null) {
            val prevInt = previousScore.toInt()

            // Score value colored by fear level
            Text(
                text = "$prevInt",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = fearScoreColor(prevInt),
            )

            // Change arrow
            val diff = currentScore - prevInt
            val (arrow, diffColor) = changeArrowAndColor(diff)
            Text(
                text = "$arrow${abs(diff)}",
                color = diffColor,
                fontSize = 12.sp,
            )
        } else {
            // Placeholder
            Text(
                text = "--",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = " ",
                fontSize = 12.sp,
            )
        }
    }
}

private fun changeArrowAndColor(diff: Int): Pair<String, Color> = when {
    diff > 0 -> "↑" to Color(0xFF4CAF50)  // green
    diff < 0 -> "↓" to Color(0xFFE53935)  // red
    else -> "" to Color.Gray
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun ComparisonCardPreview() {
    MaterialTheme {
        ComparisonCard(
            currentScore = 46,
            previousClose = 52.0,
            previous1Week = 44.0,
            previous1Month = 38.0,
            previous1Year = 34.0,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ComparisonCardNoYearPreview() {
    MaterialTheme {
        ComparisonCard(
            currentScore = 73,
            previousClose = 68.0,
            previous1Week = 55.0,
            previous1Month = 42.0,
            previous1Year = null,
            modifier = Modifier.padding(16.dp),
        )
    }
}
