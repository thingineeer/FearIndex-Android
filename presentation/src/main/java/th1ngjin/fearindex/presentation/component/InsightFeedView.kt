package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.MarketInsight
import th1ngjin.fearindex.presentation.BuildConfig
import th1ngjin.fearindex.presentation.R

/**
 * 인사이트 카드 리스트. HomeScreen 하단에 표시.
 *
 * 6종 카드를 순서대로 나열하고, historicalReturn 다음에 AdBanner 삽입.
 */
@Composable
fun InsightFeedView(
    insights: List<MarketInsight>,
    indexType: FearIndexType,
    onInsightClick: (MarketInsight) -> Unit,
    onCardViewed: (MarketInsight) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (insights.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // 섹션 헤더
        val headerText = when (indexType) {
            FearIndexType.MARKET -> stringResource(R.string.insight_feed_header_market)
            FearIndexType.KOSPI -> stringResource(R.string.insight_feed_header_market)
            FearIndexType.CRYPTO -> stringResource(R.string.insight_feed_header_crypto)
        }
        Text(
            text = headerText,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        insights.forEachIndexed { index, insight ->
            // Analytics: 카드 노출 이벤트
            LaunchedEffect(insight.id) {
                onCardViewed(insight)
            }

            InsightTeaserCard(
                insight = insight,
                onClick = { onInsightClick(insight) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // historicalReturn 카드 다음에 AdBanner 삽입
            if (insight.type == th1ngjin.fearindex.domain.entity.InsightType.HISTORICAL_RETURN) {
                AdBanner(
                    adUnitId = BuildConfig.ADMOB_BANNER_INSIGHT,
                    screenName = "홈_인사이트피드",
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
