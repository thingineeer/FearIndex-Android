package th1ngjin.fearindex.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
