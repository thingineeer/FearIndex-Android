package th1ngjin.fearindex.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.presentation.R

/**
 * 공포탐욕 점수(0~100)를 해당 언어의 등급 문자열로 변환.
 * iOS의 rating.extremeFear 등 5단계 구분과 동일 기준(24/44/55/75).
 *
 * @return 현재 locale에 맞는 번역된 문자열 (예: ko → "공포", en → "Fear")
 */
@Composable
fun ratingLabel(score: Int): String = stringResource(
    id = when {
        score <= 24 -> R.string.rating_extreme_fear
        score <= 44 -> R.string.rating_fear
        score <= 55 -> R.string.rating_neutral
        score <= 75 -> R.string.rating_greed
        else -> R.string.rating_extreme_greed
    },
)

/**
 * 엔티티의 등급(원점수 기준 [FearIndex.Rating.from])을 그대로 문자열로 변환.
 * 반올림 점수로 재판정하면 경계(예: 55.4 → 표시 55·등급 탐욕)에서 어긋나므로,
 * 현재 지수의 등급 표시는 반드시 이 오버로드를 쓴다 (2026-08-27 사용자 결정: 원점수 기준 통일).
 */
@Composable
fun ratingLabel(rating: FearIndex.Rating): String = stringResource(
    id = when (rating) {
        FearIndex.Rating.EXTREME_FEAR -> R.string.rating_extreme_fear
        FearIndex.Rating.FEAR -> R.string.rating_fear
        FearIndex.Rating.NEUTRAL -> R.string.rating_neutral
        FearIndex.Rating.GREED -> R.string.rating_greed
        FearIndex.Rating.EXTREME_GREED -> R.string.rating_extreme_greed
    },
)
