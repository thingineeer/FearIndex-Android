package th1ngjin.fearindex.domain.usecase

import th1ngjin.fearindex.domain.entity.FearIndex
import th1ngjin.fearindex.domain.entity.FearIndexPeak
import javax.inject.Inject

/**
 * 차트 view 가 의존하는 추상 타입. 구현체([ComputeFearIndexPeaks]) 에 묶이지 않도록
 * View / 테스트에서 인터페이스로만 받는다.
 *
 * iOS `FearIndexPeaksComputing` 와 1:1 대응.
 */
fun interface FearIndexPeaksComputing {
    /** 시간 오름차순 history → (high, low) peak. 빈 배열이면 `null`. */
    operator fun invoke(history: List<FearIndex>): Pair<FearIndexPeak, FearIndexPeak>?
}

/**
 * 공포지수 history 배열로부터 고점/저점 마커 정보를 계산하는 순수 함수 UseCase.
 * - 네트워크/저장소 접근 없음
 * - Period 전환(3M/6M/1Y/5Y 등) 시 UI 레이어에서 필터링된 history 에 바로 적용
 *
 * 정책 (iOS `ComputeFearIndexPeaks.swift` 와 동일):
 * 1. history 는 시간 오름차순(CNN API 응답 순서)으로 가정.
 * 2. 동점(같은 max 또는 같은 min)이 여러 개인 경우 가장 최근 포인트를 채택.
 *    (UX: 사용자는 최신 피크를 기억하기 쉬움)
 * 3. 빈 배열 → null.
 * 4. 단일 포인트 → high == low == 해당 포인트.
 */
class ComputeFearIndexPeaks @Inject constructor() : FearIndexPeaksComputing {

    /**
     * @param history 시간 오름차순 [FearIndex] 배열.
     * @return 빈 배열이면 `null`. 그 외 `(high, low)` 쌍.
     */
    override fun invoke(history: List<FearIndex>): Pair<FearIndexPeak, FearIndexPeak>? {
        if (history.isEmpty()) return null

        var highIndex = 0
        var lowIndex = 0

        // 한 번의 순회로 min/max 인덱스 모두 확정.
        // 동점이면 "최근(뒤)" 을 택하기 위해 `>=` / `<=` 사용.
        for ((idx, item) in history.withIndex()) {
            if (item.score >= history[highIndex].score) {
                highIndex = idx
            }
            if (item.score <= history[lowIndex].score) {
                lowIndex = idx
            }
        }

        val high = FearIndexPeak(
            kind = FearIndexPeak.Kind.HIGH,
            score = history[highIndex].score,
            date = history[highIndex].timestamp,
            index = highIndex,
        )
        val low = FearIndexPeak(
            kind = FearIndexPeak.Kind.LOW,
            score = history[lowIndex].score,
            date = history[lowIndex].timestamp,
            index = lowIndex,
        )
        return high to low
    }
}
