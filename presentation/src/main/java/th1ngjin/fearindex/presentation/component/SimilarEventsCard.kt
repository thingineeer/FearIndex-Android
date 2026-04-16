package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.domain.entity.AggregateStats
import th1ngjin.fearindex.domain.entity.EventMatch
import th1ngjin.fearindex.domain.entity.Similarity
import th1ngjin.fearindex.domain.entity.SimilarEventsResult
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.common.localizedEventTitle

/**
 * "지금과 비슷했던 시기" 인사이트 카드.
 * iOS `SimilarEventsCardView` 1:1 대칭.
 */
@Composable
fun SimilarEventsCard(
    result: SimilarEventsResult,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.similar_events_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.similar_events_score_label, result.currentScore),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Matches or empty
        if (result.matches.isEmpty()) {
            Text(
                text = stringResource(R.string.similar_events_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            result.matches.forEachIndexed { index, match ->
                MatchRow(match)
                if (index < result.matches.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 7.dp))
                }
            }
        }

        // Aggregate stats
        result.aggregateStats?.let { stats ->
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            AggregateSection(stats)
        }

        // Disclaimer
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.similar_events_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Composable
private fun MatchRow(match: EventMatch) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = localizedEventTitle(match.titleKey),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${match.score}${stringResource(R.string.similar_events_score_suffix)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SimilarityBadge(match.similarity)
            val oneYear = match.returnAfter.oneYear
            if (oneYear != null) {
                Text(
                    text = stringResource(
                        R.string.similar_events_one_year_return,
                        formatReturn(oneYear),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
            } else if (match.isOngoing) {
                Text(
                    text = stringResource(R.string.similar_events_ongoing),
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
private fun SimilarityBadge(similarity: Similarity) {
    val (label, color) = when (similarity) {
        Similarity.VERY -> stringResource(R.string.similar_events_sim_very) to Color(0xFFE53935)
        Similarity.CLOSE -> stringResource(R.string.similar_events_sim_close) to Color(0xFFFF9800)
        Similarity.MODERATE -> stringResource(R.string.similar_events_sim_moderate) to Color(0xFFFDD835)
        Similarity.FAR -> stringResource(R.string.similar_events_sim_far) to Color.Gray
    }
    Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun AggregateSection(stats: AggregateStats) {
    Text(
        text = stringResource(R.string.similar_events_aggregate_title, stats.score),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(
            R.string.similar_events_aggregate_summary,
            formatReturn(stats.avgReturn.oneYear),
            stats.sampleCount,
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatReturn(value: Double): String = String.format("%+.1f%%", value)
