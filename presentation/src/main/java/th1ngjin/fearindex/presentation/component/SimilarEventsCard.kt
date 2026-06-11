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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.domain.entity.AggregateStats
import th1ngjin.fearindex.domain.entity.EventMatch
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.Similarity
import th1ngjin.fearindex.domain.entity.SimilarEventsResult
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.common.localizedEventTitle

private fun assetLabel(indexType: FearIndexType): String = when (indexType) {
    FearIndexType.MARKET -> "S&P"
    FearIndexType.KOSPI -> "KOSPI"
    FearIndexType.CRYPTO -> "BTC"
}

/**
 * "지금과 비슷했던 시기" 인사이트 카드.
 * iOS `SimilarEventsCardView` v2 대칭 — isPinned 기반 섹션 분리.
 */
@Composable
fun SimilarEventsCard(
    result: SimilarEventsResult,
    indexType: FearIndexType,
    modifier: Modifier = Modifier,
) {
    val pinned = result.matches.filter { it.isPinned }
    val similar = result.matches.filter { !it.isPinned }
    val asset = assetLabel(indexType)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.insight_similar_events_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.insight_similar_events_score_label, result.currentScore),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (result.matches.isEmpty()) {
            Text(
                text = stringResource(R.string.insight_similar_events_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            if (pinned.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.insight_similar_events_pinned_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(12.dp),
                ) {
                    pinned.forEachIndexed { index, match ->
                        PinnedMatchRow(match, asset)
                        if (index < pinned.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (similar.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.insight_similar_events_similar_section_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(12.dp),
                ) {
                    similar.forEachIndexed { index, match ->
                        SimilarMatchRow(match, asset)
                        if (index < similar.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }

        result.aggregateStats?.let { stats ->
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            AggregateSection(stats)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.insight_similar_events_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
        )
        }
    }
}

@Composable
private fun PinnedMatchRow(match: EventMatch, asset: String) {
    Column {
        Text(
            text = "${localizedEventTitle(match.titleKey)} — ${stringResource(R.string.insight_event_score, match.score)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        val oneYear = match.returnAfter.oneYear
        if (oneYear != null) {
            Text(
                text = stringResource(R.string.insight_similar_events_one_year_return, "$asset ${formatReturn(oneYear)}"),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SimilarMatchRow(match: EventMatch, asset: String) {
    Column {
        Text(
            text = "${localizedEventTitle(match.titleKey)} — ${stringResource(R.string.insight_event_score, match.score)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SimilarityBadge(match.similarity)
            val oneYear = match.returnAfter.oneYear
            if (oneYear != null) {
                Text(
                    text = stringResource(R.string.insight_similar_events_one_year_return, "$asset ${formatReturn(oneYear)}"),
                    style = MaterialTheme.typography.labelSmall,
                )
            } else if (match.isOngoing) {
                Text(
                    text = stringResource(R.string.insight_similar_events_ongoing),
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
        Similarity.VERY -> stringResource(R.string.insight_similar_events_sim_very) to Color(0xFFE53935)
        Similarity.CLOSE -> stringResource(R.string.insight_similar_events_sim_close) to Color(0xFFFF9800)
        Similarity.MODERATE -> stringResource(R.string.insight_similar_events_sim_moderate) to Color(0xFFFDD835)
        Similarity.FAR -> stringResource(R.string.insight_similar_events_sim_far) to Color.Gray
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
        text = stringResource(R.string.insight_similar_events_aggregate_title, stats.score),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatColumn(
            label = stringResource(R.string.insight_current_score_avg_return),
            value = formatReturn(stats.avgReturn.oneYear),
            color = if (stats.avgReturn.oneYear >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
        )
        stats.maxDrawdown?.let { drawdown ->
            StatColumn(
                label = stringResource(R.string.insight_current_score_max_drawdown),
                value = formatReturn(drawdown.oneYear),
                color = Color(0xFFE53935),
            )
        }
        stats.bestReturn?.let { best ->
            StatColumn(
                label = stringResource(R.string.insight_current_score_best_return),
                value = formatReturn(best.oneYear),
                color = Color(0xFF4CAF50),
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.insight_current_score_sample_count, stats.sampleCount),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatReturn(value: Double): String = String.format("%+.1f%%", value)
