package th1ngjin.fearindex.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import th1ngjin.fearindex.core.util.indexTypeLabel
import th1ngjin.fearindex.domain.entity.FearIndexType
import th1ngjin.fearindex.domain.entity.InsightType
import th1ngjin.fearindex.domain.entity.MarketInsight
import th1ngjin.fearindex.domain.entity.VelocityTrend
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.common.GreedFrame

@Composable
fun insightTitle(insight: MarketInsight): String {
    val basis = indexTypeLabel(insight.indexType)
    return when (insight.type) {
        InsightType.BUY_SIGNAL -> if (insight.score <= 25) {
            stringResource(R.string.insight_buy_signal_title)
        } else {
            stringResource(R.string.insight_overheat_title)
        }
        InsightType.HISTORICAL_RETURN -> stringResource(
            if (GreedFrame.isGreed(insight.score)) R.string.insight_historical_return_title_greed
            else R.string.insight_historical_return_title,
        )
        InsightType.RETURN_CHART -> when (insight.indexType) {
            FearIndexType.MARKET -> stringResource(R.string.insight_return_chart_title_market)
            FearIndexType.KOSPI -> stringResource(R.string.insight_return_chart_title_kospi)
            FearIndexType.CRYPTO -> stringResource(R.string.insight_return_chart_title_crypto)
        }
        InsightType.DRAWDOWN_TOLERANCE -> stringResource(R.string.insight_drawdown_title, basis)
        InsightType.NUDGE -> stringResource(nudgeTitleRes(insight.score))
        InsightType.FEAR_VELOCITY -> stringResource(R.string.insight_velocity_title)
    }
}

@Composable
fun insightSummary(insight: MarketInsight): String {
    val basis = indexTypeLabel(insight.indexType)
    return when (insight.type) {
        InsightType.BUY_SIGNAL -> if (insight.score <= 25) {
            stringResource(R.string.insight_buy_signal_summary, basis)
        } else {
            stringResource(R.string.insight_overheat_summary)
        }
        InsightType.HISTORICAL_RETURN -> {
            val matched = insight.historicalEvents.size
            if (matched > 0) {
                stringResource(
                    R.string.insight_historical_return_summary_matched,
                    basis,
                    insight.score,
                    matched,
                )
            } else {
                stringResource(
                    R.string.insight_historical_return_summary_empty,
                    basis,
                    insight.score,
                )
            }
        }
        InsightType.RETURN_CHART -> stringResource(R.string.insight_return_chart_summary)
        InsightType.DRAWDOWN_TOLERANCE -> stringResource(drawdownSummaryRes(insight.score))
        InsightType.NUDGE -> stringResource(nudgeBodyRes(insight.score), insight.score)
        InsightType.FEAR_VELOCITY -> {
            val trendRes = velocityTrendRes(insight.velocity?.trend)
            val base = stringResource(trendRes)
            if (insight.velocity?.isInflectionPoint == true) {
                base + stringResource(R.string.insight_velocity_inflection_suffix)
            } else {
                base
            }
        }
    }
}

private fun nudgeTitleRes(score: Int): Int = when {
    score <= 24 -> R.string.nudge_title_extreme_fear
    score <= 44 -> R.string.nudge_title_extreme_fear
    score <= 54 -> R.string.nudge_title_stable
    score <= 74 -> R.string.nudge_title_extreme_greed
    else -> R.string.nudge_title_extreme_greed
}

private fun nudgeBodyRes(score: Int): Int = when {
    score <= 24 -> R.string.nudge_body_extreme_fear
    score <= 44 -> R.string.nudge_body_extreme_fear
    score <= 54 -> R.string.nudge_body_stable
    score <= 74 -> R.string.nudge_body_extreme_greed
    else -> R.string.nudge_body_extreme_greed
}

private fun drawdownSummaryRes(score: Int): Int = when {
    score <= 24 -> R.string.insight_drawdown_summary_extreme_fear
    score <= 44 -> R.string.insight_drawdown_summary_fear
    score <= 54 -> R.string.insight_drawdown_summary_neutral
    score <= 74 -> R.string.insight_drawdown_summary_greed
    else -> R.string.insight_drawdown_summary_extreme_greed
}

private fun velocityTrendRes(trend: VelocityTrend?): Int = when (trend) {
    VelocityTrend.CRASH_ACCELERATING -> R.string.trend_crash_accelerating
    VelocityTrend.CRASH_DECELERATING -> R.string.trend_crash_decelerating
    VelocityTrend.STABLE -> R.string.trend_stable
    VelocityTrend.RALLY_ACCELERATING -> R.string.trend_rally_accelerating
    VelocityTrend.RALLY_DECELERATING -> R.string.trend_rally_decelerating
    null -> R.string.trend_stable
}
